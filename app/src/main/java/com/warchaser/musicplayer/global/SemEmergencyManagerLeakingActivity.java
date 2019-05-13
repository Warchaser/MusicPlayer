package com.warchaser.musicplayer.global;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import java.lang.reflect.Field;

/**
 * 修复三星SemEmergencyManager的静态Context内存泄漏
 * */
public class SemEmergencyManagerLeakingActivity implements Application.ActivityLifecycleCallbacks {

    private Application mApplication;

    private SemEmergencyManagerLeakingActivity(Application application){
        this.mApplication = application;
    }

    public static void applyFix(Application application){
        if("samsung".equals(Build.MANUFACTURER) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N){
            application.registerActivityLifecycleCallbacks(new SemEmergencyManagerLeakingActivity(application));
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        try {
            swapActivityWithApplicationContext();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mApplication.unregisterActivityLifecycleCallbacks(this);
    }

    private void swapActivityWithApplicationContext() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException{
        Class<?> semEmergencyManagerClass = Class.forName("com.samsung.android.emergencymode.SemEmergencyManager");
        Field sInstanceFiled = semEmergencyManagerClass.getDeclaredField("sInstance");
        sInstanceFiled.setAccessible(true);
        Object sInstance = sInstanceFiled.get(null);
        Field mContextField = semEmergencyManagerClass.getDeclaredField("mContext");
        mContextField.setAccessible(true);
        mContextField.set(sInstance, mApplication);
    }
}
