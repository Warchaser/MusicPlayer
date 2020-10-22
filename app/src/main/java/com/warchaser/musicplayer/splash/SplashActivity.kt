package com.warchaser.musicplayer.splash

import com.warchaser.musicplayer.global.BaseActivity
import android.os.Bundle
import android.view.WindowManager
import android.content.pm.PackageManager
import com.warchaser.musicplayer.tools.CommonUtils
import com.warchaser.musicplayer.R
import android.content.Intent
import android.os.Handler
import android.view.Window
import com.warchaser.musicplayer.tools.MediaControllerService
import com.warchaser.musicplayer.main.OnAirActivity

/**
 * Created by Wucn on 2014/11/6.
 * SplashActivity
 */
class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        //可以达到覆盖启动页面的目的，
        // 如先快速启动layer-list提供的启动页面,
        // 然后经逻辑处理的一段时间之后(如网络请求CM页面数据)，
        // 再由此code覆盖启动页面
        //setContentView(R.layout.activity_splash_with_cm);
        val SPLASH_DISPLAY_LENGTH = 2000
        Handler().postDelayed({
            if (checkUriPermission()) {
//                    setContentView(R.layout.activity_splash_with_cm);
                startOnAirActivity()
            } else {
            }
        }, SPLASH_DISPLAY_LENGTH.toLong())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CODE_FOR_WRITE_PERMISSION -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户成功授予权限
                startOnAirActivity()
            } else {
                CommonUtils.showShortToast(R.string.uri_permission_not_granted)
                finish()
            }
            else -> {
            }
        }
    }

    private fun startOnAirActivity() {
        val startService = Intent(this, MediaControllerService::class.java)
        startService(startService)
        val uri = intent.data
        if (uri != null) {
            val startActivity = Intent(this, OnAirActivity::class.java)
            startActivity.data = uri
            startActivity.putExtra("isFromExternal", true)
            startActivity(startActivity)
        } else {
            startCertainActivity(OnAirActivity::class.java)
        }
        finish()
    }
}