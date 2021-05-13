package com.mchenys.pluginloader.core.hook;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.IBinder;
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
import java.util.List;

import javax.xml.transform.OutputKeys;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: api 29-30
 */
public class Android10_11Hook extends Android8_9Hook {

    public Android10_11Hook(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public void hookAms(Application application) throws Exception {
        Class<?> iActivityTaskManagerClass = Class.forName("android.app.IActivityTaskManager");
        Class<?> activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
        Object activityTaskManagerObj = ReflectUtils.invokeStaticMethod(activityTaskManagerClass, "getService");
        Object iActivityTaskManagerProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{iActivityTaskManagerClass},
                new AMSInvocationHandler(activityTaskManagerObj, mPluginManager));
        // Singleton<IActivityTaskManager> IActivityTaskManagerSingleton
        Object singletonObj = ReflectUtils.getStaticField(activityTaskManagerClass, "IActivityTaskManagerSingleton");
        ReflectUtils.setField(Class.forName("android.util.Singleton"), singletonObj, "mInstance", iActivityTaskManagerProxy);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return super.handleMessage(msg);
    }

    @Override
    public void hookActivityThread(Application application) throws Exception {
        super.hookActivityThread(application);
    }
}
