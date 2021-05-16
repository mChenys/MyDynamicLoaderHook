package com.mchenys.pluginloader.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import com.mchenys.pluginloader.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description: 资源管理类
 */
public class ResourcesManager {
    public static Resources mCombineResource;
    /**
     * 创建合并后的Resources
     *
     * @param hostContext
     * @param apk
     * @return
     */
    public static Resources createMergeResources(Context hostContext, File apk) {
        Resources hostResources = hostContext.getResources();
        try {
            AssetManager assetManager = hostResources.getAssets();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //我们需要将应用原来加载的地址取出来
                List<String> cookieNames = new ArrayList<>();
                int stringBlockCount = (int) ReflectUtils.invokeMethod(assetManager, "getStringBlockCount");

                Method getCookieNameMethod = AssetManager.class.getDeclaredMethod("getCookieName", Integer.TYPE);
                getCookieNameMethod.setAccessible(true);

                for (int i = 0; i < stringBlockCount; i++) {
                    String cookieName =
                            (String) getCookieNameMethod.invoke(assetManager, new Object[]{i + 1});
                    // 保存地址
                    cookieNames.add(cookieName);
                }
                ReflectUtils.invokeMethod(assetManager, "destroy");// 释放AssetManager的Resource
                ReflectUtils.invokeMethod(assetManager, "init");// 重新初始化AssetManager
                ReflectUtils.setField(assetManager, "mStringBlocks", null);// 清空mStringBlocks字段
                // 将原来的assets添加进去，有了此步骤就不用刻意添加sourceDir了
                for (String path : cookieNames) {
                    ReflectUtils.invokeMethod(assetManager, "addAssetPath", new Class[]{String.class}, path);
                }
                //插入插件的资源地址
                ReflectUtils.invokeMethod(assetManager, "addAssetPath", new Class[]{String.class}, apk.getAbsolutePath());
                // 插入已加载的插件
                List<LoadedPlugin> pluginList = PluginManager.getInstance().getAllLoadedPlugins();
                for (LoadedPlugin plugin : pluginList) {
                    ReflectUtils.invokeMethod(assetManager, "addAssetPath", new Class[]{String.class}, plugin.getLocation());
                }
                // 执行ensureStringBlocks方法
                ReflectUtils.invokeMethod(assetManager, "ensureStringBlocks");
                // 执行updateConfiguratio方法,此行代码非常重要,因为后面在资源查找的时候是需要通过一个ResTable_config来获取当前手机的一些配置从而获取到准确的资源，
                // 如果不进行初始化则会出现找不到资源的崩溃
                hostResources.updateConfiguration(hostResources.getConfiguration(), hostResources.getDisplayMetrics());
            } else {
                ReflectUtils.invokeMethod(assetManager, "addAssetPath", new Class[]{String.class}, apk.getAbsolutePath());
                List<LoadedPlugin> pluginList = PluginManager.getInstance().getAllLoadedPlugins();
                for (LoadedPlugin plugin : pluginList) {
                    ReflectUtils.invokeMethod(assetManager, "addAssetPath", new Class[]{String.class}, plugin.getLocation());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCombineResource = hostResources;
        return hostResources;
    }
}
