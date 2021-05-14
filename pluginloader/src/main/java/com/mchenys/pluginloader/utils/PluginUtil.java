package com.mchenys.pluginloader.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.mchenys.pluginloader.core.Constants;
import com.mchenys.pluginloader.core.LoadedPlugin;
import com.mchenys.pluginloader.core.PluginManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description:
 */
public class PluginUtil {

    public static final String TAG = Constants.TAG_PREFIX + "NativeLib";

    /**
     * 获取intent的ComponentName
     *
     * @param intent 宿主的intent
     * @return 返回的ComponentName有可能是插件的
     */
    public static ComponentName getComponent(Intent intent) {
        if (intent == null) {
            return null;
        }
        if (isIntentFromPlugin(intent)) {
            return new ComponentName(intent.getStringExtra(Constants.KEY_TARGET_PACKAGE),
                    intent.getStringExtra(Constants.KEY_TARGET_CLASS));
        }
        return intent.getComponent();
    }

    /**
     * 是否是插件的Intent
     *
     * @param intent
     * @return
     */
    public static boolean isIntentFromPlugin(Intent intent) {
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra(Constants.KEY_IS_PLUGIN, false);
    }

    /**
     * 获取插件包的主题
     * @param intent
     * @return
     */
    public static int getTheme(Intent intent) {
        return getTheme(PluginUtil.getComponent(intent));
    }

    /**
     *  获取插件包的主题
     * @param component
     * @return
     */
    public static int getTheme(ComponentName component) {
        LoadedPlugin loadedPlugin = PluginManager.getInstance().getLoadedPlugin(component);

        if (null == loadedPlugin) {
            return 0;
        }

        ActivityInfo info = loadedPlugin.getActivityInfo(component);
        if (null == info) {
            return 0;
        }
        // 优先判断Activity的theme
         if (0 != info.theme) {
            return info.theme;
        }
        // 然后判断Application的theme
        ApplicationInfo appInfo = info.applicationInfo;
        if (null != appInfo && appInfo.theme != 0) {
            return appInfo.theme;
        }

        // 否则返回默认主题
        return selectDefaultTheme(0, Build.VERSION.SDK_INT);
    }

    /**
     * 选择默认的主题
     *
     * @param curTheme
     * @param targetSdkVersion
     * @return
     */
    public static int selectDefaultTheme(int curTheme, int targetSdkVersion) {
        return selectSystemTheme(curTheme, targetSdkVersion,
                android.R.style.Theme,
                android.R.style.Theme_Holo,
                android.R.style.Theme_DeviceDefault,
                android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
    }

    /**
     * 根据版本选中对应的默认主题
     * @param curTheme
     * @param targetSdkVersion
     * @param orig
     * @param holo
     * @param dark
     * @param deviceDefault
     * @return
     */
    public static int selectSystemTheme(final int curTheme, final int targetSdkVersion, final int orig, final int holo, final int dark, final int deviceDefault) {
        if (curTheme != 0) {
            return curTheme;
        }

        if (targetSdkVersion < 11 /* Build.VERSION_CODES.HONEYCOMB */) {
            return orig;
        }

        if (targetSdkVersion < 14 /* Build.VERSION_CODES.ICE_CREAM_SANDWICH */) {
            return holo;
        }

        if (targetSdkVersion < 24 /* Build.VERSION_CODES.N */) {
            return dark;
        }

        return deviceDefault;
    }

    /**
     * copy 插件包的libs和so
     *
     * @param apk
     * @param context
     * @param packageInfo
     * @param nativeLibDir
     * @throws Exception
     */
    public static void copyNativeLib(File apk, Context context, PackageInfo packageInfo, File nativeLibDir) throws Exception {
        long startTime = System.currentTimeMillis();
        ZipFile zipfile = new ZipFile(apk.getAbsolutePath());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (String cpuArch : Build.SUPPORTED_ABIS) {
                    if (findAndCopyNativeLib(zipfile, context, cpuArch, packageInfo, nativeLibDir)) {
                        return;
                    }
                }
            } else {
                if (findAndCopyNativeLib(zipfile, context, Build.CPU_ABI, packageInfo, nativeLibDir)) {
                    return;
                }
            }
            findAndCopyNativeLib(zipfile, context, "armeabi", packageInfo, nativeLibDir);
        } finally {
            zipfile.close();
            Log.d(TAG, "Done! +" + (System.currentTimeMillis() - startTime) + "ms");
        }
    }


    private static boolean findAndCopyNativeLib(ZipFile zipfile, Context context, String cpuArch, PackageInfo packageInfo, File nativeLibDir) throws Exception {
        Log.d(TAG, "Try to copy plugin's cup arch: " + cpuArch);
        boolean findLib = false;
        boolean findSo = false;
        byte buffer[] = null;
        String libPrefix = "lib/" + cpuArch + "/";
        ZipEntry entry;
        Enumeration e = zipfile.entries();

        while (e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();

            if (entryName.charAt(0) < 'l') {
                continue;
            }
            if (entryName.charAt(0) > 'l') {
                break;
            }
            if (!findLib && !entryName.startsWith("lib/")) {
                continue;
            }
            findLib = true;
            if (!entryName.endsWith(".so") || !entryName.startsWith(libPrefix)) {
                continue;
            }

            if (buffer == null) {
                findSo = true;
                Log.d(TAG, "Found plugin's cup arch dir: " + cpuArch);
                buffer = new byte[8192];
            }

            String libName = entryName.substring(entryName.lastIndexOf('/') + 1);
            Log.d(TAG, "verify so " + libName);
            File libFile = new File(nativeLibDir, libName);
            String key = packageInfo.packageName + "_" + libName;
            if (libFile.exists()) {
                int VersionCode = Settings.getSoVersion(context, key);
                if (VersionCode == packageInfo.versionCode) {
                    Log.d(TAG, "skip existing so : " + entry.getName());
                    continue;
                }
            }
            FileOutputStream fos = new FileOutputStream(libFile);
            Log.d(TAG, "copy so " + entry.getName() + " of " + cpuArch);
            copySo(buffer, zipfile.getInputStream(entry), fos);
            Settings.setSoVersion(context, key, packageInfo.versionCode);
        }

        if (!findLib) {
            Log.d(TAG, "Fast skip all!");
            return true;
        }

        return findSo;
    }

    private static void copySo(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        int count;

        while ((count = bufferedInput.read(buffer)) > 0) {
            bufferedOutput.write(buffer, 0, count);
        }
        bufferedOutput.flush();
        bufferedOutput.close();
        output.close();
        bufferedInput.close();
        input.close();
    }

    /**
     * 从参数中取出intent
     *
     * @param args
     * @return
     */
    public static Intent getIntent(Object[] args) {
        int index = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Intent) {
                index = i;
                break;
            }
        }
        return index != -1 ? (Intent) args[index] : null;
    }

    /**
     * 文件拷贝
     *
     * @param src
     * @param dest
     * @param deleteAfter 操作成功后是否需要删除源文件,true 删除
     * @throws Exception
     */
    public static void copy(File src, File dest, boolean deleteAfter) throws Exception {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            if (dest.exists()) {
                dest.delete();
            }
            dest.createNewFile();
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dest).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
            if (dest.exists() && deleteAfter) {
                //删除源文件
                src.delete();
            }
        }
    }
}
