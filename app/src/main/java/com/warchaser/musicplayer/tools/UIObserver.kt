package com.warchaser.musicplayer.tools

import android.content.Intent

/**
 * Created by Achan on 2020/10/26
 * 观察者类
 * */
interface UIObserver {

    /**
     * 通知UI更新进度条进度
     */
    fun notifySeekBar2Update(intent: Intent?)

    /**
     * 通知UI更新是否播放或者下一首
     */
    fun notify2Play(repeatTime : Int)

    /**
     * 设置是否当前Observer可用
     * 获取当前Observer是否可用
     */
    var observerEnabled : Boolean

    /**
     * 在Service停止时，设置UI重置为初始状态
     */
    fun stopServiceAndExit()
}