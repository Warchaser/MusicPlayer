package com.warchaser.musicplayer.display

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import com.warchaser.musicplayer.R
import com.warchaser.musicplayer.global.BaseActivity
import com.warchaser.musicplayer.tools.*
import com.warchaser.musicplayer.tools.MediaControllerService.MyBinder
import kotlinx.android.synthetic.main.display.*

class DisplayActivity : BaseActivity(), View.OnClickListener{

    private var mMyBinder : MyBinder? = null

    private var mObserver : UIUpdateObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display)
        connect2MediaService()
        initComponent()
    }

    private fun initComponent(){
        if(MusicList.isListNotEmpty()){
            mTvTitle.text = FormatHelper.formatTitle(MusicList.getCurrentMusic().title, 25)
        }

        mTvDuration.text = FormatHelper.formatDuration(MusicList.getCurrentMusicMax())
        mSeekProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    mMyBinder?.changeProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        mSeekProgress.max = MusicList.getCurrentMusicMax() / 1000
        mSeekProgress.progress = MusicList.getCurrentPosition() / 1000

        val width = ImageUtil.dp2Px(this, resources.getDimension(R.dimen.seek_bar_thumb_width))
        val thumbDrawable = ImageUtil.getNewDrawable(this, R.mipmap.thumb, width)
        mSeekProgress.thumb = thumbDrawable
        mTvTimeElapsed.text = FormatHelper.formatDuration(MusicList.getCurrentPosition())

        mObserver = UIUpdateObserver().apply {
            CallObserver.instance.registerObserver(this)
        }

        val intent = intent
        intent?.run {
            val albumId = getLongExtra("albumId", -1)
            refreshCover(albumId)
        }

        mLyBtnDisplayState.setOnClickListener(this)
        mBtnState.setOnClickListener(this)

        mLyBtnDisplayNext.setOnClickListener(this)
        mBtnDisplayNext.setOnClickListener(this)

        mLyBtnDisplayPrevious.setOnClickListener(this)
        mBtnDisplayPrevious.setOnClickListener(this)

        mLyIvMode.setOnClickListener(this)
        mIvMode.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        updatePlayButton()

        mObserver?.run {
            observerEnabled = true
        }

        mMyBinder?.run {
            notifyActivity()
        }
    }

    override fun onPause() {
        super.onPause()

        mObserver?.run {
            observerEnabled = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mMyBinder?.run {
            unbindService(mServiceConnection)
        }

        mObserver?.run {
            CallObserver.instance.removeSingleObserver(this)
        }

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.mLyBtnDisplayState, R.id.mBtnState -> play()
            R.id.mLyBtnDisplayNext, R.id.mBtnDisplayNext -> playNext()
            R.id.mLyBtnDisplayPrevious, R.id.mBtnDisplayPrevious -> playPrevious()
            R.id.mLyIvMode, R.id.mIvMode -> {
                mMyBinder?.changeMode()
                refreshModeButton()
            }
        }
    }

    private fun connect2MediaService(){
        Intent(this, MediaControllerService::class.java).apply {
            bindService(this, mServiceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun play(){
        mMyBinder?.run {
            if (isPlaying){
                stopPlay()
                mBtnState.setBackgroundResource(R.mipmap.run)
            } else {
                startPlay(MusicList.getCurrentMusicInt(), MusicList.getCurrentPosition())
                mBtnState.setBackgroundResource(R.mipmap.pausedetail)
            }
        }
    }

    private fun playNext(){
        mMyBinder?.run {
            playNext()
            if(isPlaying){
                mBtnState.setBackgroundResource(R.mipmap.pausedetail)
            }
        }
    }

    private fun playPrevious(){
        mMyBinder?.run {
            playPrevious()
            if(isPlaying){
                mBtnState.setBackgroundResource(R.mipmap.pausedetail)
            }
        }
    }

    private fun refreshModeButton(){
        mIvMode?.apply {
            when(mMyBinder?.currentMode){
                MediaControllerService.MODE_ONE_LOOP -> setBackgroundResource(R.mipmap.mode_loop_for_one)
                MediaControllerService.MODE_ALL_LOOP -> setBackgroundResource(R.mipmap.mode_loop)
                MediaControllerService.MODE_RANDOM -> setBackgroundResource(R.mipmap.mode_random)
                MediaControllerService.MODE_SEQUENCE -> setBackgroundResource(R.mipmap.mode_sequence)
                else -> {}
            }
        }
    }

    private fun updatePlayButton(){
        mMyBinder?.run {
            mBtnState.setBackgroundResource(if(isPlaying) R.mipmap.pausedetail else R.mipmap.run)
        }
    }

    private fun refreshCover(albumId : Long){
        mIvCover?.run {
            setImageBitmap(CoverLoader.instance.loadDisplayCover(albumId))
        }
    }

    private var mServiceConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mMyBinder = service as MyBinder
            updatePlayButton()

            refreshModeButton()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private inner class UIUpdateObserver : UIObserver{

        private var mIsEnable : Boolean = false

        override fun notifySeekBar2Update(intent: Intent?) {
            intent?.run {
                when(intent.action){
                    MediaControllerService.ACTION_UPDATE_PROGRESS -> {
                        val progress = intent.getIntExtra(MediaControllerService.ACTION_UPDATE_PROGRESS, MusicList.getCurrentPosition())
                        if(progress > 0){
                            MusicList.setCurrentPosition(progress)// Remember the current position
                            mTvTimeElapsed.text = FormatHelper.formatDuration(progress)
                            mSeekProgress.progress = progress / 1000
                        } else {

                        }
                    }
                    MediaControllerService.ACTION_UPDATE_CURRENT_MUSIC -> {
                        //Retrieve the current music and get the title to show on top of the screen.
                        if(MusicList.size() != 0){
                            MusicList.setCurrentMusic(intent.getIntExtra(MediaControllerService.ACTION_UPDATE_CURRENT_MUSIC, 0))
                            val bean = MusicList.getCurrentMusic()
                            mTvTitle.text = FormatHelper.formatTitle(bean.title, 25)
                            refreshCover(bean.albumId)
                        } else {

                        }
                    }
                    MediaControllerService.ACTION_UPDATE_DURATION -> {
                        //Receive the duration and show under the progress bar
                        //Why do this? because from the ContentResolver, the duration is zero.
                        val duration = intent.getIntExtra(MediaControllerService.ACTION_UPDATE_DURATION, 0)
                        mTvDuration.text = FormatHelper.formatDuration(duration)
                        mSeekProgress.max = duration / 1000
                    }
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        mMyBinder?.run {
                            if(isPlaying){
                                stopPlay()
                                mBtnState.setBackgroundResource(R.mipmap.run)
                            }
                        }
                    }
                    else -> {

                    }
                }
            }
        }

        override fun notify2Play(repeatTime: Int) {
            when(repeatTime){
                MediaControllerService.SINGLE_CLICK -> play()
                MediaControllerService.DOUBLE_CLICK -> playNext()
            }
        }

        override var observerEnabled : Boolean
            get() = mIsEnable
            set(value) {
                mIsEnable = value
            }

        override fun stopServiceAndExit() {
            finish()
        }
    }
}