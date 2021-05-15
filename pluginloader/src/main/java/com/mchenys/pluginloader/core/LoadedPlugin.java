package com.mchenys.pluginloader.core;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.viewpager2.widget.MarginPageTransformer;

import com.mchenys.pluginloader.utils.DexUtil;
import com.mchenys.pluginloader.utils.PluginUtil;
import com.mchenys.pluginloader.utils.ReflectUtils;
import com.mchenys.pluginloader.utils.RunUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description: 已加载的插件包封装
 */
public class LoadedPlugin {
    public static final String TAG = Constants.TAG_PREFIX + "LoadedPlugin";

    public final String mLocation;
    public PluginManager mPluginManager;
    public Context mHostContext;
    public Context mPluginContext;
    public final File mNativeLibDir;
    public final Object mPackage; // PackageParser.Package
    public final PackageInfo mPackageInfo;
    public Resources mResources;
    public ClassLoader mClassLoader;
    public Application mApplication; // 插件的Application
    public Intent mLaunchIntent;
    public Map<ComponentName, ActivityInfo> mActivityInfos; // 插件包的ActivityInfo

    private LoadedPlugin(PluginManager pluginManager, Context context, PackageInfo packageInfo, File apk) throws Exception {
        this.mPluginManager = pluginManager;
        this.mHostContext = context;
        this.mLocation = apk.getAbsolutePath();
        this.mPackage = PackageParserCompat.parsePackage(context, apk, PackageParserCompat.PARSE_MUST_BE_APK);
        this.mPackageInfo = packageInfo;
        this.mPluginContext = createPluginContext(null);
        this.mNativeLibDir = getDir(context, Constants.NATIVE_DIR);
        this.mResources = createResources(context, apk);
        this.mClassLoader = createClassLoader(context, apk, this.mNativeLibDir, context.getClassLoader());
        this.mLaunchIntent = getLaunchIntent();
        this.mActivityInfos = getActivityInfo(mPackageInfo);
        tryToCopyNativeLib(apk);
        invokeApplication();
        Log.e(TAG, "mResources:" + mResources.hashCode());
    }

    /**
     * 创建插件包
     *
     * @param pluginManager
     * @param context
     * @param apk
     * @param forceLoad     强制加载
     * @return
     */
    public static LoadedPlugin create(PluginManager pluginManager, Context context, File apk, boolean forceLoad) throws Exception {
        PackageInfo packageInfo = getPackageInfo(context, apk);
        String packageName = packageInfo.packageName;
        Map<String, LoadedPlugin> pluginMap = pluginManager.getLoadedPlugins();
        LoadedPlugin loadedPlugin = pluginMap.get(packageName);
        if (loadedPlugin == null || forceLoad) {
            loadedPlugin = new LoadedPlugin(pluginManager, context, packageInfo, apk);
            pluginMap.put(packageName, loadedPlugin);
        }
        return loadedPlugin;
    }

