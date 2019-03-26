package com.warchaser.musicplayer.tools;

import android.content.Context;

import java.lang.reflect.Method;

public final class StatusBarUtil {

    private StatusBarUtil(){

    }

    /**
     * 收起通知栏
     * */
    public static void collapseStatusBar(Context context) {
        Object service = context.getSystemService("statusbar");
        if (null == service)
            return;
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
