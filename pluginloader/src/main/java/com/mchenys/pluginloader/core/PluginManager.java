package com.mchenys.pluginloader.core;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.mchenys.pluginloader.core.hook.Android10_11Hook;
import com.mchenys.pluginloader.core.hook.Android5_7Hook;
import com.mchenys.pluginloader.core.hook.Android8_9Hook;
import com.mchenys.pluginloader.core.hook.IAndroidHook;
import com.mchenys.pluginloader.core.hook.PLInstrumentation;
import com.mchenys.pluginloader.utils.PluginUtil;
import com.mchenys.pluginloader.utils.ReflectUtils;
import com.mchenys.pluginloader.utils.VersionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description: 插件管理类
 */
public class PluginManager {
    private static final String TAG = Constants.TAG_PREFIX + "PluginManager";

    private static final PluginManager sInstance = new PluginManager();
    // 缓存已加载的插件包
    private final Map<String, LoadedPlugin> mLoadedPluginMap = new ConcurrentHashMap<>();
    private Context mContext;
    private Application mApplication;
    private ComponentsHandler mComponentsHandler;
    private PLInstrumentation mInstrumentation;
    private ExecutorService mThreadPool = Executors.newCachedThreadPool();
    // 插件apk目录
    private File mPluginApkDir;

    private PluginManager() {
    }

    public static PluginManager getInstance() {
        return sInstance;
    }

    public Map<String, LoadedPlugin> getLoadedPlugins() {
        return mLoadedPluginMap;
    }

    /**
     * 初始化操作
     *
     * @param context
     */
    public void init(Context context) {
        if (context instanceof Application) {
            this.mApplication = (Application) context;
            this.mContext = mApplication.getBaseContext();
        } else {
            Context app = context.getApplicationContext();
            if (app == null) {
                this.mContext = context;
                this.mApplication = (Application) mContext;
            } else {
                this.mApplication = (Application) app;
                this.mContext = mApplication.getBaseContext();
            }
        }
        this.mComponentsHandler = createComponentsHandler();
        this.mPluginApkDir = mContext.getDir(Constants.PLUGIN_DIR, Context.MODE_PRIVATE);
        hookCurrentProcess();
        loadInnerPlugin();
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
            mInstrumentation = new PLInstrumentation(this, base);
            mInstrumentation.onCreate(new Bundle());
            ReflectUtils.setField(activityThread, "mInstrumentation", mInstrumentation);
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
                Log.e(TAG, "==================hookAms Android10_11");
            }
            if (VersionUtils.isAndroid8_9()) {
                androidHook = new Android8_9Hook(this);
                Log.e(TAG, "==================hookAms Android8_9");
            }
            if (VersionUtils.isAndroid5_7()) {
                androidHook = new Android5_7Hook(this);
                Log.e(TAG, "==================hookAms Android5_7");
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

    /**
     * 加载已存在的插件,保证每次启动的时候可用
     */
    @WorkerThread
    private void loadInnerPlugin() {
        if (null != mPluginApkDir.listFiles())
            for (File file : mPluginApkDir.listFiles()) {
                try {
                    loadPlugin(file, false, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }

    /**
     * 加载外部插件
     *
     * @param file
     * @param callback
     */
    public void addPlugin(File file, PluginLoadCallback callback) {
        loadPlugin(file, true, callback);
    }

    /**
     * 注意:同步操作,需要在子线程中调用
     *
     * @param file
     * @return LoadedPlugin
     */
    public LoadedPlugin addPlugin(File file) {
        try {
            // 先copy到私有目录
            File dest = new File(mPluginApkDir, file.getName());
            PluginUtil.copy(file, dest, true);
            if (dest.exists() && dest.length() > 0) {
                return LoadedPlugin.create(PluginManager.this, mContext, dest, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 加载插件
     *
     * @param file      插件包
     * @param forceLoad 是否强制加载
     * @param callback  加载回调
     */
    @WorkerThread
    private void loadPlugin(File file, boolean forceLoad, PluginLoadCallback callback) {
        workOnThread(new Runnable() {
            @Override
            public void run() {
                try {
                    File destFile = file;
                    if (!file.getParentFile().getAbsolutePath().equals(mPluginApkDir.getAbsolutePath())) {
                        // 先copy到私有目录
                        destFile = new File(mPluginApkDir, file.getName());
                        PluginUtil.copy(file, destFile, true);

                    }
                    if (null != destFile && destFile.exists() && destFile.length() > 0) {
                        LoadedPlugin loadedPlugin = LoadedPlugin.create(PluginManager.this, mContext, destFile, forceLoad);
                        if (null != callback) {
                            callback.onComplete(loadedPlugin);
                        }
                    } else {
                        if (null != callback) callback.onError("无效的插件包");
                    }
                } catch (Exception e) {
                    if (null != callback) callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * 在子线程中执行
     *
     * @param runnable
     */
    private void workOnThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mThreadPool.submit(runnable);
        } else runnable.run();
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
        list.addAll(mLoadedPluginMap.values());
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
        return this.mLoadedPluginMap.get(packageName);
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

    // 根据intent去获取对应的ResolveInfo
    public ResolveInfo resolveActivity(Intent intent) {
        return this.resolveActivity(intent, 0);
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        for (LoadedPlugin plugin : this.mLoadedPluginMap.values()) {
            // 获取对应插件的ResolveInfo
            ResolveInfo resolveInfo = plugin.resolveActivity(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }
        }
        return null;
    }

    /**
     * 卸载插件
     *
     * @param packageName
     * @return true 成功 ，false 失败
     */
    public boolean uninstallPlugin(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        LoadedPlugin loadedPlugin = mLoadedPluginMap.get(packageName);
        if (loadedPlugin == null) {
            return true;
        }
        File pluginFile = new File(loadedPlugin.mLocation);
        boolean success = pluginFile.delete();
        if (success) {
            mLoadedPluginMap.remove(packageName);
            return true;
        }
        return false;
    }

    /**
     * 获取插件的Fragment
     *
     * @param packageName
     * @param className
     * @return
     */
    public Fragment getPluginFragment(String packageName, String className) {
        LoadedPlugin loadedPlugin = mLoadedPluginMap.get(packageName);
        if (null != loadedPlugin) {
            try {
                if (Constants.COMBINE_CLASSLOADER) {
                    return (Fragment) Class.forName(className).newInstance();
                } else {
                    return (Fragment) Class.forName(className, true, loadedPlugin.mClassLoader).newInstance();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
