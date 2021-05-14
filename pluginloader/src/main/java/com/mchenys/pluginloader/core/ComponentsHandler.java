package com.mchenys.pluginloader.core;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: 组件管理类
 */
public class ComponentsHandler {
    public static final String TAG = Constants.TAG_PREFIX + "ComponentsHandler";

    private Context mHostContext;
    private PluginManager mPluginManager;
    private StubActivityInfo mStubActivityInfo = new StubActivityInfo();


    private ArrayMap<ComponentName, Service> mServices = new ArrayMap<ComponentName, Service>();
    private ArrayMap<IBinder, Intent> mBoundServices = new ArrayMap<IBinder, Intent>();
    private ArrayMap<Service, AtomicInteger> mServiceCounters = new ArrayMap<Service, AtomicInteger>();

    public ComponentsHandler(PluginManager pluginManager) {
        mPluginManager = pluginManager;
        mHostContext = pluginManager.getHostContext();
    }

    /**
     * 将隐式意图转显示意图
     *
     * @param intent
     * @return
     */
    public Intent transformIntentToExplicitAsNeeded(Intent intent) {
        ComponentName component = intent.getComponent();
        if (component == null
                || component.getPackageName().equals(mHostContext.getPackageName())) {
            ResolveInfo info = mPluginManager.resolveActivity(intent);
            if (info != null && info.activityInfo != null) {
                component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                intent.setComponent(component);
            }
        }
        return intent;
    }

    /**
     * 标记intent
     *
     * @param intent
     */
    public void markIntentIfNeeded(Intent intent) {
        if (intent.getComponent() == null) {
            return;
        }

        String targetPackageName = intent.getComponent().getPackageName();
        String targetClassName = intent.getComponent().getClassName();
        // search map and return specific launchmode stub activity
        if (!targetPackageName.equals(mHostContext.getPackageName()) && mPluginManager.getLoadedPlugin(targetPackageName) != null) {
            intent.putExtra(Constants.KEY_IS_PLUGIN, true);
            intent.putExtra(Constants.KEY_TARGET_PACKAGE, targetPackageName);
            intent.putExtra(Constants.KEY_TARGET_CLASS, targetClassName);
            // 选择合适的占坑Activity
            dispatchStubActivity(intent);
        }
    }

    /**
     * 根据intent选择合适占位Activity
     *
     * @param intent
     */
    private void dispatchStubActivity(Intent intent) {
        ComponentName component = intent.getComponent();
        String targetClassName = intent.getComponent().getClassName();
        LoadedPlugin loadedPlugin = mPluginManager.getLoadedPlugin(intent);
        ActivityInfo info = loadedPlugin.getActivityInfo(component);
        if (info == null) {
            throw new RuntimeException("can not find " + component);
        }
        int launchMode = info.launchMode;
        Resources.Theme themeObj = loadedPlugin.getResources().newTheme();
        themeObj.applyStyle(info.theme, true);
        // 获取占坑Activity的类名
        String stubActivity = mStubActivityInfo.getStubActivity(targetClassName, launchMode, themeObj);
        Log.e(TAG, String.format("dispatchStubActivity,[%s -> %s]", targetClassName, stubActivity));
        // 设置启动的占坑Activity
        intent.setClassName(mHostContext, stubActivity);
    }


    public AtomicInteger getServiceCounter(Service service) {
        return this.mServiceCounters.get(service);
    }

    /**
     * Retrieve the started service by component name
     *
     * @param component
     * @return
     */
    public Service getService(ComponentName component) {
        return this.mServices.get(component);
    }

    /**
     * Put the started service into service registry, and then increase the counter associate with
     * the service
     *
     * @param component
     * @param service
     */
    public void rememberService(ComponentName component, Service service) {
        synchronized (this.mServices) {
            this.mServices.put(component, service);
            this.mServiceCounters.put(service, new AtomicInteger(0));
        }
    }

    /**
     * Remove the service from service registry
     *
     * @param component
     * @return
     */
    public Service forgetService(ComponentName component) {
        synchronized (this.mServices) {
            Service service = this.mServices.remove(component);
            this.mServiceCounters.remove(service);
            return service;
        }
    }

    /**
     * Remove the bound service from service registry
     *
     * @param iServiceConnection IServiceConnection binder when unbindService
     * @return
     */
    public Intent forgetIServiceConnection(IBinder iServiceConnection) {
        synchronized (this.mBoundServices) {
            Intent intent = this.mBoundServices.remove(iServiceConnection);
            return intent;
        }
    }

    /**
     * save the bound service
     *
     * @param iServiceConnection IServiceConnection binder when bindService
     * @return
     */
    public void remberIServiceConnection(IBinder iServiceConnection, Intent intent) {
        synchronized (this.mBoundServices) {
            mBoundServices.put(iServiceConnection, intent);
        }
    }

    /**
     * Check if a started service with the specified component exists in the registry
     *
     * @param component
     * @return
     */
    public boolean isServiceAvailable(ComponentName component) {
        return this.mServices.containsKey(component);
    }
}
