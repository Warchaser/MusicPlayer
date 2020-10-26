package com.warchaser.musicplayer.global

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import java.lang.Exception
import kotlin.jvm.Throws

/**
 * 修复三星SemEmergencyManager的静态Context内存泄漏
 * */
class SemEmergencyManagerLeakingActivity(application: Application) : Application.ActivityLifecycleCallbacks{

    private val mApplication : Application = application

    companion object{
        fun applyFix(application: Application){
            if("samsung" == Build.MANUFACTURER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N){
                application.registerActivityLifecycleCallbacks(SemEmergencyManagerLeakingActivity(application))
            }
        }
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityStopped(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityDestroyed(activity: Activity?) {
        try {
            swapActivityWithApplicationContext()
        } catch (e : Exception) {
            e.printStackTrace()
        }

        mApplication.unregisterActivityLifecycleCallbacks(this)
    }

    @Throws(ClassNotFoundException::class, NoSuchFieldException::class, IllegalAccessException::class)
    private fun swapActivityWithApplicationContext(){
        val semEmergencyManagerClass = Class.forName("com.samsung.android.emergencymode.SemEmergencyManager")
        val sInstanceFiled = semEmergencyManagerClass.getDeclaredField("sInstance")
        sInstanceFiled.isAccessible = true
        val sInstance = sInstanceFiled.get(null)
        val mContextFiled = semEmergencyManagerClass.getDeclaredField("mContext")
        mContextFiled.isAccessible = true
        mContextFiled.set(sInstance, mApplication)
    }
}