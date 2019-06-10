package com.warchaser.musicplayer.tools;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.global.AppData;
import com.warchaser.musicplayer.main.OnAirActivity;

import java.lang.ref.WeakReference;

import androidx.core.app.NotificationCompat;

import hugo.weaving.DebugLog;

/**
 * Created by Wu on 2014/10/20.
 * Playing-Service
 */
public class MyService extends Service {

    private MediaPlayer mMediaPlayer;

    /**
     * MediaPlayer是否正在播放音乐
     * 这里增设标志位是因为MediaPlayer.isPlaying()方法在部分魔改ROM下
     * 无法正常返回状态且有时会抛异常
     * */
    private boolean mIsPlaying = false;

    private final String CHANNEL_ID = "com.warchaser.MusicPlayer.notification";

    /**
     * 用于Handler
     */
    private static final int UPDATE_PROGRESS = 1;
    private static final int UPDATE_CURRENT_MUSIC = 2;
    private static final int UPDATE_DURATION = 3;
    private static final int FOCUS_CHANGE = 4;
    //用于Handler

    public static final String ACTION_UPDATE_PROGRESS = "com.warchaser.MusicPlayer.UPDATE_PROGRESS";
    public static final String ACTION_UPDATE_DURATION = "com.warchaser.MusicPlayer.UPDATE_DURATION";
    public static final String ACTION_UPDATE_CURRENT_MUSIC = "com.warchaser.MusicPlayer.UPDATE_CURRENT_MUSIC";

    public static final String PAUSE_OR_PLAY_ACTION = "com.warchaser.MusicPlayer.playOrPause";
    public static final String NEXT_ACTION = "com.warchaser.MusicPlayer.next";
    public static final String STOP_ACTION = "com.warchaser.MusicPlayer.close";

    private final String MEDIA_SESSION_TAG = "com.warchaser.MusicPlayer.tools.MyService";

    private final int PAUSE_FLAG = 0x11;
    private final int NEXT_FLAG = 0x12;
    private final int STOP_FLAG = 0x13;

    private final int NOTIFICATION_ID = 1001;

    private int mCurrentMode = MODE_SEQUENCE; //default sequence playing

    /**
     * 播放模式
     */
    public static final int MODE_ONE_LOOP = 0;
    public static final int MODE_ALL_LOOP = 1;
    public static final int MODE_RANDOM = 2;
    public static final int MODE_SEQUENCE = 3;

    /**
     * MediaSessionCompat Flags
     * */
    private static final long MEDIA_SESSION_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;

    private MyBinder mMyBinder = new MyBinder();
    private MessageHandler mMessageHandler;

    private AudioManager mAudioManager;

    /**
     * Custom Notification's RemoteViews
     * */
    private RemoteViews mNotificationRemoteView;

    /**
     * Notification
     * */
    private Notification mNotification;

    /**
     * Notification's Manager
     * */
    private NotificationManager mNotificationManager;

    /**
     * IntentReceiver
     * For receiving Notification's clicking actions and AUDIO-BECOMING_NOISY action.
     * */
    private IntentReceiver mIntentReceiver;

    /**
     * MediaSessionCompat
     * For receiving media button actions.
     * Earphones and HeadSets.
     * */
    private MediaSessionCompat mMediaSessionCompat;

    public static final int SINGLE_CLICK = 1;
    public static final int DOUBLE_CLICK = 2;

    private boolean mIsPreparing = false;

    @Override
    public void onCreate() {
        MusicList.instance(getContentResolver());

        mMessageHandler = new MessageHandler(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initMediaPlayer();

        super.onCreate();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initializeReceiver();

        initializeMediaSessionCompat();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mMessageHandler != null) {
            mMessageHandler.removeCallbacksAndMessages(null);
            mMessageHandler = null;
        }

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }

        if(mIntentReceiver != null){
            unregisterReceiver(mIntentReceiver);
            mIntentReceiver = null;
        }

