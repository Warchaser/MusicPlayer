package com.warchaser.musicplayer.tools

import android.content.Intent

private typealias NotifySeekBar2Update = (intent: Intent?) -> Unit

private typealias Notify2Play = (repeatTime: Int) -> Unit

private typealias StopServiceAndExit = () -> Unit

private typealias ObserverEnabled = (enable : Boolean) -> Boolean

class UIObserverBuilder : UIObserver {

    private var mNotifySeekBar2Update : NotifySeekBar2Update? = null
    private var mNotify2Play : Notify2Play? = null
    private var mStopServiceAndExit : StopServiceAndExit? = null
    private var mObserverEnable : ObserverEnabled? = null

    override fun notifySeekBar2Update(intent: Intent?) {
        mNotifySeekBar2Update?.invoke(intent) ?: Unit
    }

    override fun notify2Play(repeatTime: Int) {
        mNotify2Play?.invoke(repeatTime) ?: Unit
    }

    override var observerEnabled: Boolean = false
        get() = mObserverEnable?.invoke(field) ?: false
        set(value) {
            mObserverEnable?.invoke(value)
        }

    override fun stopServiceAndExit() {
        mStopServiceAndExit?.invoke() ?: Unit
    }

    fun notifySeekBar2Update(callback : NotifySeekBar2Update){
        mNotifySeekBar2Update = callback
    }

    fun notify2Play(callback: Notify2Play){
        mNotify2Play = callback
    }

    fun stopServiceAndExit(callback : StopServiceAndExit){
        mStopServiceAndExit = callback
    }

    fun setEnable(callback : ObserverEnabled){
        mObserverEnable = callback
    }

}

fun registerUIObserver(function : UIObserverBuilder.() -> Unit) = UIObserverBuilder().also(function)