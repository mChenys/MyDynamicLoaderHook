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
import java.util.List;

/**
 * @Author: mChenys
 * @Date: 2021/5/13
 * @Description: api 26-28
 */
public class Android8_9Hook extends Android5_7Hook {

    public Android8_9Hook(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public void hookAms(Application application) throws Exception {
        Class activityManagerClass = Class.forName("android.app.ActivityManager");
        Class iActivityManagerClass = Class.forName("android.app.IActivityManager");
        final Object activityManagerObj = ReflectUtils.invokeStaticMethod(activityManagerClass, "getService");
        Object iActivityManagerProxy = Proxy.newProxyInstance(application.getClassLoader(), new Class[]{iActivityManagerClass}, new AMSInvocationHandler(activityManagerObj, mPluginManager));
        //Singleton<IActivityManager> IActivityManagerSingleton
        Object singletonObj = ReflectUtils.getStaticField(activityManagerClass, "IActivityManagerSingleton");
        ReflectUtils.setField(Class.forName("android.util.Singleton"), singletonObj, "mInstance", iActivityManagerProxy);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (Constants.EXECUTE_TRANSACTION == msg.what) {
            try {
                // ClientTransaction
                Object obj = msg.obj;
                Class<?> ClientTransactionClass = Class.forName("android.app.servertransaction.ClientTransaction");
                Class<?> LaunchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");
                if (ClientTransactionClass.isInstance(obj)) {
                    List mActivityCallbacks = ReflectUtils.getField(obj, "mActivityCallbacks");
                    for (int i = 0; i < mActivityCallbacks.size(); i++) {
                        // android.app.servertransaction.LaunchActivityItem
                        Object launchActivityItem = mActivityCallbacks.get(i);
                        if (LaunchActivityItemClass.isInstance(launchActivityItem)) {
                           /* Intent mIntent = ReflectUtils.getField(launchActivityItem, "mIntent");
                            //获取插件的intent进行替换
                            Intent pluginIntent = new Intent();
                            pluginIntent.setComponent(PluginUtil.getComponent(mIntent));
                            ReflectUtils.setField(launchActivityItem, "mIntent", pluginIntent);*/

                            // 处理intent交给PLInstrumentation处理,这里只设置占坑Activity的主题
                            Intent intent = ReflectUtils.getField(LaunchActivityItemClass, launchActivityItem, "mIntent");
                            if (PluginUtil.isIntentFromPlugin(intent)) {
                                ActivityInfo activityInfo = ReflectUtils.getField(LaunchActivityItemClass, launchActivityItem, "mInfo");
                                int theme = PluginUtil.getTheme(intent);
                                if (theme != 0) {
                                    Log.e(TAG, "resolve theme, current theme:" + activityInfo.theme + "  after :0x" + Integer.toHexString(theme));
                                    activityInfo.theme = theme;// 给占坑Activity设置主题
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return false;
    }

    @Override
    public void hookActivityThread(Application application) throws Exception {
        super.hookActivityThread(application);
    }
}
