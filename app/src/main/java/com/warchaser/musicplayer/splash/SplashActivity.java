package com.warchaser.musicplayer.splash;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.global.BaseActivity;
import com.warchaser.musicplayer.main.OnAirActivity;
import com.warchaser.musicplayer.tools.CommonUtils;
import com.warchaser.musicplayer.tools.MyService;

import androidx.annotation.NonNull;

/**
 * Created by Wucn on 2014/11/6.
 * SplashActivity
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        //可以达到覆盖启动页面的目的，
        // 如先快速启动layer-list提供的启动页面,
        // 然后经逻辑处理的一段时间之后(如网络请求CM页面数据)，
        // 再由此code覆盖启动页面
        //setContentView(R.layout.activity_splash_with_cm);

        final int SPLASH_DISPLAY_LENGTH = 2_000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (checkUriPermission()) {
//                    setContentView(R.layout.activity_splash_with_cm);
                    startOnAirActivity();
                } else {

                }
            }
        }, SPLASH_DISPLAY_LENGTH);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CODE_FOR_WRITE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 用户成功授予权限
                    startOnAirActivity();
                } else {
                    CommonUtils.showShortToast(R.string.uri_permission_not_granted);
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void startOnAirActivity() {
        Intent startService = new Intent(this, MyService.class);
        startService(startService);

        Uri uri = getIntent().getData();
        if (uri != null) {
            Intent startActivity = new Intent(this, OnAirActivity.class);
            startActivity.setData(uri);
            startActivity.putExtra("isFromExternal", true);
            startActivity(startActivity);
            finish();
        } else {
            startCertainActivity(OnAirActivity.class);
            finish();
        }
    }
}
