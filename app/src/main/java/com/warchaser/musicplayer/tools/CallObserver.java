package com.warchaser.musicplayer.tools;

import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by Wucn on 2017/1/26.
 *
 */

public class CallObserver
{
    private static ArrayList<UIObserver> mObservers = new ArrayList<UIObserver>();

    private CallObserver()
    {

    }

    public static void setObserver(UIObserver observer)
    {
        mObservers.add(observer);
    }

    public synchronized static void callObserver(final Intent intent)
    {
        process(new SubFunction<UIObserver>() {
            @Override
            public void processor(UIObserver observer) {
                observer.notifySeekBar2Update(intent);
            }
        });
    }

    /**
     * 通知UI更新逻辑
     * @param repeatTime 1 重复一次(playOrPause); 2重复两次(next)
     * @return boolean 是否存在UI Observer
     * */
    public synchronized static boolean callPlay(final int repeatTime)
    {
        final ValueBean bean = new ValueBean();

        process(new SubFunction<UIObserver>() {
            @Override
            public void processor(UIObserver observer) {
                observer.notify2Play(repeatTime);
                bean.setFlag(true);
            }
        });

        return bean.isFlag();
    }

    public static void removeAllObservers()
    {
        if(mObservers != null)
        {
            mObservers.clear();
        }
    }

    public static void removeSingleObserver(UIObserver observer)
    {
        if(mObservers != null)
        {
            mObservers.remove(observer);
        }
    }

    public static boolean isNeedCallObserver(){
        return mObservers != null && !mObservers.isEmpty();
    }

    public static void resetUIonStop(){
        process(new SubFunction<UIObserver>() {
            @Override
            public void processor(UIObserver observer) {
                observer.resetUIonStop();
            }
        });
    }

    private static void process(SubFunction<UIObserver> observerSubFunction){
        int size;
        if(mObservers != null && (size = mObservers.size()) > 0) {

            for (int i = 0; i < size; i++) {
                UIObserver observer = mObservers.get(i);
                if (observer != null && observer.getObserverEnabled()) {
                    observerSubFunction.processor(observer);
                }
            }
        }
    }

    private static class ValueBean{
        private boolean isFlag = false;

        public boolean isFlag() {
            return isFlag;
        }

        public void setFlag(boolean flag) {
            isFlag = flag;
        }
    }

    private interface SubFunction<T>{
        void processor(T t);
    }

}
