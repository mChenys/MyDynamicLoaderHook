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
    public void onCreate() {
        super.onCreate();
        PluginManager.getInstance().init(this);
    }
}
