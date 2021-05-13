package com.mchenys.pluginloadbyhook;

import android.app.Application;
import android.content.Context;

import com.mchenys.pluginloader.core.PluginManager;

/**
 * @Author: mChenys
 * @Date: 2021/5/13
 * @Description:
 */
public class App extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PluginManager.getInstance().init(this);
    }
}
