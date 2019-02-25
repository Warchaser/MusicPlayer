package com.warchaser.musicplayer.globalInfo;

import android.app.Application;

import com.warchaser.musicplayer.tools.NLog;

/**
 * Created by Administrator on 2014/12/26.
 */
public class AppData extends Application {

    private static AppData mThis;

    @Override
    public void onCreate() {
        super.onCreate();

        mThis = this;

        NLog.initLogFile(this);
    }

    public static AppData getApp() {
        return mThis;
    }
}
