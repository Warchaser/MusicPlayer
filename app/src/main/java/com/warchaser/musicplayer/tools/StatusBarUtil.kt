package com.warchaser.musicplayer.tools

import android.annotation.SuppressLint
import android.content.Context
import java.lang.Exception
import java.lang.reflect.Method

/**
 * Util for StatusBar
 * */
object StatusBarUtil {

    /**
     * 收起通知栏
     * */
    @JvmStatic
    @SuppressLint("WrongConstant")
    fun collapseStatusBar(context: Context){
        //Context.STATUS_BAR_SERVICE = "statusbar"
        //注释为hide，只能反射
        val service : Any = context.getSystemService("statusbar")?:return

        try {
            val clazz : Class<*> = Class.forName("android.app.StatusBarManager")
            val sdkVersion = android.os.Build.VERSION.SDK_INT
            val collapse : Method = if(sdkVersion <= 16){
                clazz.getMethod("collapse")
            } else {
                clazz.getMethod("collapsePanels")
            }

            collapse.isAccessible = true
            collapse.invoke(service)
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

}