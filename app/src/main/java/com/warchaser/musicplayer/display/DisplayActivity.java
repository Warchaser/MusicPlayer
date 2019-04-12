package com.warchaser.musicplayer.display;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.global.BaseActivity;
import com.warchaser.musicplayer.tools.CallObserver;
import com.warchaser.musicplayer.tools.CoverLoader;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.ImageUtil;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;
import com.warchaser.musicplayer.tools.MyService;
import com.warchaser.musicplayer.tools.MyService.MyBinder;
import com.warchaser.musicplayer.tools.UIObserver;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by Wu on 2014/10/22.
 */
public class DisplayActivity extends BaseActivity implements OnClickListener {

    private MyBinder mMyBinder;

    /**
     * SeekBar, Playing Progress
     */
    @BindView(R.id.progress)
    SeekBar mSeekProgress;

    /**
     * TextView, Current Playing Music's Title
     */
    @BindView(R.id.tvDisplayCurrentTitle)
    TextView mTvTitle;

    /**
     * TextView, Current Playing Time Elapsed
     */
    @BindView(R.id.tvDisplayTimeElapsed)
    TextView mTvTimeElapsed;

    /**
     * TextView, Current Music Playing's Duration
     */
    @BindView(R.id.tvDisplayDuration)
    TextView mTvDuration;

    /**
     * ImageView, Current Playing Mode
     */
    @BindView(R.id.ivMode)
    ImageView mIvMode;

    /**
     * ImageView, Current Music's Cover
     */
    @BindView(R.id.iv_cover)
    ImageView mIvCover;

    /**
     * Button, Current Music Playing State
     */
    @BindView(R.id.btnDisplayState)
    Button mBtnState;

    private UIUpdateObserver mObserver;

    private Unbinder mUnBinder;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMyBinder = (MyBinder) service;
            updatePlayButton();

