package com.warchaser.musicplayer.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * Created by Wu on 2014/12/15.
 *
 */
public class MediaButtonReceiver extends BroadcastReceiver
{

    private static int mClickCounter = 0;
    private static final int DOUBLE_CLICK_DURATION = 500;
    private static long mLastClickTime = 0;

    private final int SINGLE_CLICK = 1;
    private final int DOUBLE_CLICK = 2;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String intentAction = intent.getAction();
        if(Intent.ACTION_MEDIA_BUTTON.equalsIgnoreCase(intentAction))
        {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
            {
                return;
            }
            int keycode = event.getKeyCode();
            int action = event.getAction();
            final long eventTime = event.getEventTime();

            switch (keycode){

                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

                    if (action == KeyEvent.ACTION_UP) {


                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {

                            System.out.println(eventTime - mLastClickTime);
                            System.out.println("mLastClickTime: " + mLastClickTime);
                            System.out.println("eventTime: " + eventTime);

                            if (eventTime - mLastClickTime >= DOUBLE_CLICK_DURATION) {
                                mClickCounter = 0;
                            }

                            mClickCounter++;
                            if (mClickCounter >= 3) {
                                mClickCounter = 0;
                            }
                            mLastClickTime = eventTime;

                            if(mClickCounter == 2 && !CallObserver.callPlay(DOUBLE_CLICK))
                            {
                                if(MusicList.mMyBinder != null)
                                {
                                    MusicList.mMyBinder.playNext();
                                }
                            } else if(mClickCounter == 1 && !CallObserver.callPlay(SINGLE_CLICK)){
                                if(MusicList.mMyBinder != null)
                                {
                                    if(MusicList.mMyBinder.getIsPlaying())
                                    {
                                        MusicList.mMyBinder.stopPlay();
                                    }
                                    else
                                    {
                                        MusicList.mMyBinder.startPlay(MusicList.iCurrentMusic, MusicList.iCurrentPosition);
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