    /**
     * 获取插件包的PackageInfo
     *
     * @param context
     * @param apk
     * @return
     * @throws Exception
     */
    private static PackageInfo getPackageInfo(Context context, File apk) throws Exception {
       /* return context.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),
                PackageManager.GET_ACTIVITIES |
                        PackageManager.GET_RECEIVERS |
                        PackageManager.GET_PROVIDERS |
                        PackageManager.GET_SERVICES);*/
        return context.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),
                PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
    }


    private File getDir(Context context, String name) {
        return context.getDir(name, Context.MODE_PRIVATE);
    }

    /**
     * 创建插件ClassLoader
     *
     * @param context
     * @param apk
     * @param libsDir
     * @param parent
     * @return
     * @throws Exception
     */
    private ClassLoader createClassLoader(Context context, File apk, File libsDir, ClassLoader parent) throws Exception {
        File dexOutputDir = getDir(context, Constants.OPTIMIZE_DIR);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        DexClassLoader pluginClassLoader = new DexClassLoader(apk.getAbsolutePath(), dexOutputPath, libsDir.getAbsolutePath(), parent);

        if (Constants.COMBINE_CLASSLOADER) {
            DexUtil.insertDex(pluginClassLoader, parent, libsDir);
        }
        return pluginClassLoader;
    }

    /**
     * 创建插件AssetManager
     *
     * @param apk
     * @return
     * @throws Exception
     */
    private AssetManager createAssetManager(File apk) throws Exception {
        AssetManager am = AssetManager.class.newInstance();
        ReflectUtils.invokeMethod(am, "addAssetPath", new Class[]{String.class}, apk.getAbsolutePath());
        return am;
    }

    /**
     * 创建插件Resources
     *
     * @param context
     * @param apk
     * @return
     * @throws Exception
     */
    private Resources createResources(Context context, File apk) throws Exception {
        if (Constants.COMBINE_RESOURCES) {
            // 合并资源
            return ResourcesManager.createMergeResources(context, apk);
        } else {
            // 返回插件的资源
            Resources hostResources = context.getResources();
            AssetManager assetManager = createAssetManager(apk);
            return new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
        }
    }

    /**
     * 赋值so
     *
     * @param apk
     * @throws Exception
     */
    private void tryToCopyNativeLib(File apk) throws Exception {
        PluginUtil.copyNativeLib(apk, mHostContext, mPackageInfo, mNativeLibDir);
    }

    public PluginContext createPluginContext(Context context) {
        if (context == null) {
            return new PluginContext(this);
        }
        return new PluginContext(this, context);
    }

    // 执行插件Application#onCreate
    private void invokeApplication() throws Exception {
        final Exception[] temp = new Exception[1];
        // make sure application's callback is run on ui thread.
        RunUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mApplication != null) {
                    return;
                }
                try {
                    mApplication = makeApplication(false, mPluginManager.getInstrumentation());
                } catch (Exception e) {
                    temp[0] = e;
                }
            }
        }, true);

        if (temp[0] != null) {
            throw temp[0];
        }
    }

    private Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) throws Exception {
        if (null != this.mApplication) {
            return this.mApplication;
        }

        String appClass = this.mPackageInfo.applicationInfo.className;
        if (forceDefaultAppClass || null == appClass) {
            appClass = "android.app.Application";
        }
        Object activityThread = null;
        try {
            Field mThread = Class.forName("android.app.Instrumentation").getDeclaredField("mThread");
            mThread.setAccessible(true);
            activityThread = mThread.get(instrumentation);
        } catch (Exception e) {
        }
        if (activityThread == null) {
            this.mApplication = (Application) this.mClassLoader.loadClass(appClass).newInstance();
            //  app.attach(context);
            ReflectUtils.invokeMethod(Class.forName("android.app.Application"), mApplication,
                    "attach", new Class[]{Class.forName("android.content.Context")}, this.getPluginContext());
        } else {
            // 创建插件的Application
            this.mApplication = instrumentation.newApplication(this.mClassLoader, appClass, this.getPluginContext());
        }
        // inject activityLifecycleCallbacks of the host application
        mApplication.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksProxy());
        instrumentation.callApplicationOnCreate(this.mApplication);
        return this.mApplication;
    }


    /**
     * 封装ActivityInfo
     *
     * @param packageInfo
     * @return
     */
    private Map<ComponentName, ActivityInfo> getActivityInfo(PackageInfo packageInfo) {
        Map<ComponentName, ActivityInfo> map = new HashMap<>();
        for (ActivityInfo activity : packageInfo.activities) {
            map.put(new ComponentName(packageInfo.packageName, activity.name), activity);
        }
        return map;
    }


    /**
     * 返回插件包路径
     *
     * @return
     */
    public String getLocation() {
        return this.mLocation;
    }

    /**
     * 插件包名
     *
     * @return
     */
    public String getPackageName() {
        return this.mPackageInfo.packageName;
    }

    /**
     * 返回插件AssetManager
     *
     * @return
     */
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    /**
     * 返回插件Resources
     *
     * @return
     */
    public Resources getResources() {
        return this.mResources;
    }

    /**
     * 返回插件Theme
     *
     * @return
     */
    public Resources.Theme getTheme() {
        Resources.Theme theme = this.mResources.newTheme();
        theme.applyStyle(PluginUtil.selectDefaultTheme(this.mPackageInfo.applicationInfo.theme, Build.VERSION.SDK_INT), false);
        return theme;
    }

    /**
     * 更新Resource
     *
     * @param newResources
     */
    public void updateResources(Resources newResources) {
        this.mResources = newResources;
    }

    /**
     * 插件ClassLoader
     *
     * @return
     */
    public ClassLoader getClassLoader() {
        return this.mClassLoader;
    }

    /**
     * 插件管理类
     *
     * @return
     */
    public PluginManager getPluginManager() {
        return this.mPluginManager;
    }

    /**
     * 宿主Context
     *
     * @return
     */
    public Context getHostContext() {
        return this.mHostContext;
    }

    /**
     * 插件Context
     *
     * @return
     */
    public Context getPluginContext() {
        return this.mPluginContext;
    }

    /**
     * 插件的Application
     *
     * @return
     */
    public Application getApplication() {
        return mApplication;
    }


    /**
     * 获取启动Intent
     *
     * @return
     */
    public Intent getLaunchIntent() {
        try {
            ContentResolver resolver = this.mPluginContext.getContentResolver();
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            ArrayList/*ArrayList<Activity>*/ activities = ReflectUtils.getField(this.mPackage, "activities");
            for (Object/*PackageParser.Activity*/ activity : activities) {
                ArrayList/*ArrayList<ActivityIntentInfo>*/ intents = ReflectUtils.getField(Class.forName("android.content.pm.PackageParser$Component"), activity, "intents");
                if (null != intents) {
                    for (Object/*PackageParser.ActivityIntentInfo*/ intentInfo : intents) {

                        boolean match = (int) ReflectUtils.invokeMethod(Class.forName("android.content.IntentFilter"), intentInfo, "match", new Class[]{
                                Class.forName("android.content.ContentResolver"),
                                Class.forName("android.content.Intent"),
                                boolean.class,
                                String.class
                        }, resolver, launcher, false, TAG) > 0;
                        if (match) {
                            return Intent.makeMainActivity((ComponentName) ReflectUtils.invokeMethod(Class.forName("android.content.pm.PackageParser$Component"), activity, "getComponentName", null));
                        }

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //=====================根据intent返回ResolveInfo==============================================
    public ResolveInfo resolveActivity(Intent intent, int flags) {
        List<ResolveInfo> query = this.queryIntentActivities(intent, flags);
        if (null == query || query.isEmpty()) {
            return null;
        }

        ContentResolver resolver = this.mPluginContext.getContentResolver();
        return chooseBestActivity(intent, intent.resolveTypeIfNeeded(resolver), flags, query);
    }

    private ResolveInfo chooseBestActivity(Intent intent, String s, int flags, List<ResolveInfo> query) {
        return query.get(0);
    }


    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        if (null != component) {
            for (Map.Entry<ComponentName, ActivityInfo> entry : mActivityInfos.entrySet()) {
                ComponentName source = entry.getKey();
                if (match(source, component)) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = entry.getValue();
                    resolveInfos.add(resolveInfo);
                }
            }
        } else {
            try {
                ArrayList/*ArrayList<Activity>*/ activities = ReflectUtils.getField(this.mPackage, "activities");
                for (Object/*PackageParser.Activity*/ activity : activities) {
                    ArrayList/*ArrayList<ActivityIntentInfo>*/ intents = ReflectUtils.getField(Class.forName("android.content.pm.PackageParser$Component"), activity, "intents");
                    for (Object/*PackageParser.ActivityIntentInfo*/ intentInfo : intents) {
                        boolean match = (int) ReflectUtils.invokeMethod(Class.forName("android.content.IntentFilter"), intentInfo, "match", new Class[]{
                                Class.forName("android.content.ContentResolver"),
                                Class.forName("android.content.Intent"),
                                boolean.class,
                                String.class
                        }, resolver, intent, true, TAG) >= 0;
                        if (match) {
                            ResolveInfo resolveInfo = new ResolveInfo();
                            resolveInfo.activityInfo = ReflectUtils.getField(Class.forName("android.content.pm.PackageParser$Activity"), activity, "info");
                            resolveInfos.add(resolveInfo);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resolveInfos;
    }

    /**
     * 返回对应的 ActivityInfo
     *
     * @param componentName
     * @return
     */
    public ActivityInfo getActivityInfo(ComponentName componentName) {
        return this.mActivityInfos.get(componentName);
    }

    /**
     * 匹配ComponentName
     *
     * @param source
     * @param target
     * @return
     */
    public boolean match(ComponentName source, ComponentName target) {
        if (source == target) return true;
        if (source != null && target != null
                && source.getClassName().equals(target.getClassName())
                && (source.getPackageName().equals(target.getPackageName())
                || mHostContext.getPackageName().equals(target.getPackageName()))) {
            return true;
        }
        return false;
    }
    //=====================根据intent返回ResolveInfo==============================================
}
