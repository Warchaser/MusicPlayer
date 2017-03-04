package com.warchaser.musicplayer.displayActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.mainActivity.OnAirActivity;
import com.warchaser.musicplayer.tools.CallObserver;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;
import com.warchaser.musicplayer.tools.MyService;
import com.warchaser.musicplayer.tools.MyService.MyBinder;
import com.warchaser.musicplayer.tools.UIObserver;

import java.util.List;

/**
 * Created by Wu on 2014/10/22.
 */
public class DisplayActivity extends Activity implements OnClickListener {

    private MyBinder myBinder;

    private SeekBar SeekProgress;
    private TextView tvTitle,tvTimeElapsed, tvDuration;
    private ImageView ivMode;
    private Button btnPrevious;
    private Button btnNext;
    private Button btnState;

    private LinearLayout lyBtnMode;
    private LinearLayout lyBtnDisplayPrevious;
    private LinearLayout lyBtnDisplayState;
    private LinearLayout lyBtnDisplayNext;

    private UIUpdateObserver mObserver;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (MyBinder) service;
            updatePlayButton();

            switch (myBinder.getCurrentMode()){
                case 0:
                    ivMode.setBackgroundResource(R.mipmap.mode_loop_for_one);
                    break;
                case 1:
                    ivMode.setBackgroundResource(R.mipmap.mode_loop);
                    break;
                case 2:
                    ivMode.setBackgroundResource(R.mipmap.mode_random);
                    break;
                case 3:
                    ivMode.setBackgroundResource(R.mipmap.mode_sequence);
                    break;
            }
        }
    };

    private void connectToMyService(){
        Intent intent = new Intent(DisplayActivity.this,MyService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.display);
//        getWindow().setFlags(0x08000000, 0x08000000);
        connectToMyService();
        initComponent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myBinder != null){
            unbindService(serviceConnection);
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

        if(myBinder != null)
        {
            myBinder.notifyActivity();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lyBtnDisplayState:
            case R.id.btnDisplayState:
                play();
                break;

            case R.id.lyBtnDisplayNext:
            case R.id.btnDisplayNext:
                myBinder.playNext();
                if(myBinder.getIsPlaying()){
                    btnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                break;

            case R.id.lyBtnDisplayPrevious:
            case R.id.btnDisplayPrevious:
                myBinder.playPrevious();
                if(myBinder.getIsPlaying()){
                    btnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                break;

            case R.id.lyIvMode:
            case R.id.ivMode:
                myBinder.changeMode();
                switch (myBinder.getCurrentMode()){
                    case 0:
                        ivMode.setBackgroundResource(R.mipmap.mode_loop_for_one);
                        break;
                    case 1:
                        ivMode.setBackgroundResource(R.mipmap.mode_loop);
                        break;
                    case 2:
                        ivMode.setBackgroundResource(R.mipmap.mode_random);
                        break;
                    case 3:
                        ivMode.setBackgroundResource(R.mipmap.mode_sequence);
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void initComponent(){
        //Current title
        tvTitle = (TextView) findViewById(R.id.tvDisplayCurrentTitle);
        if(MusicList.musicInfoList.size() != 0)
        {
            tvTitle.setText(FormatHelper.formatTitle(MusicList.musicInfoList.get(MusicList.iCurrentMusic).getTitle(), 25));
        }
        //Current title duration(the right-side TextView)
        tvDuration = (TextView) findViewById(R.id.tvDisplayDuration);
        tvDuration.setText(FormatHelper.formatDuration(MusicList.iCurrentMax));
        //DisplayActivity seekBar
        SeekProgress = (SeekBar) findViewById(R.id.progress);
        SeekProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    myBinder.changeProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SeekProgress.setMax(MusicList.iCurrentMax / 1000);
        SeekProgress.setProgress(MusicList.iCurrentPosition / 1000);

        //Current title elapse(the left-side TextView)
        tvTimeElapsed = (TextView) findViewById(R.id.tvDisplayTimeElapsed);
        tvTimeElapsed.setText(FormatHelper.formatDuration(MusicList.iCurrentPosition));

        ivMode = (ImageView) findViewById(R.id.ivMode);
        ivMode.setOnClickListener(this);

        btnPrevious = (Button) findViewById(R.id.btnDisplayPrevious);
        btnPrevious.setOnClickListener(this);

        btnState = (Button) findViewById(R.id.btnDisplayState);
        btnState.setOnClickListener(this);

        btnNext = (Button) findViewById(R.id.btnDisplayNext);
        btnNext.setOnClickListener(this);

        lyBtnMode = (LinearLayout) findViewById(R.id.lyIvMode);
        lyBtnMode.setOnClickListener(this);

        lyBtnDisplayPrevious = (LinearLayout) findViewById(R.id.lyBtnDisplayPrevious);
        lyBtnDisplayPrevious.setOnClickListener(this);

        lyBtnDisplayState = (LinearLayout) findViewById(R.id.lyBtnDisplayState);
        lyBtnDisplayState.setOnClickListener(this);

        lyBtnDisplayNext = (LinearLayout) findViewById(R.id.lyBtnDisplayNext);
        lyBtnDisplayNext.setOnClickListener(this);

        mObserver = new UIUpdateObserver();
        CallObserver.setObserver(mObserver);
    }

    private void play(){
        if(myBinder.getIsPlaying()){
            myBinder.stopPlay();
            btnState.setBackgroundResource(R.mipmap.run);
        }else{
            myBinder.startPlay(MusicList.iCurrentMusic,MusicList.iCurrentPosition);
            btnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void updatePlayButton()
    {
        if(myBinder != null)
        {
            if(myBinder.getIsPlaying()){
                btnState = (Button) findViewById(R.id.btnDisplayState);
                btnState.setBackgroundResource(R.mipmap.pausedetail);
            }
            else{
                btnState = (Button) findViewById(R.id.btnDisplayState);
                btnState.setBackgroundResource(R.mipmap.run);
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
            if(MyService.ACTION_UPDATE_PROGRESS.equals(action)){
                int progress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS, MusicList.iCurrentPosition);
                if(progress > 0){
                    MusicList.iCurrentPosition = progress; // Remember the current position
                    tvTimeElapsed.setText(FormatHelper.formatDuration(progress));
                    SeekProgress.setProgress(progress / 1000);
                }
            }else if(MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action) && MusicList.musicInfoList.size() != 0){
                //Retrieve the current music and get the title to show on top of the screen.
                MusicList.iCurrentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                tvTitle.setText(FormatHelper.formatTitle(MusicList.musicInfoList.get(MusicList.iCurrentMusic).getTitle(), 25));
            }else if(MyService.ACTION_UPDATE_DURATION.equals(action)){
                //Receive the duration and show under the progress bar
                //Why do this ? because from the ContentResolver, the duration is zero.
                int duration = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                tvDuration.setText(FormatHelper.formatDuration(duration));
                SeekProgress.setMax(duration / 1000);
            }else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
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
