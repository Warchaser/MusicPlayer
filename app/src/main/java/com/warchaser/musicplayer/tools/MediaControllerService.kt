package com.warchaser.musicplayer.tools

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.warchaser.musicplayer.R
import com.warchaser.musicplayer.global.AppData
import com.warchaser.musicplayer.main.OnAirActivity

class MediaControllerService : Service() {

    private var mMediaPlayer : MediaPlayer? = null
    private val mMyBinder : MyBinder = MyBinder()

    /**
     * MediaPlayer是否正在播放音乐
     * 这里增设标志位是因为MediaPlayer.isPlaying()方法在部分魔改ROM下
     * 无法正常返回状态且有时会抛异常
     * */
    private var mIsPlaying : Boolean = false

    private val CHANNEL_ID : String = "com.warchaser.MusicPlayer.notification"

    /**
     * 进度条刷新间隔
     * */
    private val REFRESH_TIME : Int = 1000

    private val MEDIA_SESSION_TAG = "com.warchaser.MusicPlayer.tools.MediaControllerService"

    private val PAUSE_FLAG = 0x11
    private val NEXT_FLAG = 0x12
    private val STOP_FLAG = 0x13

    private val NOTIFICATION_ID = 1001

    private var mCurrentMode: Int = MODE_SEQUENCE //default sequence playing

    private var mPlayBackState: PlaybackStateCompat? = null

    private var mIsPreparing : Boolean = false

    private var mMessageCallback : MessageCallback? = null

    private var mAudioManager: AudioManager? = null

    /**
     * Custom Notification's RemoteViews
     */
    private var mNotificationRemoteView: RemoteViews? = null

    /**
     * Notification
     */
    private var mNotification: Notification? = null

    /**
     * Notification's Manager
     */
    private var mNotificationManager: NotificationManager? = null

    /**
     * IntentReceiver
     * For receiving Notification's clicking actions and AUDIO-BECOMING_NOISY action.
     */
    private var mIntentReceiver: IntentReceiver? = null

    /**
     * MediaSessionCompat
     * For receiving media button actions.
     * Earphones and HeadSets.
     */
    private var mMediaSessionCompat: MediaSessionCompat? = null

    companion object{
        /**
         * 用于Handler
         */
        private const val UPDATE_PROGRESS = 1
        private const val UPDATE_CURRENT_MUSIC = 2
        private const val UPDATE_DURATION = 3
        private const val FOCUS_CHANGE = 4
        //用于Handler

        //用于Handler
        const val ACTION_UPDATE_PROGRESS = "com.warchaser.MusicPlayer.UPDATE_PROGRESS"
        const val ACTION_UPDATE_DURATION = "com.warchaser.MusicPlayer.UPDATE_DURATION"
        const val ACTION_UPDATE_CURRENT_MUSIC = "com.warchaser.MusicPlayer.UPDATE_CURRENT_MUSIC"

        const val PAUSE_OR_PLAY_ACTION = "com.warchaser.MusicPlayer.playOrPause"
        const val NEXT_ACTION = "com.warchaser.MusicPlayer.next"
        const val STOP_ACTION = "com.warchaser.MusicPlayer.close"

        /**
         * 播放模式
         */
        const val MODE_ONE_LOOP = 0
        const val MODE_ALL_LOOP = 1
        const val MODE_RANDOM = 2
        const val MODE_SEQUENCE = 3

        /**
         * MediaSessionCompat Flags
         */
        private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)

        const val SINGLE_CLICK = 1
        const val DOUBLE_CLICK = 2

