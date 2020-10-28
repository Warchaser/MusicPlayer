package com.warchaser.musicplayer.global

import android.app.Application
import com.warchaser.musicplayer.tools.CoverLoader
import com.warchaser.musicplayer.tools.CrashHandler
import com.warchaser.musicplayer.tools.NLog

/**
 * Created by Achan on 2020/10/23.
 * Application
 */
class AppData : Application(){

    companion object{
        private lateinit var mApp : AppData

        @JvmStatic
        fun getApp() : AppData{
            return mApp
        }
    }

    override fun onCreate() {
        super.onCreate()
        mApp = this

        SemEmergencyManagerLeakingActivity.applyFix(this)

        CoverLoader.instance.init(this)

        NLog.initLogFile(this)
        //全局Crash捕获(无法捕获RuntimeError)
        CrashHandler.instance.init()
    }

}