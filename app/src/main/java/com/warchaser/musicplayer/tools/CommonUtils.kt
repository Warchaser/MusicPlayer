package com.warchaser.musicplayer.tools

import android.content.Context
import android.widget.Toast
import com.warchaser.musicplayer.global.AppData.Companion.getApp

object CommonUtils {
    @JvmStatic
    fun showShortToast(resId: Int) {
        context?.run {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun showShortToast(message: String?) {
        context?.run {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun showLongToast(resId: Int) {
        context?.run {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun showLongToast(message: String?) {
        context?.run {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private val context: Context?
        get() = getApp().applicationContext
}