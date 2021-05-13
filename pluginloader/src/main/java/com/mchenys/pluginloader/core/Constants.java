package com.mchenys.pluginloader.core;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description:
 */
public class Constants {

    public static final int EXECUTE_TRANSACTION = 159;
    public final static int LAUNCH_ACTIVITY = 100;

    public static final String KEY_IS_PLUGIN = "isPlugin";
    public static final String KEY_TARGET_PACKAGE = "target.package";
    public static final String KEY_TARGET_CLASS = "target.class";

    public static final String OPTIMIZE_DIR = "odex";
    public static final String NATIVE_DIR = "nativeLibs";

    public static final boolean COMBINE_RESOURCES = true;
    public static final boolean COMBINE_CLASSLOADER = true;
    public static final boolean DEBUG = true;

    public static final String TAG = "PLoader";
    public static final String TAG_PREFIX = TAG + ".";
}
