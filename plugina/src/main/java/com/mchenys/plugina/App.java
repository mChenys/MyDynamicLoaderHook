package com.mchenys.plugina;

import android.app.Application;
import android.util.Log;

/**
 * Email 643353964@qq.com
 * Create by mChenys on 2021/5/14
 * Describe:
 */
public class App  extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("cys", "plugina application run...");
        Log.e("App", "mResources:" + (getResources() == null ? "" : getResources().hashCode()));
        Log.e("App", "mBase:" + (getBaseContext() == null ? "" : getBaseContext().hashCode()));
    }
}
