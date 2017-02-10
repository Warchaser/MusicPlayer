package com.warchaser.musicplayer.tools;

import android.content.Intent;

/**
 * Created by Wucn on 2017/1/26.
 * 观察者类
 */

public interface UIObserver
{
    void notifySeekBar2Update(Intent intent);

    void notify2Play();

    void setObserverEnabled(boolean enabled);

    boolean getObserverEnabled();
}
