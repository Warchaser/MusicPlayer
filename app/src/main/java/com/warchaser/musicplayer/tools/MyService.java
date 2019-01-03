package com.warchaser.musicplayer.tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.RemoteViews;

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

    public static final String PAUSE_OR_PLAY_ACTION = "com.warchaser.MusicPlayer.playOrPause";
    public static final String NEXT_ACTION = "com.warchaser.MusicPlayer.next";
    public static final String CLOSE_ACTION = "com.warchaser.MusicPlayer.close";

    private final int PAUSE_FLAG = 0x11;
    private final int NEXT_FLAG = 0x12;
    private final int CLOSE_FLAG = 0x13;

    private final int NOTIFICATION_ID = 1001;

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

    private RemoteViews mNotificationRemoteView;
    private Notification mNotification;
    private NotificationManager mNotificationManager;

    private IntentReceiver mIntentReceiver;

    @Override
    public void onCreate()
    {
        MusicList.instance(getContentResolver());

        mMessageHandler = new MessageHandler(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initMediaPlayer();

        super.onCreate();
        //将ACTION_MEDIA_BUTTON注册到AudioManager，目前只能这么干(2014.12.24)
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        rec = new ComponentName(getPackageName(),
                MediaButtonReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(rec);

        //取得电话管理服务
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        if(telephonyManager != null)
        {
            //注册监听对象，对电话的来电状态进行监听
            telephonyManager.listen(new TelListener(), PhoneStateListener.LISTEN_CALL_STATE);
        }

        initializeReceiver();

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

        unregisterReceiver(mIntentReceiver);

        stopSelf();
    }

    private void initializeReceiver(){
        mIntentReceiver = new IntentReceiver();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PAUSE_OR_PLAY_ACTION);
        intentFilter.addAction(NEXT_ACTION);
        intentFilter.addAction(CLOSE_ACTION);

        registerReceiver(mIntentReceiver, intentFilter);
    }

    private static class MessageHandler extends Handler
    {

        private WeakReference<MyService> mServiceWeakReference;

        MessageHandler(MyService service)
        {
            mServiceWeakReference = new WeakReference<>(service);
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

    }

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

//        String notificationTitle;
//
//        if(MusicList.musicInfoList.size() == 0)
//        {
//            notificationTitle = "Mr.Song is not here for now……";
//        }
//        else
//        {
//            notificationTitle = MusicList.musicInfoList.get(MusicList.iCurrentMusic).getTitle();
//        }
//
//        Notification notification = new Notification.Builder(MyService.this)
//                .setTicker("MusicPlayer")
//                .setSmallIcon(R.mipmap.notification1)
//                .setContentTitle("Playing")
//                .setContentText(notificationTitle)
//                .setContentIntent(mPendingIntent)
//                .getNotification();
//        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(NOTIFICATION_ID, getNotification());

        CallObserver.callObserver(intent);
    }

    private Notification getNotification(){
        final String notificationTitle;
        if(mNotificationRemoteView == null){
            mNotificationRemoteView = new RemoteViews(this.getPackageName(), R.layout.notification);
        }

        MusicInfo bean;

        if(MusicList.musicInfoList.isEmpty())
        {
            notificationTitle = "Mr.Song is not here for now……";
            mNotificationRemoteView.setImageViewResource(R.id.fileImage, R.mipmap.disc);
        }
        else
        {
            bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
            notificationTitle = bean.getTitle();
            String uriString = bean.getUriWithCoverPic();
            if(TextUtils.isEmpty(uriString))
            {
                mNotificationRemoteView.setImageViewResource(R.id.fileImage, R.mipmap.disc);
            }
            else
            {
                Drawable drawable = ImageUtil.getCoverDrawableFromMusicFile(uriString, this, 70);
                if(drawable == null)
                {
                    mNotificationRemoteView.setImageViewResource(R.id.fileImage, R.mipmap.disc);
                }
                else
                {
                    mNotificationRemoteView.setImageViewUri(R.id.fileImage, Uri.parse(uriString));
                }
            }
        }

        mNotificationRemoteView.setTextViewText(R.id.fileName, notificationTitle);

        Intent pauseIntent = new Intent(PAUSE_OR_PLAY_ACTION);
        pauseIntent.putExtra("FLAG", PAUSE_FLAG);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, 0);
        mNotificationRemoteView.setImageViewResource(R.id.ivPauseOrPlay, mMyBinder.getIsPlaying() ? R.mipmap.pausedetail : R.mipmap.run);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.ivPauseOrPlay, pausePendingIntent);

        Intent nextIntent = new Intent(NEXT_ACTION);
        nextIntent.putExtra("FLAG", NEXT_FLAG);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.ivNext, nextPendingIntent);

        Intent closeIntent = new Intent(CLOSE_ACTION);
        closeIntent.putExtra("FLAG", CLOSE_FLAG);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, closeIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.ivClose, closePendingIntent);

        Intent activityIntent = new Intent(MyService.this,OnAirActivity.class);
        //clear the top of the stack, this flag can forbid the possibility of the two activities
        //existing at the same time
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(MyService.this, 0, activityIntent, 0);
        mNotificationRemoteView.setOnClickPendingIntent(R.id.lyRoot, activityPendingIntent);

        if(mNotification == null)
        {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.warchaser.MusicPlayer.notification")
                    .setContent(mNotificationRemoteView)
                    .setSmallIcon(R.mipmap.notification1);

            mNotification = builder.build();
            mNotification.flags |= Notification.FLAG_NO_CLEAR;
        }

        return mNotification;
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
                            if(MusicList.iCurrentMusic == MusicList.musicInfoList.size() - 1)
                            {
                                play(0,0);
                            }
                            else
                            {
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

        try
        {

            if(!MusicList.musicInfoList.isEmpty())
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

    private void stop()
    {
        
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
                    CommonUtils.showShortToast(R.string.last_song_tip);
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
                    CommonUtils.showShortToast(R.string.first_song_tip);
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
        MusicList.mMyBinder = mMyBinder;
        return mMyBinder;
    }

    public void startPlayNormal(){
        play(MusicList.iCurrentMusic, MusicList.iCurrentPosition);
    }

    /**
     * 设置通知栏播放状态按钮图标
     * */
    private void setRemoteViewPlayOrPause(){
        if(mNotificationRemoteView != null){
            mNotificationRemoteView.setImageViewResource(R.id.ivPauseOrPlay, !mMyBinder.getIsPlaying() ? R.mipmap.pausedetail : R.mipmap.run);
        }

        if(mNotificationManager != null){
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    private void setRemoteViewPlayOrPausePassive(){
        if(mNotificationRemoteView != null){
            mNotificationRemoteView.setImageViewResource(R.id.ivPauseOrPlay, mMyBinder.getIsPlaying() ? R.mipmap.pausedetail : R.mipmap.run);
        }

        if(mNotificationManager != null){
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    /**
     * 处理通知栏主动控制的消息
     * */
    private synchronized void handleIntentCommand(Intent intent){

        if(intent == null){
            return;
        }

        final String action = intent.getAction();
        if(NEXT_ACTION.equals(action)){
            setRemoteViewPlayOrPause();
            if(!CallObserver.callPlay(2)){
                next();
            }
        } else if(PAUSE_OR_PLAY_ACTION.equals(action)){
            setRemoteViewPlayOrPause();
            if(!CallObserver.callPlay(1)){
                if(mMyBinder.getIsPlaying()){
                    stop();
                } else {
                    startPlayNormal();
                }
            }

        } else if(CLOSE_ACTION.equals(action)){

        }

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
                        startPlayNormal();
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

        public void updateNotification(){
            setRemoteViewPlayOrPausePassive();
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

        public synchronized boolean getIsPlaying(){
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
            if(mMediaPlayer != null)
            {
                MusicList.iCurrentPosition = pProgress * 1000;
                if(mIsPlaying)
                {
                    mMediaPlayer.seekTo(MusicList.iCurrentPosition);
                }
                else
                {
                    startPlayNormal();
                }
            }
        }

        public void notifyProgress()
        {
            toUpdateDuration();
        }
    }

    private class IntentReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntentCommand(intent);
        }
    }
}