        releaseMediaSessionCompat();
    }

    private void initializeReceiver() {
        mIntentReceiver = new IntentReceiver();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PAUSE_OR_PLAY_ACTION);
        intentFilter.addAction(NEXT_ACTION);
        intentFilter.addAction(STOP_ACTION);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);//拔出耳机

        registerReceiver(mIntentReceiver, intentFilter);
    }

    private void initializeMediaSessionCompat(){
        mMediaSessionCompat = new MediaSessionCompat(this, MEDIA_SESSION_TAG);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mMediaSessionCompat.setCallback(mMediaSessionCompatCallback);
        mMediaSessionCompat.setActive(true);
    }

    private void releaseMediaSessionCompat(){
        if(mMediaSessionCompat != null){
            mMediaSessionCompat.setCallback(null);
            mMediaSessionCompat.setActive(false);
            mMediaSessionCompat.release();
            mMediaSessionCompat = null;
        }
    }

    /**
     * 向MessageHandler发送消息
     * */
    private void sendMessage(int what, Object object, int arg1, int arg2){
        if(mMessageHandler == null){
            return;
        }

        if(object == null){

            if(arg1 == -1 && arg2 == -1){
                mMessageHandler.obtainMessage(what).sendToTarget();
            } else {
                mMessageHandler.obtainMessage(what, arg1, arg2).sendToTarget();
            }

        } else {
            mMessageHandler.obtainMessage(what, object).sendToTarget();
        }
    }

    /**
     * 向MessageHandler发送延迟消息
     * */
    private void sendMessageDelayed(int what, final long delayed){
        if(mMessageHandler == null){
            return;
        }

        mMessageHandler.sendEmptyMessageDelayed(what, delayed);
    }

    /**
     * MessageHandler
     * */
    private static class MessageHandler extends Handler {

        private WeakReference<MyService> mServiceWeakReference;

        MessageHandler(MyService service) {
            mServiceWeakReference = new WeakReference<>(service);
        }

        public void handleMessage(Message msg) {
            final MyService service = mServiceWeakReference.get();

            switch (msg.what) {
                case UPDATE_PROGRESS:
                    service.toUpdateProgress();
                    break;
                case UPDATE_CURRENT_MUSIC:
                    service.toUpdateCurrentMusic();
                    break;
                case UPDATE_DURATION:
                    service.toUpdateDuration();
                    break;
                case FOCUS_CHANGE:
                    service.handleAudioFocusChanged(msg.arg1);
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * 处理音源焦点
     */
    private synchronized void handleAudioFocusChanged(int type) {
        switch (type) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                setRemoteViewPlayOrPause();
                if (CallObserver.callPlay(SINGLE_CLICK)) {
                    stop();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                setRemoteViewPlayOrPause();
                if (CallObserver.callPlay(SINGLE_CLICK)) {
                    stop();
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                setRemoteViewPlayOrPause();
                if (CallObserver.callPlay(SINGLE_CLICK)) {
                    if (isPlaying()) {
                        stop();
                    } else {
                        startPlayNormal();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void toUpdateProgress() {
        if (mMediaPlayer != null && isPlaying() && CallObserver.isNeedCallObserver()) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_PROGRESS);
            intent.putExtra(ACTION_UPDATE_PROGRESS, currentPosition);

            CallObserver.callObserver(intent);

            //每1秒发送一次广播，进度条每秒更新
            sendMessageDelayed(UPDATE_PROGRESS, 1000);
        }
    }

    @DebugLog
    private void toUpdateCurrentMusic() {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_CURRENT_MUSIC);
        intent.putExtra(ACTION_UPDATE_CURRENT_MUSIC, MusicList.getCurrentMusicInt());

        startForeground(NOTIFICATION_ID, getNotification());

        CallObserver.callObserver(intent);
    }

    @DebugLog
    private Notification getNotification() {
        final String notificationTitle;
        mNotificationRemoteView = new RemoteViews(this.getPackageName(), R.layout.notification);

        MusicInfo bean;

        if (!MusicList.isListNotEmpty()) {
            notificationTitle = "Mr.Song is not here for now……";
            mNotificationRemoteView.setImageViewResource(R.id.fileImage, R.mipmap.disc);
        } else {
            bean = MusicList.getCurrentMusic();
            notificationTitle = bean.getTitle();
            String uriString = bean.getUriWithCoverPic();
            if (TextUtils.isEmpty(uriString)) {
                mNotificationRemoteView.setImageViewResource(R.id.fileImage, R.mipmap.disc);
            } else {
                Bitmap bitmap = CoverLoader.get().loadThumb(bean.getAlbumId());
                if (bitmap == null) {
                    mNotificationRemoteView.setImageViewResource(R.id.fileImage, R.mipmap.disc);
                } else {
                    mNotificationRemoteView.setImageViewBitmap(R.id.fileImage, bitmap);
                }
            }
        }

        mNotificationRemoteView.setTextViewText(R.id.fileName, notificationTitle);

        Intent pauseIntent = new Intent(PAUSE_OR_PLAY_ACTION);
        pauseIntent.putExtra("FLAG", PAUSE_FLAG);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.ivPauseOrPlay, pausePendingIntent);

        mNotificationRemoteView.setImageViewResource(R.id.ivPauseOrPlay, isPlaying() ? R.mipmap.pausedetail : R.mipmap.run);

        Intent nextIntent = new Intent(NEXT_ACTION);
        nextIntent.putExtra("FLAG", NEXT_FLAG);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.ivNext, nextPendingIntent);

        Intent closeIntent = new Intent(STOP_ACTION);
        closeIntent.putExtra("FLAG", STOP_FLAG);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, closeIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.ivClose, closePendingIntent);

        Intent activityIntent = new Intent(MyService.this, OnAirActivity.class);
        //clear the top of the stack, this flag can forbid the possibility of the two activities
        //existing at the same time
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(MyService.this, 0, activityIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.lyRoot, activityPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContent(mNotificationRemoteView)
                .setSmallIcon(R.mipmap.notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "MyService", NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
        }

        builder.setOnlyAlertOnce(true);
        mNotification = builder.build();
        mNotification.flags |= Notification.FLAG_NO_CLEAR;

        return mNotification;
    }

    private void toUpdateDuration() {
        if (mMediaPlayer != null) {
            int duration = mMediaPlayer.getDuration();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_DURATION);
            intent.putExtra(ACTION_UPDATE_DURATION, duration);

            CallObserver.callObserver(intent);
        }
    }

    public void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer pMediaPlayer) {
                mIsPreparing = false;
                mMediaPlayer.seekTo(MusicList.getCurrentPosition());
                mMediaPlayer.start();

                sendMessage(UPDATE_DURATION, null, -1, -1);
            }
        });

        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer pMediaPlayer) {
                if (isPlaying()) {

                    int currentPosition = mMediaPlayer.getCurrentPosition();

                    if (isEmptyInFile(currentPosition)) {
                        mMediaPlayer.seekTo(currentPosition + 1000);
                        mMediaPlayer.start();
                        return;
                    }

                    switch (mCurrentMode) {
                        case MODE_ONE_LOOP:
                            mMediaPlayer.start();
                            break;
                        case MODE_ALL_LOOP:
                            play((MusicList.getNextPosition()) % MusicList.size(), MusicList.FIRST_PROGRESS);
                            break;
                        case MODE_RANDOM:
                            play(getRandomPosition(), MusicList.FIRST_PROGRESS);
                            break;
                        case MODE_SEQUENCE:
                            if (MusicList.isLastMusic()) {
                                play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                            } else {
                                next();
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                NLog.eWithFile("MyService", "MediaPlayer has met a problem! what: " + i + " extra: " + i2);
                return false;
            }
        });

    }

    private boolean isEmptyInFile(int currentPosition) {
        return currentPosition < mMediaPlayer.getDuration();
    }

    private void setCurrentMusic() {
        sendMessage(UPDATE_CURRENT_MUSIC, null, -1, -1);
    }

    private int getRandomPosition() {
        return (int) (Math.random() * (MusicList.getLastPosition()));
    }

    private boolean isPlaying(){
        return mIsPlaying;
    }

    private void play(int currentMusic, int currentProgress) {
        int status = mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        MusicList.setCurrentPosition(currentProgress);
        MusicList.setCurrentMusic(currentMusic);

        mMediaPlayer.stop();
        mMediaPlayer.reset();

        try {
            if (MusicList.isListNotEmpty()) {
                mMediaPlayer.setDataSource(MusicList.getCurrentMusic().getUrl());
                mMediaPlayer.prepareAsync();
                mIsPreparing = true;
                sendMessage(UPDATE_PROGRESS, null, -1, -1);
                mIsPlaying = true;
                updatePlaybackState();
                updateMetaData(MusicList.getCurrentMusic());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        setCurrentMusic();
    }

    private void stop() {

        MusicList.setCurrentPosition(mMediaPlayer.getCurrentPosition());

        mMediaPlayer.stop();
        mIsPlaying = false;
    }

    private void nextOnDelete(){
        play(MusicList.getCurrentMusicInt(), MusicList.FIRST_PROGRESS);
    }

    private void next() {
        switch (mCurrentMode) {
            case MODE_ONE_LOOP:
                if (MusicList.isLastMusic()) {
                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getNextPosition(), MusicList.FIRST_PROGRESS);
                }
                break;
            case MODE_ALL_LOOP:
                if (MusicList.isLastMusic()) {
                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getNextPosition(), MusicList.FIRST_PROGRESS);
                }
                break;
            case MODE_SEQUENCE:
                if (MusicList.isLastMusic()) {
                    CommonUtils.showShortToast(R.string.last_song_tip);
                    play(MusicList.FIRST_POSITION, MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getNextPosition(), MusicList.FIRST_PROGRESS);
                }
                break;
            case MODE_RANDOM:
                play(getRandomPosition(), MusicList.FIRST_PROGRESS);
                break;
        }
    }

    private void previous() {
        switch (mCurrentMode) {
            case MODE_ONE_LOOP:
                if (MusicList.isFirstMusic()) {
                    play(MusicList.getLastPosition(), MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getPreviousPosition(), MusicList.FIRST_PROGRESS);
                }
                break;
            case MODE_ALL_LOOP:
                if (MusicList.isFirstMusic()) {
                    play(MusicList.getLastPosition(), MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getPreviousPosition(), MusicList.FIRST_PROGRESS);
                }
                break;
            case MODE_SEQUENCE:
                if (MusicList.isFirstMusic()) {
                    CommonUtils.showShortToast(R.string.first_song_tip);
                    play(MusicList.getLastPosition(), MusicList.FIRST_PROGRESS);
                } else {
                    play(MusicList.getPreviousPosition(), MusicList.FIRST_PROGRESS);
                }
                break;
            case MODE_RANDOM:
                play(getRandomPosition(), MusicList.FIRST_PROGRESS);
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        MusicList.mMyBinder = mMyBinder;
        return mMyBinder;
    }

    public void startPlayNormal() {
        play(MusicList.getCurrentMusicInt(), MusicList.getCurrentPosition());
    }

    /**
     * 设置通知栏播放状态按钮图标
     */
    private void setRemoteViewPlayOrPause() {
        if (mNotificationRemoteView != null) {
            mNotificationRemoteView.setImageViewResource(R.id.ivPauseOrPlay, !isPlaying() ? R.mipmap.pausedetail : R.mipmap.run);
        }

        notifyNotification();
    }

    /**
     * 设置通知栏播放状态按钮图标(被动的)
     * */
    private void setRemoteViewPlayOrPausePassive() {
        if (mNotificationRemoteView != null) {
            mNotificationRemoteView.setImageViewResource(R.id.ivPauseOrPlay, isPlaying() ? R.mipmap.pausedetail : R.mipmap.run);
        }

        notifyNotification();
    }

    private void notifyNotification(){
        if (mNotificationManager != null && mNotification != null) {
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    /**
     * 处理通知栏主动控制的消息
     */
    private synchronized void handleIntentCommand(Intent intent) {

        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        if (NEXT_ACTION.equals(action)) {
            setRemoteViewPlayOrPause();
            if (CallObserver.callPlay(DOUBLE_CLICK)) {
                next();
            }
        } else if (PAUSE_OR_PLAY_ACTION.equals(action)) {
            setRemoteViewPlayOrPause();
            if (CallObserver.callPlay(SINGLE_CLICK)) {
                if (isPlaying()) {
                    stop();
                } else {
                    startPlayNormal();
                }
            }

        } else if (STOP_ACTION.equals(action)) {
            if(CallObserver.isNeedCallObserver()){
                CallObserver.stopUI();
            } else {
                stopSelf();
            }

            StatusBarUtil.collapseStatusBar(AppData.getApp().getApplicationContext());
        } else if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)){
            if(isPlaying()){
                setRemoteViewPlayOrPause();
                if(CallObserver.isNeedCallObserver()){
                    CallObserver.callObserver(new Intent().setAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                } else {
                    stop();
                }
            }
        }
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(final int focusChange) {
            sendMessage(FOCUS_CHANGE, null, focusChange, 0);
        }
    };

    public void updatePlaybackState(){
        int state = (isPlaying() || mIsPreparing) ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        if(mMediaSessionCompat != null){
            mMediaSessionCompat.setPlaybackState(
                    new PlaybackStateCompat.Builder()
                            .setActions(MEDIA_SESSION_ACTIONS)
                            .setState(state, MusicList.getCurrentPosition(), 1)
                            .build());
        }
    }

    public void updateMetaData(MusicInfo music) {
        if (music == null) {
            if(mMediaSessionCompat != null){
                mMediaSessionCompat.setMetadata(null);
            }
            return;
        }

        MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, music.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, music.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, music.getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, music.getDuration())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, CoverLoader.get().loadThumb(music.getAlbumId()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            metaData.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, MusicList.size());
        }

        if(mMediaSessionCompat != null){
            mMediaSessionCompat.setMetadata(metaData.build());
        }
    }

    private final MediaSessionCompat.Callback mMediaSessionCompatCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            super.onPlay();
            setRemoteViewPlayOrPause();
            if (CallObserver.callPlay(SINGLE_CLICK)) {
                if (isPlaying()) {
                    stop();
                } else {
                    startPlayNormal();
                }
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            setRemoteViewPlayOrPause();
            if (CallObserver.callPlay(SINGLE_CLICK)) {
                if (isPlaying()) {
                    stop();
                } else {
                    startPlayNormal();
                }
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            setRemoteViewPlayOrPause();
            if (CallObserver.callPlay(DOUBLE_CLICK)) {
                next();
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            setRemoteViewPlayOrPause();
        }

        @Override
        public void onStop() {
            super.onStop();
            setRemoteViewPlayOrPause();
        }
    };

    public class MyBinder extends Binder {
        public void startPlay(int currentMusic, int currentPosition) {
            play(currentMusic, currentPosition);
            updateNotification();
        }

        public void stopPlay() {
            stop();
            updateNotification();
        }

        public void updateNotification() {
            setRemoteViewPlayOrPausePassive();
        }

        public void playNext() {
            next();
        }

        public void playNextOnDelete(){
            nextOnDelete();
        }

        public void playPrevious() {
            previous();
        }

        /**
         * MODE_ONE_LOOP = 1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         */
        public void changeMode() {
            mCurrentMode = (mCurrentMode + 1) % 4;
        }

        public int getCurrentMode() {
            return mCurrentMode;
        }

        public synchronized boolean getIsPlaying() {
            return isPlaying();
        }

        /**
         * Notify Activities to update the current music and duration when current activity changes.
         */
        public void notifyActivity() {
            toUpdateCurrentMusic();
            toUpdateDuration();
        }

        /**
         * seekBar action
         */
        public void changeProgress(int progress) {
            if (mMediaPlayer != null) {
                MusicList.setCurrentPosition(progress * 1000);
                if (isPlaying()) {
                    mMediaPlayer.seekTo(MusicList.getCurrentPosition());
                } else {
                    startPlayNormal();
                }
            }
        }

        public void notifyProgress() {
            toUpdateDuration();
            toUpdateProgress();
        }
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntentCommand(intent);
        }
    }
}
