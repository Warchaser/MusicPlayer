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

    public static void callObserver(Intent intent)
    {
        int size;
        if(mObservers != null && (size = mObservers.size()) > 0)
        {

            for(int i = 0; i < size; i++)
            {
                UIObserver observer = mObservers.get(i);
                if(observer != null && observer.getObserverEnabled())
                {
                    observer.notifySeekBar2Update(intent);
                }
            }
        }
    }

    public static boolean callPlay()
    {
        int size;
        boolean isOneUIEnabled = false;
        if(mObservers != null && (size = mObservers.size()) > 0)
        {
            for(int i = 0; i < size; i++)
            {
                UIObserver observer = mObservers.get(i);
                if(observer != null && observer.getObserverEnabled())
                {
                    observer.notify2Play();
                    isOneUIEnabled = true;
                }
            }
        }

        return isOneUIEnabled;
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
            if(mObservers.contains(observer))
            {
                mObservers.remove(observer);
            }
        }
    }

}
