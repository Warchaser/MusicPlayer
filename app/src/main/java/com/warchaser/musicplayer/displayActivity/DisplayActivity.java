package com.warchaser.musicplayer.displayActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.globalInfo.BaseActivity;
import com.warchaser.musicplayer.mainActivity.OnAirActivity;
import com.warchaser.musicplayer.tools.CallObserver;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.ImageUtil;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;
import com.warchaser.musicplayer.tools.MyService;
import com.warchaser.musicplayer.tools.MyService.MyBinder;
import com.warchaser.musicplayer.tools.UIObserver;


/**
 * Created by Wu on 2014/10/22.
 *
 */
public class DisplayActivity extends BaseActivity implements OnClickListener
{

    private MyBinder mMyBinder;

    private SeekBar mSeekProgress;
    private TextView mTvTitle, mTvTimeElapsed, mTvDuration;
    private ImageView mIvMode;
    private ImageView mIvCover;
    private Button mBtnPrevious;
    private Button mBtnNext;
    private Button mBtnState;

    private LinearLayout mLyBtnMode;
    private LinearLayout mLyBtnDisplayPrevious;
    private LinearLayout mLyBtnDisplayState;
    private LinearLayout mLyBtnDisplayNext;

    private UIUpdateObserver mObserver;

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mMyBinder = (MyBinder) service;
            updatePlayButton();