            refreshModeButton();
        }
    };

    private void connectToMyService() {
        Intent intent = new Intent(DisplayActivity.this, MyService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void refreshModeButton(){
        switch (mMyBinder.getCurrentMode()) {
            case MyService.MODE_ONE_LOOP:
                mIvMode.setBackgroundResource(R.mipmap.mode_loop_for_one);
                break;
            case MyService.MODE_ALL_LOOP:
                mIvMode.setBackgroundResource(R.mipmap.mode_loop);
                break;
            case MyService.MODE_RANDOM:
                mIvMode.setBackgroundResource(R.mipmap.mode_random);
                break;
            case MyService.MODE_SEQUENCE:
                mIvMode.setBackgroundResource(R.mipmap.mode_sequence);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);
        mUnBinder = ButterKnife.bind(this);
        connectToMyService();
        initComponent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyBinder != null) {
            unbindService(mServiceConnection);
        }

        if (mUnBinder != null) {
            mUnBinder.unbind();
        }

        mUnBinder = null;

        CallObserver.removeSingleObserver(mObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mObserver != null) {
            mObserver.setObserverEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePlayButton();

        if (mObserver != null) {
            mObserver.setObserverEnabled(true);
        }

        if (mMyBinder != null) {
            mMyBinder.notifyActivity();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @OnClick({R.id.lyBtnDisplayState, R.id.btnDisplayState, R.id.lyBtnDisplayNext,
            R.id.btnDisplayNext, R.id.lyBtnDisplayPrevious, R.id.btnDisplayPrevious,
            R.id.lyIvMode, R.id.ivMode})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lyBtnDisplayState:
            case R.id.btnDisplayState:
                play();
                break;

            case R.id.lyBtnDisplayNext:
            case R.id.btnDisplayNext:
                playNext();
                break;

            case R.id.lyBtnDisplayPrevious:
            case R.id.btnDisplayPrevious:
                playPrevious();
                break;

            case R.id.lyIvMode:
            case R.id.ivMode:
                mMyBinder.changeMode();
                refreshModeButton();
                break;
            default:
                break;
        }
    }

    private void playNext() {
        mMyBinder.playNext();
        if (mMyBinder.getIsPlaying()) {
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void playPrevious() {
        mMyBinder.playPrevious();
        if (mMyBinder.getIsPlaying()) {
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void initComponent() {
        if (!MusicList.isListEmpty()) {
            mTvTitle.setText(FormatHelper.formatTitle(MusicList.getCurrentMusic().getTitle(), 25));
        }

        mTvDuration.setText(FormatHelper.formatDuration(MusicList.getCurrentMusicMax()));
        mSeekProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    mMyBinder.changeProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekProgress.setMax(MusicList.getCurrentMusicMax() / 1000);
        mSeekProgress.setProgress(MusicList.getCurrentPosition() / 1000);

        final int width = ImageUtil.dp2Px(this, getResources().getDimension(R.dimen.seek_bar_thumb_width));
        Drawable thumbDrawable = ImageUtil.getNewDrawable(this, R.mipmap.thumb, width);
        mSeekProgress.setThumb(thumbDrawable);

        mTvTimeElapsed.setText(FormatHelper.formatDuration(MusicList.getCurrentPosition()));

        mObserver = new UIUpdateObserver();
        CallObserver.registerObserver(mObserver);

        Intent intent = getIntent();
        if (intent != null) {
            final long albumId = intent.getLongExtra("albumId", -1);
            refreshCover(albumId);
        }

    }

    private void play() {
        if (mMyBinder.getIsPlaying()) {
            mMyBinder.stopPlay();
            mBtnState.setBackgroundResource(R.mipmap.run);
        } else {
            mMyBinder.startPlay(MusicList.getCurrentMusicInt(), MusicList.getCurrentPosition());
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void updatePlayButton() {
        if (mMyBinder != null) {
            if (mMyBinder.getIsPlaying()) {
                mBtnState.setBackgroundResource(R.mipmap.pausedetail);
            } else {
                mBtnState.setBackgroundResource(R.mipmap.run);
            }
        }
    }

    private void refreshCover(long albumId){
        if(mIvCover != null){
            mIvCover.setImageBitmap(CoverLoader.get().loadDisplayCover(albumId));
        }
    }

    private class UIUpdateObserver implements UIObserver {
        private boolean mIsEnabled;

        @Override
        public void notifySeekBar2Update(Intent intent) {
            String action = intent.getAction();
            if (MyService.ACTION_UPDATE_PROGRESS.equals(action)) {
                int progress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS, MusicList.getCurrentPosition());
                if (progress > 0) {
                    MusicList.setCurrentPosition(progress);// Remember the current position
                    mTvTimeElapsed.setText(FormatHelper.formatDuration(progress));
                    mSeekProgress.setProgress(progress / 1000);
                }
            } else if (MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action) && MusicList.size() != 0) {
                //Retrieve the current music and get the title to show on top of the screen.
                MusicList.setCurrentMusic(intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0));
                MusicInfo bean = MusicList.getCurrentMusic();
                mTvTitle.setText(FormatHelper.formatTitle(bean.getTitle(), 25));
                refreshCover(bean.getAlbumId());
            } else if (MyService.ACTION_UPDATE_DURATION.equals(action)) {
                //Receive the duration and show under the progress bar
                //Why do this ? because from the ContentResolver, the duration is zero.
                int duration = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                mTvDuration.setText(FormatHelper.formatDuration(duration));
                mSeekProgress.setMax(duration / 1000);
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                if(mMyBinder.getIsPlaying()){
                    mMyBinder.stopPlay();
                    mBtnState.setBackgroundResource(R.mipmap.run);
                }
            }
        }

        @Override
        public void notify2Play(int repeatTime) {
            switch (repeatTime) {
                case MyService.SINGLE_CLICK:
                    play();
                    break;
                case MyService.DOUBLE_CLICK:
                    playNext();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void setObserverEnabled(boolean enabled) {
            this.mIsEnabled = enabled;
        }

        @Override
        public boolean getObserverEnabled() {
            return mIsEnabled;
        }

        @Override
        public void stopServiceAndExit() {
            finish();
        }
    }
}
