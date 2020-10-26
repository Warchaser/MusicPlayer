package com.warchaser.musicplayer.global

import android.app.Activity
import java.lang.Exception
import java.util.*
import kotlin.system.exitProcess

/**
 * 堆栈式管理Activity
 */
class AppManager private constructor(){

    companion object{
        val mInstance = Holder.mHolder
    }

    private object Holder{
        val mHolder = AppManager()
    }

    private var mActivityStack : Stack<Activity>? = null

    /**
     * 添加Activity到堆栈
     */
    fun addActivity(activity: Activity){
        if(mActivityStack == null){
            mActivityStack = Stack()
        }

        mActivityStack?.add(activity)
    }

    /**
     * 移除Activity
     */
    fun removeActivity(activity: Activity){
        mActivityStack?.run {
            if(isNotEmpty()){
                val iterator = iterator()
                while (iterator.hasNext()){
                    val tmps = iterator.next()
                    if(tmps == activity){
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    /**
     * 获取指定的Activity
     */
    fun getActivity(cls : Class<*>) : Activity?{
        mActivityStack?.run {
            for(activity in this){
                if(activity.javaClass == cls){
                    return activity
                }
            }
        }

        return null
    }

    /**
     * 获取当前显示Activity（堆栈中最后一个传入的activity）
     */
    fun getLastActivity() : Activity?{
        mActivityStack?.run {
            if(isNotEmpty()){
                return lastElement()
            }
        }

        return null
    }

    /**
     * 将Stack中的Activity强转为Class<?>的实例
     * @param cls 继承自Activity的Class
     * @return 能转换的情况下，转换为Class<?>
     * */
    fun <T : Activity> getLastActivity(cls : Class<T>) : Activity?{
        val activity = getLastActivity()
        if(cls.isInstance(activity)){
            return cls.cast(activity)
        }

        return null
    }

    /**
     * 获取所有Activity
     */
    fun getAllActivityStack() : Stack<Activity>?{
        return mActivityStack
    }

    /**
     * 结束指定的Activity
     */
    fun finishActivity(activity: Activity?){
        activity?.run {
            if(!isFinishing){
                finish()
                mActivityStack?.remove(activity)
            }
        }
    }

    /**
     * 结束指定类名的Activity
     */
    fun finishActivity(cls : Class<*>){
        mActivityStack?.run {
            if(isNotEmpty()){
                val iterator = iterator()
                while (iterator.hasNext()){
                    val tmps = iterator.next()
                    if(cls == tmps.javaClass){
                        iterator.remove()
                        tmps.finish()
                    }
                }
            }
        }
    }

    /**
     * 结束除当前传入以外所有Activity
     */
    fun finishOthersActivity(cls: Class<*>){
        mActivityStack?.run {
            if(isNotEmpty()){
                val iterator = iterator()
                while (iterator.hasNext()){
                    val tmps = iterator.next()
                    if(cls != tmps.javaClass){
                        iterator.remove()
                        tmps.finish()
                    }
                }
            }
        }
    }

    /**
     * 结束所有Activity
     */
    fun finishAllActivity(){
        mActivityStack?.run {
            if(isNotEmpty()){
                val iterator = iterator()
                while (iterator.hasNext()){
                    val tmps = iterator.next()
                    iterator.remove()
                    tmps.finish()
                }
            }
        }
    }

    /**
     * 退出应用程序
     */
    fun appExit(){
        try {
            finishAllActivity()
            // 杀死该应用进程
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

}