            switch (mMyBinder.getCurrentMode())
            {
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

    private void connectToMyService()
    {
        Intent intent = new Intent(DisplayActivity.this,MyService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.display);
        connectToMyService();
        initComponent();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(mMyBinder != null)
        {
            unbindService(mServiceConnection);
        }

        CallObserver.removeSingleObserver(mObserver);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(mObserver != null)
        {
            mObserver.setObserverEnabled(false);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        updatePlayButton();

        if(mObserver != null)
        {
            mObserver.setObserverEnabled(true);
        }

        if(mMyBinder != null)
        {
            mMyBinder.notifyActivity();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.lyBtnDisplayState:
            case R.id.btnDisplayState:
                play();
                break;

            case R.id.lyBtnDisplayNext:
            case R.id.btnDisplayNext:
                mMyBinder.playNext();
                if(mMyBinder.getIsPlaying())
                {
                    mBtnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                break;

            case R.id.lyBtnDisplayPrevious:
            case R.id.btnDisplayPrevious:
                mMyBinder.playPrevious();
                if(mMyBinder.getIsPlaying())
                {
                    mBtnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                break;

            case R.id.lyIvMode:
            case R.id.ivMode:
                mMyBinder.changeMode();
                switch (mMyBinder.getCurrentMode())
                {
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

    private void initComponent(){
        //Current title
        mTvTitle = (TextView) findViewById(R.id.tvDisplayCurrentTitle);
        if(MusicList.musicInfoList.size() != 0)
        {
            mTvTitle.setText(FormatHelper.formatTitle(MusicList.musicInfoList.get(MusicList.iCurrentMusic).getTitle(), 25));
        }
        //Current title duration(the right-side TextView)
        mTvDuration = (TextView) findViewById(R.id.tvDisplayDuration);
        mTvDuration.setText(FormatHelper.formatDuration(MusicList.iCurrentMax));
        //DisplayActivity seekBar
        mSeekProgress = (SeekBar) findViewById(R.id.progress);
        mSeekProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                if(b){
                    mMyBinder.changeProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });
        mSeekProgress.setMax(MusicList.iCurrentMax / 1000);
        mSeekProgress.setProgress(MusicList.iCurrentPosition / 1000);

        //Current title elapse(the left-side TextView)
        mTvTimeElapsed = (TextView) findViewById(R.id.tvDisplayTimeElapsed);
        mTvTimeElapsed.setText(FormatHelper.formatDuration(MusicList.iCurrentPosition));

        mIvMode = (ImageView) findViewById(R.id.ivMode);
        mIvMode.setOnClickListener(this);

        mBtnPrevious = (Button) findViewById(R.id.btnDisplayPrevious);
        mBtnPrevious.setOnClickListener(this);

        mBtnState = (Button) findViewById(R.id.btnDisplayState);
        mBtnState.setOnClickListener(this);

        mBtnNext = (Button) findViewById(R.id.btnDisplayNext);
        mBtnNext.setOnClickListener(this);

        mLyBtnMode = (LinearLayout) findViewById(R.id.lyIvMode);
        mLyBtnMode.setOnClickListener(this);

        mLyBtnDisplayPrevious = (LinearLayout) findViewById(R.id.lyBtnDisplayPrevious);
        mLyBtnDisplayPrevious.setOnClickListener(this);

        mLyBtnDisplayState = (LinearLayout) findViewById(R.id.lyBtnDisplayState);
        mLyBtnDisplayState.setOnClickListener(this);

        mLyBtnDisplayNext = (LinearLayout) findViewById(R.id.lyBtnDisplayNext);
        mLyBtnDisplayNext.setOnClickListener(this);

        mObserver = new UIUpdateObserver();
        CallObserver.setObserver(mObserver);

        mIvCover = (ImageView) findViewById(R.id.iv_cover);

        Intent intent = getIntent();
        if (intent != null){
            String uri = intent.getStringExtra("uri");
            ImageUtil.setBottomBarDisc(this, uri, R.dimen.bottom_bar_disc_width_and_height, mIvCover, R.mipmap.disc, false);
        }

    }

    private void play(){
        if(mMyBinder.getIsPlaying())
        {
            mMyBinder.stopPlay();
            mBtnState.setBackgroundResource(R.mipmap.run);
        }
        else
        {
            mMyBinder.startPlay(MusicList.iCurrentMusic,MusicList.iCurrentPosition);
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void updatePlayButton()
    {
        if(mMyBinder != null)
        {
            if(mMyBinder.getIsPlaying()){
                mBtnState = (Button) findViewById(R.id.btnDisplayState);
                mBtnState.setBackgroundResource(R.mipmap.pausedetail);
            }
            else
            {
                mBtnState = (Button) findViewById(R.id.btnDisplayState);
                mBtnState.setBackgroundResource(R.mipmap.run);
            }
        }
    }

    private class UIUpdateObserver implements UIObserver
    {
        private boolean mIsEnabled;
        @Override
        public void notifySeekBar2Update(Intent intent)
        {
            String action = intent.getAction();
            if(MyService.ACTION_UPDATE_PROGRESS.equals(action))
            {
                int progress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS, MusicList.iCurrentPosition);
                if(progress > 0)
                {
                    MusicList.iCurrentPosition = progress; // Remember the current position
                    mTvTimeElapsed.setText(FormatHelper.formatDuration(progress));
                    mSeekProgress.setProgress(progress / 1000);
                }
            }
            else if(MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action) && MusicList.musicInfoList.size() != 0)
            {
                //Retrieve the current music and get the title to show on top of the screen.
                MusicList.iCurrentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                mTvTitle.setText(FormatHelper.formatTitle(MusicList.musicInfoList.get(MusicList.iCurrentMusic).getTitle(), 25));
                MusicInfo bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
                ImageUtil.setBottomBarDisc(DisplayActivity.this, bean.getUriWithCoverPic(), R.dimen.bottom_bar_disc_width_and_height, mIvCover, R.mipmap.disc, false);

            }
            else if(MyService.ACTION_UPDATE_DURATION.equals(action))
            {
                //Receive the duration and show under the progress bar
                //Why do this ? because from the ContentResolver, the duration is zero.
                int duration = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                mTvDuration.setText(FormatHelper.formatDuration(duration));
                mSeekProgress.setMax(duration / 1000);
            }
            else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action))
            {
                play();
            }
        }

        @Override
        public void notify2Play() {
            play();
        }

        @Override
        public void setObserverEnabled(boolean enabled)
        {
            this.mIsEnabled = enabled;
        }

        @Override
        public boolean getObserverEnabled()
        {
            return mIsEnabled;
        }
    }
}
