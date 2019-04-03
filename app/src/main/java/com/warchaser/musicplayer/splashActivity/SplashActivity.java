package com.warchaser.musicplayer.splashActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.globalInfo.BaseActivity;
import com.warchaser.musicplayer.mainActivity.OnAirActivity;
import com.warchaser.musicplayer.tools.CommonUtils;
import com.warchaser.musicplayer.tools.MyService;

import androidx.annotation.NonNull;

/**
 * Created by Administrator on 2014/11/6.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_startup);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int SPLASH_DISPLAY_LENGTH = 3000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (checkUriPermission()) {
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
