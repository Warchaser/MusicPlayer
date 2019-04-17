package com.warchaser.musicplayer.global;

import android.app.Activity;

import java.util.Stack;

/**
 * 堆栈式管理Activity
 */
public class AppManager {

    private Stack<Activity> mActivityStack;

    private AppManager() {

    }

    /**
     * 单一实例
     */
    public static AppManager getInstance() {
        return SingleApp.INSTANCE;
    }

    public static class SingleApp {
        public static AppManager INSTANCE = new AppManager();
    }

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(Activity activity) {
        if (mActivityStack == null) {
            mActivityStack = new Stack<Activity>();
        }
        mActivityStack.add(activity);
    }

    /**
     * 移除Activity
     */
    public void removeActivity(Activity activity) {
        if (mActivityStack != null && !mActivityStack.isEmpty()) {
            mActivityStack.remove(activity);
        }
    }

    /**
     * 获取指定的Activity
     */
    public Activity getActivity(Class<?> cls) {
        if (mActivityStack != null) {
            for (Activity activity : mActivityStack) {
                if (activity.getClass().equals(cls)) {
                    return activity;
                }
            }
        }
        return null;
    }

    /**
     * 获取当前显示Activity（堆栈中最后一个传入的activity）
     */
    public Activity getLastActivity() {
        if (mActivityStack != null && !mActivityStack.isEmpty()) {
            return mActivityStack.lastElement();
        }
        return null;
    }

    /**
     * 将Stack中的Activity强转为Class<?>的实例
     * @param cls 继承自Activity的Class
     * @return 能转换的情况下，转换为Class<?>
     * */
    public <T extends Activity> T getLastActivity(Class<T> cls){

        Activity activity = getLastActivity();

        if(cls.isInstance(activity)){
            return cls.cast(activity);
        }

        return null;
    }

    /**
     * 获取所有Activity
     */
    public Stack<Activity> getAllActivityStacks() {
        return mActivityStack;
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            if (!activity.isFinishing()) {
                activity.finish();
                mActivityStack.remove(activity);
            }
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public void finishActivity(Class<?> cls) {
        for (Activity activity : mActivityStack) {
            if (activity.getClass().equals(cls)) {
                finishActivity(activity);
                break;
            }
        }
    }

    /**
     * 结束除当前传入以外所有Activity
     */
    public void finishOthersActivity(Class<?> cls) {
        if (mActivityStack != null)
            for (Activity activity : mActivityStack) {
                if (!activity.getClass().equals(cls)) {
                    activity.finish();
                }
            }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {

        if (mActivityStack != null) {
            for (Activity activity : mActivityStack) {
                activity.finish();
            }
            mActivityStack.clear();
        }
    }

    /**
     * 退出应用程序
     */
    public void AppExit() {
        try {
            finishAllActivity();
            android.os.Process.killProcess(android.os.Process.myPid());// 杀死该应用进程
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
