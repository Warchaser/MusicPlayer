package com.warchaser.musicplayer.tools;

import android.annotation.SuppressLint;
import android.content.Context;

import java.lang.reflect.Method;

/**
 * Util for StatusBar
 * */
public final class StatusBarUtil {

    private StatusBarUtil(){

    }

    /**
     * 收起通知栏
     * */
    @SuppressLint("WrongConstant")
    public static void collapseStatusBar(Context context) {
        //Context.STATUS_BAR_SERVICE = "statusbar"
        //注释为hide，只能反射
        Object service = context.getSystemService("statusbar");
        if (null == service){
            return;
        }

        try {
            Class<?> clazz = Class.forName("android.app.StatusBarManager");
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            Method collapse = null;
            if (sdkVersion <= 16) {
                collapse = clazz.getMethod("collapse");
            } else {
                collapse = clazz.getMethod("collapsePanels");
            }
            collapse.setAccessible(true);
            collapse.invoke(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
