package com.warchaser.musicplayer.tools;

import android.content.Intent;

/**
 * Created by Wucn on 2017/1/26.
 * 观察者类
 */

public interface UIObserver {
    /**
     * 通知UI更新进度条进度
     */
    void notifySeekBar2Update(Intent intent);

    /**
     * 通知UI更新是否播放或者下一首
     */
    void notify2Play(int repeatTime);

    /**
     * 设置是否当前Observer可用
     */
    void setObserverEnabled(boolean enabled);

    /**
     * 获取当前Observer是否可用
     */
    boolean getObserverEnabled();

    /**
     * 在Service停止时，设置UI重置为初始状态
     */
    void resetUIonStop();
}
