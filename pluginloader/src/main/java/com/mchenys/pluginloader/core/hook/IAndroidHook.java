package com.mchenys.pluginloader.core.hook;

import android.app.Application;
import android.os.Handler;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description:
 */
public interface IAndroidHook extends Handler.Callback {

    void hookAms(Application application) throws Exception;

    void hookActivityThread(Application application) throws Exception;
}
