package com.mchenys.pluginloader.core.hook;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;

import com.mchenys.pluginloader.core.Constants;
import com.mchenys.pluginloader.core.LoadedPlugin;
import com.mchenys.pluginloader.core.PluginManager;
import com.mchenys.pluginloader.utils.PluginUtil;
import com.mchenys.pluginloader.utils.ReflectUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: 主要处理插件Activity的创建
 */
public class PLInstrumentation extends Instrumentation {
    public static final String TAG = Constants.TAG_PREFIX + "PLInstrumentation";
    private final Instrumentation mBase;
    private final ArrayList<WeakReference<Activity>> mActivities = new ArrayList<>();
    private PluginManager mPluginManager;

    public PLInstrumentation(PluginManager pluginManager, Instrumentation base) {
        this.mPluginManager = pluginManager;
        this.mBase = base;
    }


    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            cl.loadClass(className);
            Log.i(TAG, String.format("newActivity[%s]", className));

        } catch (ClassNotFoundException e) {
            ComponentName component = PluginUtil.getComponent(intent);

            if (component == null) {
                return newActivity(mBase.newActivity(cl, className, intent));
            }

            String targetClassName = component.getClassName();
            Log.d(TAG, String.format("newActivity[%s : %s/%s]", className, component.getPackageName(), targetClassName));

            LoadedPlugin plugin = this.mPluginManager.getLoadedPlugin(component);

            if (plugin == null) {
                // Not found then goto stub activity.
                boolean debuggable = false;
                try {
                    Context context = this.mPluginManager.getHostContext();
                    debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                } catch (Throwable ex) {

                }

                if (debuggable) {
                    throw new ActivityNotFoundException("error intent: " + intent.toURI());
                }

                Log.i(TAG, "Not found. starting the stub activity: " + StubActivity.class);
                return newActivity(mBase.newActivity(cl, StubActivity.class.getName(), intent));
            }

            Activity activity = mBase.newActivity(plugin.getClassLoader(), targetClassName, intent);
            activity.setIntent(intent);

            // for 4.1+
            try {
                // 修改插件Activity的mResources
                ReflectUtils.setField(Class.forName("android.view.ContextThemeWrapper"), activity, "mResources", plugin.getResources());
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            return newActivity(activity);
        }

        return newActivity(mBase.newActivity(cl, className, intent));
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        injectActivity(activity);
        mBase.callActivityOnCreate(activity, icicle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        injectActivity(activity);
        mBase.callActivityOnCreate(activity, icicle, persistentState);
    }

    protected void injectActivity(Activity activity) {
        final Intent intent = activity.getIntent();
        if (PluginUtil.isIntentFromPlugin(intent)) {
            Context base = activity.getBaseContext();
            try {
                LoadedPlugin plugin = this.mPluginManager.getLoadedPlugin(intent);
                // 修改Context的mResources
                ReflectUtils.setField(base, "mResources", plugin.getResources());
                // 修改ContextWrapper的mBase
                ReflectUtils.setField(Class.forName("android.content.ContextWrapper"), activity, "mBase", plugin.createPluginContext(activity.getBaseContext()));
                // 修改插件Activity的mApplication
                ReflectUtils.setField(activity, "mBase", plugin.getApplication());

                // set screenOrientation
                ActivityInfo activityInfo = plugin.getActivityInfo(PluginUtil.getComponent(intent));
                if (activityInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    activity.setRequestedOrientation(activityInfo.screenOrientation);
                }

                // for native activity
                ComponentName component = PluginUtil.getComponent(intent);
                Intent wrapperIntent = new Intent(intent);
                wrapperIntent.setClassName(component.getPackageName(), component.getClassName());
                wrapperIntent.setExtrasClassLoader(activity.getClassLoader());
                activity.setIntent(wrapperIntent);

            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }

    @Override
    public Context getContext() {
        return mBase.getContext();
    }

    @Override
    public Context getTargetContext() {
        return mBase.getTargetContext();
    }

    @Override
    public ComponentName getComponentName() {
        return mBase.getComponentName();
    }

    protected Activity newActivity(Activity activity) {
        synchronized (mActivities) {
            for (int i = mActivities.size() - 1; i >= 0; i--) {
                if (mActivities.get(i).get() == null) {
                    mActivities.remove(i);
                }
            }
            mActivities.add(new WeakReference<>(activity));
        }
        return activity;
    }

    List<WeakReference<Activity>> getActivities() {
        synchronized (mActivities) {
            return new ArrayList<>(mActivities);
        }
    }

}
