package com.mchenys.pluginloader.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: 插件的Context
 */
public class PluginContext  extends ContextWrapper {

    private final LoadedPlugin mPlugin;

    public PluginContext(LoadedPlugin plugin) {
        super(plugin.getPluginManager().getHostContext());
        this.mPlugin = plugin;
    }

    public PluginContext(LoadedPlugin plugin, Context base) {
        super(base);
        this.mPlugin = plugin;
    }

    @Override
    public Context getApplicationContext() {
        return this.mPlugin.getApplication();
    }

    private Context getHostContext() {
        return getBaseContext();
    }

    @Override
    public ContentResolver getContentResolver() {
       // return new PluginContentResolver(getHostContext());
        return getBaseContext().getContentResolver();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.mPlugin.getClassLoader();
    }


    @Override
    public Object getSystemService(String name) {
        // intercept CLIPBOARD_SERVICE,NOTIFICATION_SERVICE
        if (name.equals(Context.CLIPBOARD_SERVICE)) {
            return getHostContext().getSystemService(name);
        } else if (name.equals(Context.NOTIFICATION_SERVICE)) {
            return getHostContext().getSystemService(name);
        }

        return super.getSystemService(name);
    }

    @Override
    public Resources getResources() {
        return this.mPlugin.getResources();
    }

    @Override
    public AssetManager getAssets() {
        return this.mPlugin.getAssets();
    }

    @Override
    public Resources.Theme getTheme() {
        return this.mPlugin.getTheme();
    }

    @Override
    public void startActivity(Intent intent) {
        ComponentsHandler componentsHandler = mPlugin.getPluginManager().getComponentsHandler();
        componentsHandler.transformIntentToExplicitAsNeeded(intent);
        super.startActivity(intent);
    }
}
