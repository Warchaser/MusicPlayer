package com.warchaser.musicplayer.tools;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.mainActivity.OnAirActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by Wu on 2014/10/20.
 *
 */
public class MyService extends Service
{

    private MediaPlayer mMediaPlayer;
    private boolean mIsPlaying = false;

    private static final int updateProgress = 1;
    private static final int updateCurrentMusic = 2;
    private static final int updateDuration = 3;

    public static final String ACTION_UPDATE_PROGRESS = "com.warchaser.MusicPlayer.UPDATE_PROGRESS";
    public static final String ACTION_UPDATE_DURATION = "com.warchaser.MusicPlayer.UPDATE_DURATION";
    public static final String ACTION_UPDATE_CURRENT_MUSIC = "com.warchaser.MusicPlayer.UPDATE_CURRENT_MUSIC";

    private int mCurrentMode = 3; //default sequence playing

    public static final int MODE_ONE_LOOP = 0;
    public static final int MODE_ALL_LOOP = 1;
    public static final int MODE_RANDOM = 2;
    public static final int MODE_SEQUENCE = 3;

    private MyBinder mMyBinder = new MyBinder();
    private MessageHandler mMessageHandler;

    private AudioManager mAudioManager;
    private ComponentName rec;

    private PendingIntent mPendingIntent;

    @Override
    public void onCreate()
    {
        mMessageHandler = new MessageHandler(this);

        initMediaPlayer();

        super.onCreate();
        //将ACTION_MEDIA_BUTTON注册到AudioManager，目前只能这么干(2014.12.24)
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        rec = new ComponentName(getPackageName(),
                MediaButtonReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(rec);

//        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
//        mediaButtonIntent.setComponent(rec);

        Intent intent = new Intent(MyService.this,OnAirActivity.class);
        //clear the top of the stack, this flag can forbid the possibility of the two activities
        //existing at the same time
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(MyService.this, 0, intent, 0);

        //取得电话管理服务
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        if(telephonyManager != null){
            //注册监听对象，对电话的来电状态进行监听
            telephonyManager.listen(new TelListener(), PhoneStateListener.LISTEN_CALL_STATE);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mMediaPlayer != null)
        {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mAudioManager.unregisterMediaButtonEventReceiver(rec);
        }

        if(mMessageHandler != null){
            mMessageHandler.removeCallbacksAndMessages(null);
            mMessageHandler = null;
        }

        stopSelf();
    }

    private static class MessageHandler extends Handler
    {

        private WeakReference<MyService> mServiceWeakReference;

        public MessageHandler(MyService service)
        {
            mServiceWeakReference = new WeakReference<MyService>(service);
        }

        public void handleMessage(Message msg)
        {
            final MyService service = mServiceWeakReference.get();

            switch (msg.what)
            {
                case updateProgress:
                    service.toUpdateProgress();
                    break;
                case updateCurrentMusic:
                    service.toUpdateCurrentMusic();
                    break;
                case updateDuration:
                    service.toUpdateDuration();
                    break;
            }
        }

    };

    private void toUpdateProgress()
    {
        if(mMediaPlayer != null && mIsPlaying)
        {
            int i = mMediaPlayer.getCurrentPosition();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_PROGRESS);
            intent.putExtra(ACTION_UPDATE_PROGRESS,i);

            CallObserver.callObserver(intent);

            //每1秒发送一次广播，进度条每秒更新
            mMessageHandler.sendEmptyMessageDelayed(updateProgress, 1000);
        }
    }

    private void toUpdateCurrentMusic()
    {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_CURRENT_MUSIC);
        intent.putExtra(ACTION_UPDATE_CURRENT_MUSIC, MusicList.iCurrentMusic);

        String notificationTitle = "";

        if(MusicList.musicInfoList.size() == 0)
        {
            notificationTitle = "Mr.Song is not here for now……";
        }
        else
        {
            notificationTitle = MusicList.musicInfoList.get(MusicList.iCurrentMusic).getTitle();
        }

