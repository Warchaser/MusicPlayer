package com.warchaser.musicplayer.tools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.text.TextUtils
import java.lang.Exception

object PackageUtil {

    @JvmStatic
    fun getSimpleClassName(`object` : Any) : String{
        val clazz = `object`.javaClass
        val str1 : String = clazz.name.replace("$", ".")
        val str2 : String = str1.replace(clazz.`package`.name, "") + "."
        return str2.substring(1)
    }

    @JvmStatic
    fun getAppOpenIntentByPackageName(context: Context, packageName : String) : Intent?{
        //Activity完整名
        var mainActivityName : String? = null
        //
        val pkgMag = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val list : List<ResolveInfo> = pkgMag.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES)
        for(info in list){
            val name = info.activityInfo.packageName
            if(name == packageName){
                mainActivityName = name
                break
            }
        }

        if(TextUtils.isEmpty(mainActivityName)){
            return null
        }

        intent.component = ComponentName(packageName, mainActivityName)

        return intent
    }

    @JvmStatic
    fun getPackageContext(context: Context, packageName : String) : Context?{
        var pkgContext : Context? = null
        if(context.packageName == packageName){
            pkgContext = context
        } else {
            try {
                //创建第三方应用的上下文环境
                pkgContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE)
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }

        return pkgContext
    }

    @JvmStatic
    fun openPackage(context: Context, packageName: String) : Boolean{
        val pkgContext = getPackageContext(context, packageName)
        val intent = getAppOpenIntentByPackageName(context, packageName)
        return if(pkgContext != null && intent != null){
            pkgContext.startActivity(intent)
            true
        } else {
            false
        }
    }

    /**
     * 检查包是否存在
     *
     * @param packageName
     * @return
     */
    @JvmStatic
    fun checkPackInfo(packageName: String, packageManager : PackageManager) : Boolean{
        var packageInfo : PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0)
        } catch (e : PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return packageInfo != null
    }

    @JvmStatic
    fun launchApp(packageName: String, context: Context){
        var packageManager : PackageManager = context.packageManager
        if(checkPackInfo(packageName, packageManager)){
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                context.startActivity(this)
            }
        }
    }

}