package com.warchaser.musicplayer.global

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.warchaser.musicplayer.tools.PackageUtil

/**
 * Created by Achan on 2020/10/23.
 * 基类，
 */
open class BaseActivity : AppCompatActivity(){

    protected val CODE_FOR_WRITE_PERMISSION : Int = 1

    protected lateinit var TAG : String

    override fun onCreate(savedInstanceState: Bundle?) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val modes = window.windowManager.defaultDisplay.supportedModes
            modes.sortBy {
                it.refreshRate
            }
            window.let {
                val lp = it.attributes
                lp.preferredDisplayModeId = modes.last().modeId
                it.attributes = lp
            }
        }

        super.onCreate(savedInstanceState)
        TAG = PackageUtil.getSimpleClassName(this)
        AppManager.mInstance.addActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppManager.mInstance.removeActivity(this)
        clearMember()
    }

    protected fun checkUriPermission() : Boolean{
        if(android.os.Build.VERSION.SDK_INT >= 23){
            val hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED){
                val activity = this
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), CODE_FOR_WRITE_PERMISSION)
                return false
            }

            return true
        }

        return true
    }

    protected open fun clearMember(){

    }

    protected fun startCertainActivity(cls : Class<*>){
        startActivity(Intent(this, cls))
    }

}