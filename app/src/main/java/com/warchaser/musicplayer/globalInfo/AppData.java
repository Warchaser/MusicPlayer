package com.warchaser.musicplayer.globalInfo;

import android.app.Application;

/**
 * Created by Administrator on 2014/12/26.
 *
 */
public class AppData extends Application
{

    private static AppData mThis;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mThis = this;
    }

    public static AppData getApp() {
        return mThis;
    }
}
