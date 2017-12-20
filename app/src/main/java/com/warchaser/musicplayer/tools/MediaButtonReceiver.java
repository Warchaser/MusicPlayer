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

            switch (keycode){

                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if(KeyEvent.ACTION_UP == action)
                    {
                        if(!CallObserver.callPlay())
                        {
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
                    break;
                default:
                    break;
            }
        }
    }
}
