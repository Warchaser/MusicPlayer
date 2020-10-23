package com.warchaser.musicplayer.tools;

import android.content.Context;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.warchaser.musicplayer.global.AppData;

public class CommonUtils {
    private CommonUtils() {

    }

    public static void showShortToast(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void showShortToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    public static void showLongToast(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    public static int getColor(int resId){
        return ContextCompat.getColor(getContext(), resId);
    }

    private static Context getContext(){
        return AppData.getApp().getApplicationContext();
    }
}
