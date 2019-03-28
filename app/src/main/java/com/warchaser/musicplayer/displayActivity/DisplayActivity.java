package com.warchaser.musicplayer.displayActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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

import java.lang.ref.WeakReference;

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

    private float mCoverWidth = 0;

    private static final int REFRESH_PLAYING_STATE = 1;
    private static final int REFRESH_DISC = 2;
    private static final int REFRESH_PROGRESS = 3;
    private static final int REFRESH_DURATION = 4;

    private MessageHandler mMessageHandler;

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

        if(mMessageHandler != null){
            mMessageHandler.removeCallbacksAndMessages(null);
            mMessageHandler = null;
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

        mMessageHandler = new MessageHandler(this);

        if (!MusicList.isListEmpty()) {
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
        sendMessage(REFRESH_PLAYING_STATE, null);
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

    private void refreshPlayingStatePassively(){
        if (mMyBinder.getIsPlaying()) {
            mMyBinder.stopPlay();
            mBtnState.setBackgroundResource(R.mipmap.run);
        } else {
            mMyBinder.startPlay(MusicList.iCurrentMusic, MusicList.iCurrentPosition);
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void refreshDisc(Bundle bundle){
        MusicInfo bean = MusicList.getCurrentMusic();
        mTvTitle.setText(FormatHelper.formatTitle(bean.getTitle(), 25));
        ImageUtil.setBottomBarPic(DisplayActivity.this, mIvCover, bundle.getParcelable(MyService.KEY_ALBUM), R.mipmap.disc);
    }

    private void refreshProgress(int progress){
        if (progress > 0) {
            MusicList.iCurrentPosition = progress; // Remember the current position
            mTvTimeElapsed.setText(FormatHelper.formatDuration(progress));
            mSeekProgress.setProgress(progress / 1000);
        }
    }

    private void refreshDuration(int duration){
        mTvDuration.setText(FormatHelper.formatDuration(duration));
        mSeekProgress.setMax(duration / 1000);
    }

    private void sendMessage(int what, Object object){
        if(mMessageHandler == null){
            return;
        }

        mMessageHandler.obtainMessage(what, object).sendToTarget();
    }

    private static class MessageHandler extends Handler {

        private WeakReference<DisplayActivity> mActivity;

        MessageHandler(DisplayActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final DisplayActivity activity = mActivity.get();
            switch (msg.what){
                case REFRESH_PLAYING_STATE:
                    activity.refreshPlayingStatePassively();
                    break;
                case REFRESH_DISC:
                    Bundle bundle = (Bundle) msg.obj;
                    activity.refreshDisc(bundle);
                    break;
                case REFRESH_PROGRESS:
                    int progress = (int)msg.obj;
                    activity.refreshProgress(progress);
                    break;
                case REFRESH_DURATION:
                    int duration = (int)msg.obj;
                    activity.refreshDuration(duration);
                    break;
                default:
                    break;

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
                sendMessage(REFRESH_PROGRESS, progress);
            } else if (MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action) && !MusicList.isListEmpty()) {
                MusicList.iCurrentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                Bundle bundle = intent.getExtras();
                sendMessage(REFRESH_DISC, bundle);
            } else if (MyService.ACTION_UPDATE_DURATION.equals(action)) {
                //Receive the duration and show under the progress bar
                //Why do this ? because from the ContentResolver, the duration is zero.
                int duration = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                sendMessage(REFRESH_DURATION, duration);
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