        Notification notification = new Notification.Builder(MyService.this)
                .setTicker("MusicPlayer")
                .setSmallIcon(R.mipmap.notification1)
                .setContentTitle("Playing")
                .setContentText(notificationTitle)
                .setContentIntent(mPendingIntent)
                .getNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(1, notification);

        CallObserver.callObserver(intent);
    }

    private void toUpdateDuration()
    {
        if(mMediaPlayer != null)
        {
            int duration = mMediaPlayer.getDuration();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_DURATION);
            intent.putExtra(ACTION_UPDATE_DURATION,duration);

            CallObserver.callObserver(intent);
        }
    }

    public void initMediaPlayer()
    {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
        {

            @Override
            public void onPrepared(MediaPlayer pMediaPlayer)
            {
                mMediaPlayer.seekTo(MusicList.iCurrentPosition);
                mMediaPlayer.start();
                mMessageHandler.sendEmptyMessage(updateDuration);
            }
        });

        mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
        {

            @Override
            public void onCompletion(MediaPlayer pMediaPlayer)
            {
                if(mIsPlaying)
                {

                    int currentPosition = mMediaPlayer.getCurrentPosition();

                    if(isEmptyInFile(currentPosition))
                    {
                        mMediaPlayer.seekTo(currentPosition + 1000);
                        mMediaPlayer.start();
                        return ;
                    }

                    switch (mCurrentMode)
                    {
                        case MODE_ONE_LOOP:
                            mMediaPlayer.start();
                            break;
                        case MODE_ALL_LOOP:
                            play((MusicList.iCurrentMusic + 1) % MusicList.musicInfoList.size(),0);
                            break;
                        case MODE_RANDOM:
                            play(getRandomPosition(),0);
                            break;
                        case MODE_SEQUENCE:
                            if(MusicList.iCurrentMusic == MusicList.musicInfoList.size() - 1){
                                play(0,0);
                            }
                            else{
                                next();
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener()
        {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i2)
            {

                System.out.println("MediaPlayer has met a problem!" + i);

                return false;
            }
        });

    }

    private boolean isEmptyInFile(int currentPosition)
    {
        return currentPosition < mMediaPlayer.getDuration();

    }

    private void setCurrentMusic(int pCurrentMusic)
    {
        MusicList.iCurrentMusic = pCurrentMusic;
        mMessageHandler.sendEmptyMessage(updateCurrentMusic);
    }

    private int getRandomPosition()
    {
        return (int)(Math.random() * (MusicList.musicInfoList.size() - 1));
    }

    private void play(int CurrentMusic, int CurrentPosition)
    {
        MusicList.iCurrentPosition = CurrentPosition;
        setCurrentMusic(CurrentMusic);

        mMediaPlayer.reset();

        try {

            if(MusicList.musicInfoList.size() != 0)
            {
                mMediaPlayer.setDataSource(MusicList.musicInfoList.get(MusicList.iCurrentMusic).getUrl());
                mMediaPlayer.prepareAsync();
                mMessageHandler.sendEmptyMessage(updateProgress);
                mIsPlaying = true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stop(){
        
        MusicList.iCurrentPosition = mMediaPlayer.getCurrentPosition();

        mMediaPlayer.stop();
        mIsPlaying = false;
    }

    private void next()
    {
        switch(mCurrentMode)
        {
            case MODE_ONE_LOOP:
                if(MusicList.iCurrentMusic == MusicList.musicInfoList.size() - 1)
                {
                    play(0,0);
                }
                else
                {
                    play(MusicList.iCurrentMusic + 1,0);
                }
                break;
            case MODE_ALL_LOOP:
                if(MusicList.iCurrentMusic == MusicList.musicInfoList.size() - 1)
                {
                    play(0,0);
                }
                else
                {
                    play(MusicList.iCurrentMusic + 1,0);
                }
                break;
            case MODE_SEQUENCE:
                if(MusicList.iCurrentMusic == MusicList.musicInfoList.size() - 1)
                {
                    Toast.makeText(this, "最后一首歌曲了，亲，即将播放第一首歌曲～", Toast.LENGTH_SHORT).show();
                    play(0,0);
                }
                else
                {
                    play(MusicList.iCurrentMusic + 1,0);
                }
                break;
            case MODE_RANDOM:
                play(getRandomPosition(),0);
                break;
        }
    }

    private void previous()
    {
        switch(mCurrentMode)
        {
            case MODE_ONE_LOOP:
                if(MusicList.iCurrentMusic == 0)
                {
                    play(MusicList.musicInfoList.size() - 1,0);
                }
                else
                {
                    play(MusicList.iCurrentMusic - 1,0);
                }
                break;
            case MODE_ALL_LOOP:
                if(MusicList.iCurrentMusic == 0)
                {
                    play(MusicList.musicInfoList.size() - 1,0);
                }
                else
                {
                    play(MusicList.iCurrentMusic - 1,0);
                }
                break;
            case MODE_SEQUENCE:
                if(MusicList.iCurrentMusic == 0)
                {
                    Toast.makeText(this, "已经是第一首歌了，亲，即将播放最后一首歌曲～", Toast.LENGTH_SHORT).show();
                    play(MusicList.musicInfoList.size() - 1,0);
                }
                else
                {
                    play(MusicList.iCurrentMusic - 1,0);
                }
                break;
            case MODE_RANDOM:
                play(getRandomPosition(),0);
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        MusicList.mMyBinder = (MyBinder) mMyBinder;
        return mMyBinder;
    }

    private class TelListener extends PhoneStateListener
    {
        public void onCallStateChanged(int state, String incomingNumber)
        {
            super.onCallStateChanged(state, incomingNumber);
            //来电状态
            if(state == TelephonyManager.CALL_STATE_RINGING)
            {
                if(!CallObserver.callPlay(1))
                {
                    if(mMyBinder.getIsPlaying())
                    {
                        mMyBinder.stopPlay();
                    }
                }
            }
            else if(state == TelephonyManager.CALL_STATE_IDLE)
            {
                if(!CallObserver.callPlay(1))
                {
                    if(!mMyBinder.getIsPlaying())
                    {
                        mMyBinder.startPlay(MusicList.iCurrentMusic, MusicList.iCurrentPosition);
                    }
                }
            }
        }
    }

    public class MyBinder extends Binder
    {
        public void startPlay(int CurrentMusic,int CurrentPosition)
        {
            play(CurrentMusic,CurrentPosition);
        }

        public void stopPlay(){
            stop();
        }

        public void playNext(){
            next();
        }

        public void playPrevious(){
            previous();
        }

        /**
         * MODE_ONE_LOOP = 1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         */
        public void changeMode(){
            mCurrentMode = (mCurrentMode + 1) % 4;
        }

        public int getCurrentMode(){
            return mCurrentMode;
        }

        public boolean getIsPlaying(){
            return mIsPlaying;
        }

        /**
         * Notify Activities to update the current music and duration when current activity changes.
         */
        public void notifyActivity()
        {
            toUpdateCurrentMusic();
            toUpdateDuration();
        }

        /**
         * seekBar action
         * */
        public void changeProgress(int pProgress)
        {
            if(mMediaPlayer != null){
                MusicList.iCurrentPosition = pProgress * 1000;
                if(mIsPlaying){
                    mMediaPlayer.seekTo(MusicList.iCurrentPosition);
                }else{
                    play(MusicList.iCurrentMusic,MusicList.iCurrentPosition);
                }
            }
        }

        public void notifyProgress(){
            toUpdateDuration();
        }

        public void rebindObserverOnResume()
        {
            toUpdateCurrentMusic();
            toUpdateDuration();
        }
    }
}
