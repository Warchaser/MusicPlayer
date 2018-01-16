package com.warchaser.musicplayer.tools;

import android.widget.Toast;

import com.warchaser.musicplayer.globalInfo.AppData;

public class CommonUtils
{
    private CommonUtils()
    {

    }

    public static void showShortToast(int resId)
    {
        Toast.makeText(AppData.getApp().getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void showShortToast(String message)
    {
        Toast.makeText(AppData.getApp().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public static void showLongToast(int resId)
    {
        Toast.makeText(AppData.getApp().getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(String message)
    {
        Toast.makeText(AppData.getApp().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
