package com.mchenys.pluginloader.core;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.mchenys.pluginloader.core.hook.Android10_11Hook;
import com.mchenys.pluginloader.core.hook.Android5_7Hook;
import com.mchenys.pluginloader.core.hook.Android8_9Hook;
import com.mchenys.pluginloader.core.hook.IAndroidHook;
import com.mchenys.pluginloader.core.hook.PLInstrumentation;
import com.mchenys.pluginloader.utils.PluginUtil;
import com.mchenys.pluginloader.utils.ReflectUtils;
import com.mchenys.pluginloader.utils.ReflectionLimit;
import com.mchenys.pluginloader.utils.VersionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description: 插件管理类
 */
public class PluginManager {
    private static final String TAG = Constants.TAG_PREFIX + "PluginManager";

    private static final PluginManager sInstance = new PluginManager();
    // 缓存已加载的插件包
    private final Map<String, LoadedPlugin> mPlugins = new ConcurrentHashMap<>();
    private Context mContext;
    private Application mApplication;
    private ComponentsHandler mComponentsHandler;
    private PLInstrumentation mInstrumentation;


    private PluginManager() {
    }

    public static PluginManager getInstance() {
        return sInstance;
    }

    /**
     * 初始化操作
     *
     * @param context
     */
    public void init(Context context) {
        ReflectionLimit.clearLimit();
        if (context instanceof Application) {
            this.mApplication = (Application) context;
            this.mContext = mApplication.getBaseContext();
        } else {
            final Context app = context.getApplicationContext();
            if (app == null) {
                this.mContext = context;
                this.mApplication = (Application) mContext;
            } else {
                this.mApplication = (Application) app;
                this.mContext = mApplication.getBaseContext();
            }
        }

        this.mComponentsHandler = createComponentsHandler();
        hookCurrentProcess();
    }

    private void hookCurrentProcess() {
        hookInstrumentation();
        hookSystemServices();
        hookDataBindingUtil();
    }

    private void hookInstrumentation() {
        try {
            Object activityThread = ReflectUtils.getActivityThread();
            Instrumentation base = ReflectUtils.getField(activityThread, "mInstrumentation");
            ReflectUtils.setField(activityThread, "mInstrumentation", new PLInstrumentation(this, base));
            Log.e(TAG, "==================hookInstrumentation success!!! ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookSystemServices() {
        try {
            IAndroidHook androidHook = null;
            if (VersionUtils.isAndroid10_11()) {
                androidHook = new Android10_11Hook(this);
            }
            if (VersionUtils.isAndroid8_9()) {
                androidHook = new Android8_9Hook(this);
            }
            if (VersionUtils.isAndroid5_7()) {
                androidHook = new Android5_7Hook(this);
            }
            if (androidHook == null) {
                throw new RuntimeException("not support system version");
            }
            androidHook.hookAms(mApplication);
            Log.e(TAG, "==================hookAms success!!! ");
            androidHook.hookActivityThread(mApplication);
            Log.e(TAG, "==================hookActivityThread success!!! ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookDataBindingUtil() {

    }


    private ComponentsHandler createComponentsHandler() {
        return new ComponentsHandler(this);
    }

    /**
     * 获取宿主的上下文
     *
     * @return
     */
    public Context getHostContext() {
        return this.mContext;
    }

    /**
     * 返回所有已加载的插件包
     *
     * @return
     */
    public List<LoadedPlugin> getAllLoadedPlugins() {
        List<LoadedPlugin> list = new ArrayList<>();
        list.addAll(mPlugins.values());
        return list;
    }

    public LoadedPlugin getLoadedPlugin(Intent intent) {
        return getLoadedPlugin(PluginUtil.getComponent(intent));
    }

    public LoadedPlugin getLoadedPlugin(ComponentName component) {
        if (component == null) {
            return null;
        }
        return this.getLoadedPlugin(component.getPackageName());
    }

    public LoadedPlugin getLoadedPlugin(String packageName) {
        return this.mPlugins.get(packageName);
    }

    /**
     * @return PLInstrumentation
     */
    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
    }

    public ComponentsHandler getComponentsHandler() {
        return this.mComponentsHandler;
    }

    // 根据intent去收集需要启动的activity的信息
    public ResolveInfo resolveActivity(Intent intent) {
        return this.resolveActivity(intent, 0);
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        for (LoadedPlugin plugin : this.mPlugins.values()) {
            ResolveInfo resolveInfo = plugin.resolveActivity(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }
        }
        return null;
    }
}
