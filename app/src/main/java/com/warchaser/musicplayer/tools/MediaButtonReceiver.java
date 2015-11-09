package com.warchaser.musicplayer.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.warchaser.musicplayer.mainActivity.OnAirActivity;

/**
 * Created by Wu on 2014/12/15.
 */
public class MediaButtonReceiver extends BroadcastReceiver {

    long buttonDownTime;//
    long buttonUpTime;//

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if(Intent.ACTION_MEDIA_BUTTON.equalsIgnoreCase(intentAction)){
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }
            int keycode = event.getKeyCode();
            int action = event.getAction();

            switch (keycode){

                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if(KeyEvent.ACTION_UP == action){
                        OnAirActivity.play(OnAirActivity.iCurrentMusic);
//                        buttonUpTime = SystemClock.currentThreadTimeMillis();
//                        System.out.println("actionUp: " + buttonUpTime);
                    }

//                    if(KeyEvent.ACTION_DOWN == action){
//                        buttonDownTime = SystemClock.currentThreadTimeMillis();
//                        System.out.println("actionDown: " + buttonDownTime);
//
//                    }
//
//                    if(buttonUpTime - buttonDownTime > 10){
//                        System.out.println(">10: " + "差: "+(buttonUpTime - buttonDownTime) + "," +
//                                "buttonUpTime: " + buttonUpTime + "," + "buttonDownTime: " + buttonUpTime);
//                        MainActivity.play(MainActivity.iCurrentMusic);
//                    }else{
//                        if(buttonUpTime - buttonDownTime < 5)
//                            System.out.println("<5: " + "差: "+(buttonUpTime - buttonDownTime) + "," +
//                                    "buttonUpTime: " + buttonUpTime + "," + "buttonDownTime: " + buttonUpTime);
//                            MainActivity.play(MainActivity.iCurrentMusic + 1);
//                    }

                    break;
            }
        }
    }
}
