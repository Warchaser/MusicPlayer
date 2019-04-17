package com.warchaser.musicplayer.global;

import android.app.Application;

import com.warchaser.musicplayer.tools.CoverLoader;
import com.warchaser.musicplayer.tools.CrashHandler;
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

        CoverLoader.get().init(mThis);
        NLog.initLogFile(this);
        //全局Crash捕获(无法捕获RuntimeError)
        CrashHandler.getInstance().init();
    }

    public static AppData getApp() {
        return mThis;
    }
}
