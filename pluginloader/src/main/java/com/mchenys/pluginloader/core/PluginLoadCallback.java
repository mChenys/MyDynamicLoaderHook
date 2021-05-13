package com.mchenys.pluginloader.core;

/**
 * @Author: mChenys
 * @Date: 2021/5/13
 * @Description:
 */
public interface PluginLoadCallback {
    /**
     * 添加完成
     */
    void onComplete(LoadedPlugin plugin);

    /**
     * 添加失败
     *
     * @param message
     */
    void onError(String message);
}
