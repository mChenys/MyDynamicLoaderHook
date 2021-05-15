package com.mchenys.pluginloader.core.hook;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mchenys.pluginloader.core.Constants;
import com.mchenys.pluginloader.core.PluginManager;
import com.mchenys.pluginloader.utils.PluginUtil;
import com.mchenys.pluginloader.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author: mChenys
 * @Date: 2021/5/13
 * @Description: api21-25的hook AMS 和 ActivityThread
 */
public class Android5_7Hook implements IAndroidHook {
    protected String TAG = Constants.TAG_PREFIX;

    protected final PluginManager mPluginManager;

    public Android5_7Hook(PluginManager pluginManager) {
        this.mPluginManager = pluginManager;
        this.TAG += this.getClass().getSimpleName();
    }

    @Override
    public void hookAms(Application application) throws Exception {
        Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
        Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
        Object activityManagerNativeObj = ReflectUtils.invokeStaticMethod(activityManagerNativeClass, "getDefault");

        Object iActivityManagerProxy = Proxy.newProxyInstance(application.getClassLoader(), new Class[]{iActivityManagerClass},
                new AMSInvocationHandler(activityManagerNativeObj, mPluginManager));

        // Singleton<IActivityManager> gDefault
        Object singletonObj = ReflectUtils.getStaticField(activityManagerNativeClass, "gDefault");
        ReflectUtils.setField(Class.forName("android.util.Singleton"), singletonObj, "mInstance", iActivityManagerProxy);

    }

    @Override
    public void hookActivityThread(Application application) throws Exception {
        Object activityThread = ReflectUtils.getActivityThread();
        Object mH = ReflectUtils.getField(activityThread, "mH");
        ReflectUtils.setField(Class.forName("android.os.Handler"), mH, "mCallback", this);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (Constants.LAUNCH_ACTIVITY == msg.what) {
            // ActivityClientRecord
            try {
                Object activityClientRecord = msg.obj;
                Intent intent = ReflectUtils.getField(activityClientRecord, "intent");
                if (PluginUtil.isIntentFromPlugin(intent)) {
                    ActivityInfo activityInfo = ReflectUtils.getField(activityClientRecord, "activityInfo");
                    int theme = PluginUtil.getTheme(intent);
                    if (theme != 0) {
                        Log.d(TAG, "resolve theme, current theme:" + activityInfo.theme + "  after :0x" + Integer.toHexString(theme));
                        activityInfo.theme = theme;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
