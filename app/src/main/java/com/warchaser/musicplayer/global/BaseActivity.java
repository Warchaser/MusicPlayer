package com.warchaser.musicplayer.global;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.warchaser.musicplayer.tools.PackageUtil;

/**
 * Created by Warchaser on 2017/8/5.
 * 基类，
 */
public class BaseActivity extends AppCompatActivity {

    protected final int CODE_FOR_WRITE_PERMISSION = 1;
    protected String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        checkUriPermission();
        TAG = PackageUtil.getSimpleClassName(this);
        AppManager.getInstance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().removeActivity(this);
        clearMember();
    }

    protected boolean checkUriPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                BaseActivity activity = this;
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        CODE_FOR_WRITE_PERMISSION);
                return false;

            }

            return true;
        }

        return true;
    }

    protected void clearMember() {

    }

    protected void startCertainActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}
