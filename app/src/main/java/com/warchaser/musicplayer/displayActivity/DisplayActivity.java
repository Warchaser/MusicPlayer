package com.warchaser.musicplayer.displayActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.globalInfo.BaseActivity;
import com.warchaser.musicplayer.tools.CallObserver;
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

//    /**
//     * Button, Play Previous
//     * */
//    @BindView(R.id.btnDisplayPrevious)
//    Button mBtnPrevious;
//
//    /**
//     * Button, Play Next
//     * */
//    @BindView(R.id.btnDisplayNext)
//    Button mBtnNext;

    /**
     * Button, Current Music Playing State
     */
    @BindView(R.id.btnDisplayState)
    Button mBtnState;

//    @BindView(R.id.lyIvMode)
//    LinearLayout mLyBtnMode;
//
//    @BindView(R.id.lyBtnDisplayPrevious)
//    LinearLayout mLyBtnDisplayPrevious;
//
//    @BindView(R.id.lyBtnDisplayState)
//    LinearLayout mLyBtnDisplayState;
//
//    @BindView(R.id.lyBtnDisplayNext)
//    LinearLayout mLyBtnDisplayNext;

    private UIUpdateObserver mObserver;

    private Unbinder mUnbinder;

    private float mCoverWidth = 0;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMyBinder = (MyBinder) service;
            updatePlayButton();

            switch (mMyBinder.getCurrentMode()) {
                case 0:
                    mIvMode.setBackgroundResource(R.mipmap.mode_loop_for_one);
                    break;
                case 1:
                    mIvMode.setBackgroundResource(R.mipmap.mode_loop);
                    break;
                case 2:
                    mIvMode.setBackgroundResource(R.mipmap.mode_random);
                    break;
                case 3:
                    mIvMode.setBackgroundResource(R.mipmap.mode_sequence);
                    break;
            }
        }
    };

    private void connectToMyService() {
        Intent intent = new Intent(DisplayActivity.this, MyService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);
        mUnbinder = ButterKnife.bind(this);
        connectToMyService();
        initComponent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyBinder != null) {
            unbindService(mServiceConnection);
        }

        if (mUnbinder != null) {
            mUnbinder.unbind();
        }

        mUnbinder = null;

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
                switch (mMyBinder.getCurrentMode()) {
                    case 0:
                        mIvMode.setBackgroundResource(R.mipmap.mode_loop_for_one);
                        break;
                    case 1:
                        mIvMode.setBackgroundResource(R.mipmap.mode_loop);
                        break;
                    case 2:
                        mIvMode.setBackgroundResource(R.mipmap.mode_random);
                        break;
                    case 3:
                        mIvMode.setBackgroundResource(R.mipmap.mode_sequence);
                        break;
                }
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
        if (!MusicList.musicInfoList.isEmpty()) {
            mTvTitle.setText(FormatHelper.formatTitle(MusicList.getCurrentMusic().getTitle(), 25));
        }

        mTvDuration.setText(FormatHelper.formatDuration(MusicList.iCurrentMax));
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
        mSeekProgress.setMax(MusicList.iCurrentMax / 1000);
        mSeekProgress.setProgress(MusicList.iCurrentPosition / 1000);

        mTvTimeElapsed.setText(FormatHelper.formatDuration(MusicList.iCurrentPosition));

        mObserver = new UIUpdateObserver();
        CallObserver.setObserver(mObserver);

        Intent intent = getIntent();
        if (intent != null) {
            final String uri = intent.getStringExtra("uri");

            mIvCover.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mCoverWidth = mIvCover.getMeasuredWidth() - mIvCover.getPaddingLeft() - mIvCover.getPaddingRight();
                    ImageUtil.setBottomBarDisc(DisplayActivity.this, uri, mCoverWidth, mIvCover, R.mipmap.disc, false);
                    mIvCover.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

    }

    private void play() {
        if (mMyBinder.getIsPlaying()) {
            mMyBinder.stopPlay();
            mBtnState.setBackgroundResource(R.mipmap.run);
        } else {
            mMyBinder.startPlay(MusicList.iCurrentMusic, MusicList.iCurrentPosition);
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

    private class UIUpdateObserver implements UIObserver {
        private boolean mIsEnabled;

        @Override
        public void notifySeekBar2Update(Intent intent) {
            String action = intent.getAction();
            if (MyService.ACTION_UPDATE_PROGRESS.equals(action)) {
                int progress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS, MusicList.iCurrentPosition);
                if (progress > 0) {
                    MusicList.iCurrentPosition = progress; // Remember the current position
                    mTvTimeElapsed.setText(FormatHelper.formatDuration(progress));
                    mSeekProgress.setProgress(progress / 1000);
                }
            } else if (MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action) && MusicList.musicInfoList.size() != 0) {
                //Retrieve the current music and get the title to show on top of the screen.
                MusicList.iCurrentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                MusicInfo bean = MusicList.getCurrentMusic();
                mTvTitle.setText(FormatHelper.formatTitle(bean.getTitle(), 25));
                ImageUtil.setBottomBarDisc(DisplayActivity.this, bean.getUriWithCoverPic(), mCoverWidth, mIvCover, R.mipmap.disc, false);

            } else if (MyService.ACTION_UPDATE_DURATION.equals(action)) {
                //Receive the duration and show under the progress bar
                //Why do this ? because from the ContentResolver, the duration is zero.
                int duration = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                mTvDuration.setText(FormatHelper.formatDuration(duration));
                mSeekProgress.setMax(duration / 1000);
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                play();
            }
        }

        @Override
        public void notify2Play(int repeatTime) {
            switch (repeatTime) {
                case 1:
                    play();
                    break;
                case 2:
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
