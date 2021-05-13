package com.mchenys.pluginloader.core.hook;

import android.content.Context;
import android.content.Intent;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mchenys.pluginloader.core.Constants;
import com.mchenys.pluginloader.core.PluginManager;
import com.mchenys.pluginloader.utils.PluginUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: AMS方法处理
 */
public class AMSInvocationHandler implements InvocationHandler {

    private static final String TAG = Constants.TAG_PREFIX + "AMSHandler";
    private final PluginManager mPluginManager;
    private final Object mBase; // 真正代理的对象

    public AMSInvocationHandler(Object base, PluginManager pluginManager) {
        this.mBase = base;
        this.mPluginManager = pluginManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            return startActivity(method, args);
        } else if ("startService".equals(method.getName())) {
            try {
                return startService(method, args);
            } catch (Throwable e) {
                Log.e(TAG, "Start service error", e);
            }
        } else if ("stopService".equals(method.getName())) {
            try {
                return stopService(method, args);
            } catch (Throwable e) {
                Log.e(TAG, "Stop Service error", e);
            }
        } else if ("stopServiceToken".equals(method.getName())) {
            try {
                return stopServiceToken(method, args);
            } catch (Throwable e) {
                Log.e(TAG, "Stop service token error", e);
            }
        } else if ("bindService".equals(method.getName())) {
            try {
                return bindService(method, args);
            } catch (Throwable e) {
                Log.w(TAG, e);
            }
        } else if ("unbindService".equals(method.getName())) {
            try {
                return unbindService(method, args);
            } catch (Throwable e) {
                Log.w(TAG, e);
            }
        } else if ("getIntentSender".equals(method.getName())) {
            try {
                getIntentSender(method, args);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        } else if ("overridePendingTransition".equals(method.getName())) {
            try {
                overridePendingTransition(method, args);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        try {
            return method.invoke(this.mBase, args);
        } catch (Throwable th) {
            Log.w(TAG, th);
        }
        return null;
    }

    /**
     * 启动Activity
     *
     * @param method
     * @param args
     * @return
     * @throws Exception
     */
    private Object startActivity(Method method, Object[] args) throws Exception {
        Intent intent = PluginUtil.getIntent(args);
        if (null != intent) {
            // intent转换
            mPluginManager.getComponentsHandler().transformIntentToExplicitAsNeeded(intent);
            // null component is an implicitly intent
            if (intent.getComponent() != null) {
                Log.d(TAG, String.format("injectIntent for startActivity[%s : %s]", intent.getComponent().getPackageName(), intent.getComponent().getClassName()));
                // resolve intent with Stub Activity if needed
                this.mPluginManager.getComponentsHandler().markIntentIfNeeded(intent);
            }
        }
        return method.invoke(this.mBase, args);
    }

    private void overridePendingTransition(Method method, Object[] args) {

    }

    private void getIntentSender(Method method, Object[] args) {

    }

    private Object unbindService(Method method, Object[] args) {
        return null;
    }

    private Object bindService(Method method, Object[] args) {
        return null;
    }

    private Object stopServiceToken(Method method, Object[] args) {
        return null;
    }

    private Object stopService(Method method, Object[] args) {
        return null;
    }

    private Object startService(Method method, Object[] args) {
        return null;
    }


}
