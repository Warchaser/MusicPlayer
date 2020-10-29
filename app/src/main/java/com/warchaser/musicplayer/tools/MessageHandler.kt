package com.warchaser.musicplayer.tools

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.warchaser.musicplayer.tools.PackageUtil.getSimpleClassName
import java.lang.ref.WeakReference

abstract class MessageHandler<T>(t: T) : Handler(Looper.getMainLooper()) {

    private val mWeakReference: WeakReference<T> = WeakReference(t)
    protected val TAG: String

    override final fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        handleMessage(get(), msg)
    }

    open fun handleMessage(t: T?, message: Message?) {}

    fun get(): T? {
        return mWeakReference.get()
    }

    fun destroy() {
        removeCallbacksAndMessages(null)
    }

    init {
        TAG = getSimpleClassName(this)
    }
}