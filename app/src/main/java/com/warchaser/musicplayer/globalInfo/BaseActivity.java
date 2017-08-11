package com.warchaser.musicplayer.globalInfo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.warchaser.musicplayer.R;


/**
 * Created by Warchaser on 2017/8/5.
 * 基类，
 */

public class BaseActivity extends AppCompatActivity{

    protected final int CODE_FOR_WRITE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        checkUriPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMember();
    }

    protected boolean checkUriPermission(){
        if(android.os.Build.VERSION.SDK_INT >= 23){
            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                BaseActivity activity = this;
                ActivityCompat.requestPermissions(activity,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        CODE_FOR_WRITE_PERMISSION);
                return false;

            }

            return true;
        }

        return true;
    }

    protected void clearMember(){

    }

    protected void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected String getRestStrings(int resId){
        return getResources().getString(resId);
    }

    protected void startCertainActivity(Class<?> cls){
        startActivity(new Intent(this, cls));
    }
}