        private class MessageCallback(mediaControllerService: MediaControllerService) : MessageHandler<MediaControllerService>(mediaControllerService){
            override fun handleMessage(t: MediaControllerService?, message: Message?) {
                super.handleMessage(t, message)

                t?.run {
                    when(message?.what){
                        UPDATE_PROGRESS -> toUpdateProgress()
                        UPDATE_CURRENT_MUSIC -> toUpdateCurrentMusic()
                        UPDATE_DURATION -> toUpdateDuration()
                        FOCUS_CHANGE -> handleAudioFocusChanged(message.arg1)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        MusicList.init(contentResolver)

        mMessageCallback = MessageCallback(this)
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        initMediaPlayer()
        super.onCreate()

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeReceiver()
        initializeMediaSessionCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        if(isMediaPlayerNotNull()){
            mMediaPlayer?.run {
                stop()
                release()
            }
            mMediaPlayer = null
        }

        mMessageCallback?.run {
            destroy()
        }
        mMessageCallback = null

        mAudioManager?.run {
            abandonAudioFocus(mAudioFocusListener)
        }

        mIntentReceiver?.run {
            unregisterReceiver(mIntentReceiver)
        }
        mIntentReceiver = null

        releaseMediaSessionCompat()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMyBinder
    }

    private fun initMediaPlayer(){
        mMediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
                override fun onPrepared(mp: MediaPlayer?) {
                    mIsPreparing = false
                    seekTo(MusicList.getCurrentPosition())
                    start()

                    sendMessage(UPDATE_DURATION, null, -1, -1)
                }
            })

            setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
                override fun onCompletion(mp: MediaPlayer?) {
                    if (this@MediaControllerService.isPlaying()) {
                        val currentPosition = currentPosition

                        if (isEmptyFile(currentPosition)) {
                            seekTo(currentPosition + 1000)
                            start()
                            return
                        }

                        when (mCurrentMode) {
                            MODE_ONE_LOOP -> start()
                            MODE_ALL_LOOP -> play((MusicList.getNextPosition()) % MusicList.size(), MusicList.FIRST_PROGRESS)
                            MODE_RANDOM -> play(getRandomPosition(), MusicList.FIRST_PROGRESS)
                            MODE_SEQUENCE -> {
                                if (MusicList.isLastMusic()) {
                                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS)
                                } else {
                                    next()
                                }
                            }
                        }

                    }
                }
            })

            setOnErrorListener(object : MediaPlayer.OnErrorListener {
                override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
                    NLog.eWithFile("MediaControllerService", "MediaPlayer has met a problem! what: $what extra: $extra")
                    return false
                }
            })
        }

    }

    private fun initializeReceiver(){
        mIntentReceiver = IntentReceiver()

        val intentFilter = IntentFilter().apply {
            addAction(PAUSE_OR_PLAY_ACTION)
            addAction(NEXT_ACTION)
            addAction(STOP_ACTION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }

        registerReceiver(mIntentReceiver, intentFilter)
    }

    private fun initializeMediaSessionCompat(){
        mPlayBackState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()

        mMediaSessionCompat = MediaSessionCompat(this, MEDIA_SESSION_TAG).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            setCallback(mMediaSessionCompatCallback)
            setPlaybackState(mPlayBackState)
            isActive = true
        }
    }

    private fun releaseMediaSessionCompat(){
        mMediaSessionCompat?.run {
            setCallback(null)
            isActive = false
            release()
        }
        mMediaSessionCompat = null
    }

    /**
     * 向MessageHandler发送消息
     * */
    private fun sendMessage(what: Int, `object`: Any?, arg1: Int, arg2: Int){
        mMessageCallback?.sendMessage(what, `object`, arg1, arg2)
    }

    /**
     * 向MessageHandler发送延迟消息
     * */
    private fun sendMessageDelayed(what: Int, delayed: Long){
        mMessageCallback?.run {
            sendEmptyMessageDelayed(what, delayed)
        }
    }

    private fun isMediaPlayerNotNull() : Boolean{
        return mMediaPlayer != null
    }

    private fun toUpdateProgress(){
        mMediaPlayer?.run {
            if(this@MediaControllerService.isPlaying() && CallObserver.instance.isNeedCallObserver()){
                Intent().apply {
                    action = ACTION_UPDATE_PROGRESS
                    putExtra(ACTION_UPDATE_PROGRESS, currentPosition)
                    CallObserver.instance.callObserver(this)

                    //每1秒发送一次广播，进度条每秒更新
                    sendMessageDelayed(UPDATE_PROGRESS, REFRESH_TIME.toLong())
                }
            }
        }
    }

    private fun toUpdateCurrentMusic(){
        Intent().apply {
            action = ACTION_UPDATE_CURRENT_MUSIC
            putExtra(ACTION_UPDATE_CURRENT_MUSIC, MusicList.getCurrentMusicInt())
            startForeground(NOTIFICATION_ID, getNotification())
            CallObserver.instance.callObserver(this)
        }
    }

    private fun getNotification() : Notification?{
        val notificationTitle : String
        val bean : MusicInfo
        mNotificationRemoteView = RemoteViews(this.packageName, R.layout.notification).apply {
            if(!MusicList.isListNotEmpty()){
                notificationTitle = "Mr.Song is not here for now……"
                setImageViewResource(R.id.fileImage, R.mipmap.disc)
            } else {
                bean = MusicList.getCurrentMusic()
                notificationTitle = bean.title!!
                val uriString : String? = bean.uriWithCoverPic
                if(TextUtils.isEmpty(uriString)){
                    setImageViewResource(R.id.fileImage, R.mipmap.disc)
                } else {
                    val bitmap : Bitmap? = CoverLoader.instance.loadThumb(bean.albumId)
                    if(bitmap == null){
                        setImageViewResource(R.id.fileImage, R.mipmap.disc)
                    } else {
                        setImageViewBitmap(R.id.fileImage, bitmap)
                    }
                }
            }

            setTextViewText(R.id.fileName, notificationTitle)

            val pauseIntent = Intent(PAUSE_OR_PLAY_ACTION).apply {
                putExtra("FLAG", PAUSE_FLAG)
            }

            PendingIntent.getBroadcast(this@MediaControllerService, 0, pauseIntent, 0).apply {
                setOnClickPendingIntent(R.id.ivPauseOrPlay, this)
            }

            setImageViewResource(R.id.ivPauseOrPlay, if (isPlaying()) R.mipmap.pausedetail else R.mipmap.run)

            val nextIntent = Intent(NEXT_ACTION).apply {
                putExtra("FLAG", NEXT_FLAG)
            }

            PendingIntent.getBroadcast(this@MediaControllerService, 0, nextIntent, 0).apply {
                setOnClickPendingIntent(R.id.ivNext, this)
            }

            val closeIntent = Intent(STOP_ACTION).apply {
                putExtra("FLAG", STOP_FLAG)
            }

            PendingIntent.getBroadcast(this@MediaControllerService, 0, closeIntent, 0).apply {
                setOnClickPendingIntent(R.id.ivClose, this)
            }

            val activityIntent = Intent(this@MediaControllerService, OnAirActivity::class.java).apply{
                //clear the top of the stack, this flag can forbid the possibility of the two activities
                //existing at the same time
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            PendingIntent.getActivity(this@MediaControllerService, 0, activityIntent, 0).apply {
                setOnClickPendingIntent(R.id.lyRoot, this)
            }

            val builder = NotificationCompat.Builder(this@MediaControllerService, CHANNEL_ID)
                    .setContent(this)
                    .setSmallIcon(R.mipmap.notification)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel(CHANNEL_ID, "MediaControllerService", NotificationManager.IMPORTANCE_LOW).apply {
                    setSound(null, null)
                    mNotificationManager?.createNotificationChannel(this)
                }
            }

            builder.setOnlyAlertOnce(true)
            mNotification = builder.build().apply {
                flags = flags or Notification.FLAG_NO_CLEAR
            }
        }

        return mNotification
    }

    private fun toUpdateDuration(){
        mMediaPlayer?.run {
            Intent().apply {
                action = ACTION_UPDATE_DURATION
                putExtra(ACTION_UPDATE_DURATION, duration)
                CallObserver.instance.callObserver(this)
            }
        }
    }

    private fun isPlaying() : Boolean{
        return mIsPlaying
    }

    private fun isEmptyFile(currentPosition : Int) : Boolean{
        mMediaPlayer?.run {
            return currentPosition < duration
        }

        return false
    }

    private fun setCurrentMusic(){
        sendMessage(UPDATE_CURRENT_MUSIC, null, -1, -1)
    }

    private fun getRandomPosition() : Int{
        return (Math.random() * MusicList.getLastPosition()).toInt()
    }

    private fun play(currentMusic: Int, currentProgress: Int){
        mMediaPlayer?.run {
            val status = mAudioManager?.requestAudioFocus(mAudioFocusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

            if(status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                return
            }

            MusicList.setCurrentPosition(currentProgress)
            MusicList.setCurrentMusic(currentMusic)

            stop()
            reset()

            try {
                if(MusicList.isListNotEmpty()){
                    setDataSource(MusicList.getCurrentMusic().url)
                    prepareAsync()
                    mIsPreparing = true
                    sendMessage(UPDATE_PROGRESS, null, -1, -1);
                    mIsPlaying = true
                    updatePlaybackState()
                    updateMetaData(MusicList.getCurrentMusic())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setCurrentMusic()
        }
    }

    private fun stop(){
        mMediaPlayer?.run {
            MusicList.setCurrentPosition(currentPosition)
            stop()
            mIsPlaying = false
            updatePlaybackState()
            updateMetaData(MusicList.getCurrentMusic())
        }
    }

    private fun nextOnDelete(){
        play(MusicList.getCurrentMusicInt(), MusicList.FIRST_PROGRESS)
    }

    private fun next(){
        when(mCurrentMode){
            MODE_ONE_LOOP -> {
                if (MusicList.isLastMusic()) {
                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getNextPosition(), MusicList.FIRST_PROGRESS);
                }
            }
            MODE_ALL_LOOP -> {
                if (MusicList.isLastMusic()) {
                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getNextPosition(), MusicList.FIRST_PROGRESS);
                }
            }
            MODE_SEQUENCE -> {
                if (MusicList.isLastMusic()) {
                    CommonUtils.showShortToast(R.string.last_song_tip);
                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getNextPosition(), MusicList.FIRST_PROGRESS);
                }
            }
            MODE_RANDOM -> {
                play(getRandomPosition(), MusicList.FIRST_PROGRESS)
            }
        }
    }

    private fun previous(){
        when(mCurrentMode){
            MODE_ONE_LOOP -> {
                if (MusicList.isFirstMusic()) {
                    play(MusicList.getLastPosition(), MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getPreviousPosition(), MusicList.FIRST_PROGRESS);
                }
            }
            MODE_ALL_LOOP -> {
                if (MusicList.isFirstMusic()) {
                    play(MusicList.getLastPosition(), MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getPreviousPosition(), MusicList.FIRST_PROGRESS);
                }
            }
            MODE_SEQUENCE -> {
                if (MusicList.isFirstMusic()) {
                    CommonUtils.showShortToast(R.string.first_song_tip);
                    play(MusicList.getLastPosition(), MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getPreviousPosition(), MusicList.FIRST_PROGRESS);
                }
            }
            MODE_RANDOM -> {
                play(getRandomPosition(), MusicList.FIRST_PROGRESS)
            }
        }
    }

    private fun startPlayNormal(){
        play(MusicList.getCurrentMusicInt(), MusicList.getCurrentPosition())
    }

    /**
     * 设置通知栏播放状态按钮图标
     */
    private fun setRemoteViewPlayOrPause(){
        mNotificationRemoteView?.run {
            setImageViewResource(R.id.ivPauseOrPlay, if (!isPlaying()) R.mipmap.pausedetail else R.mipmap.run)
        }

        notifyNotification()
    }

    /**
     * 设置通知栏播放状态按钮图标(被动的)
     * */
    private fun setRemoteViewPlayOrPausePassive(){
        mNotificationRemoteView?.run {
            setImageViewResource(R.id.ivPauseOrPlay, if (isPlaying()) R.mipmap.pausedetail else R.mipmap.run)
        }

        notifyNotification()
    }

    private fun notifyNotification(){
        mNotificationManager?.run {
            mNotification?.run {
                notify(NOTIFICATION_ID, this)
            }
        }
    }

    private fun updatePlaybackState(){
        val state = if(isPlaying() || mIsPreparing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mMediaSessionCompat?.run {
            setPlaybackState(
                    PlaybackStateCompat.Builder()
                            .setActions(MEDIA_SESSION_ACTIONS)
                            .setState(state, MusicList.getCurrentPosition().toLong(), 1f)
                            .build())
        }
    }

    private fun updateMetaData(music: MusicInfo?){
        if(music == null){
            mMediaSessionCompat?.run {
                setMetadata(null)
            }
            return
        }

        val metaData = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, music.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, music.album)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, music.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, music.duration.toLong())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, CoverLoader.instance.loadThumb(music.albumId))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            metaData.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, MusicList.size().toLong())
        }

        mMediaSessionCompat?.run {
            setMetadata(metaData.build())
        }
    }

    /**
     * 处理音源焦点
     */
    @Synchronized
    private fun handleAudioFocusChanged(type : Int){
        when(type){
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                setRemoteViewPlayOrPause()
                if (CallObserver.instance.callPlay(SINGLE_CLICK)) {
                    stop()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                setRemoteViewPlayOrPause()
                if (CallObserver.instance.callPlay(SINGLE_CLICK)) {
                    stop()
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                setRemoteViewPlayOrPause()
                if (CallObserver.instance.callPlay(SINGLE_CLICK)) {
                    if (isPlaying()) {
                        stop()
                    } else {
                        startPlayNormal()
                    }
                }
            }
        }
    }

    private val mMediaSessionCompatCallback : MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            setRemoteViewPlayOrPause()

            if (CallObserver.instance.callPlay(SINGLE_CLICK)) {
                if (isPlaying()) {
                    stop()
                } else {
                    startPlayNormal()
                }
            }
        }

        override fun onPause() {
            super.onPause()
            setRemoteViewPlayOrPause()
            if (CallObserver.instance.callPlay(SINGLE_CLICK)) {
                if (isPlaying()) {
                    stop()
                } else {
                    startPlayNormal()
                }
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            setRemoteViewPlayOrPause()
            if (CallObserver.instance.callPlay(DOUBLE_CLICK)) {
                next()
            }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            setRemoteViewPlayOrPause()
            if(CallObserver.instance.callPlay(DOUBLE_CLICK)){
                previous()
            }
        }

        override fun onStop() {
            super.onStop()
            setRemoteViewPlayOrPause()
        }
    }

    private val mAudioFocusListener : AudioManager.OnAudioFocusChangeListener = object : AudioManager.OnAudioFocusChangeListener{
        override fun onAudioFocusChange(focusChange: Int) {
            sendMessage(FOCUS_CHANGE, null, focusChange, 0)
        }
    }

    /**
     * 处理通知栏主动控制的消息
     */
    @Synchronized
    private fun handleIntentCommand(intent: Intent?){
        intent?.run {
            val action = action ?: return

            when(action){
                NEXT_ACTION -> {
                    setRemoteViewPlayOrPause()
                    if (CallObserver.instance.callPlay(DOUBLE_CLICK)) {
                        next()
                    }
                }

                PAUSE_OR_PLAY_ACTION -> {
                    setRemoteViewPlayOrPause()
                    if (CallObserver.instance.callPlay(SINGLE_CLICK)) {
                        if (isPlaying()) {
                            stop()
                        } else {
                            startPlayNormal()
                        }
                    }
                }

                STOP_ACTION -> {
                    if (CallObserver.instance.isNeedCallObserver()) {
                        CallObserver.instance.stopUI()
                    } else {
                        stopSelf()
                    }

                    StatusBarUtil.collapseStatusBar(AppData.getApp().applicationContext)
                }

                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (isPlaying()) {
                        setRemoteViewPlayOrPause()
                        if (CallObserver.instance.isNeedCallObserver()) {
                            CallObserver.instance.callObserver(intent)
                        } else {
                            stop()
                        }
                    }
                }
            }
        }
    }

    private inner class IntentReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntentCommand(intent)
        }
    }

    inner class MyBinder : Binder(){
        fun startPlay(currentMusic: Int, currentPosition: Int){
            play(currentMusic, currentPosition)
            updateNotification()
        }

        fun stopPlay(){
            stop()
            updateNotification()
        }

        fun updateNotification(){
            setRemoteViewPlayOrPausePassive()
        }

        fun playNext(){
            next()
        }

        fun playNextOnDelete(){
            nextOnDelete()
        }

        fun playPrevious(){
            previous()
        }

        /**
         * MODE_ONE_LOOP = 1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         */
        fun changeMode(){
            mCurrentMode = (mCurrentMode + 1) % 4
        }

        fun getCurrentMode() : Int{
            return mCurrentMode
        }

        @Synchronized
        fun getIsPlaying() : Boolean{
            return isPlaying()
        }

        /**
         * Notify Activities to update the current music and duration when current activity changes.
         */
        fun notifyActivity(){
            toUpdateCurrentMusic()
            toUpdateDuration()
        }

        fun changeProgress(progress : Int){
            mMediaPlayer?.run {
                MusicList.setCurrentPosition(progress * 1000)
                if(this@MediaControllerService.isPlaying()){
                    seekTo(MusicList.getCurrentPosition())
                } else {
                    startPlayNormal()
                }
            }
        }

        fun notifyProgress(){
            toUpdateDuration()
            toUpdateProgress()
        }
    }

}