package com.mchenys.pluginloader.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.mchenys.pluginloader.utils.ReflectUtils;

import java.util.ArrayList;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: 插件ActivityLifecycleCallbacks代理
 */
public class ActivityLifecycleCallbacksProxy implements Application.ActivityLifecycleCallbacks {

    final ArrayList<Application.ActivityLifecycleCallbacks> mActivityLifecycleCallbacks =
            ReflectUtils.getFieldSlience("android.app.Application", ReflectUtils.getActivityThreadApplication(), "mActivityLifecycleCallbacks");

    Object[] collectActivityLifecycleCallbacks() {
        if (mActivityLifecycleCallbacks == null) {
            return null;
        }
        Object[] callbacks = null;
        synchronized (mActivityLifecycleCallbacks) {
            if (mActivityLifecycleCallbacks.size() > 0) {
                callbacks = mActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityCreated(activity,
                        savedInstanceState);
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityStarted(activity);
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityResumed(activity);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityPaused(activity);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityStopped(activity);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivitySaveInstanceState(activity,
                        outState);
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((Application.ActivityLifecycleCallbacks) callbacks[i]).onActivityDestroyed(activity);
            }
        }
    }
}
