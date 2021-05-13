package com.mchenys.pluginloader.utils;

import java.lang.reflect.Method;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: 解决Android10之后的hide方法的反射失败问题
 */
public class ReflectionLimit {

    private static Object sVMRuntime;
    private static Method setHiddenApiExemptions;

    static {
        if (VersionUtils.isAndroid10_11()) {
            try {
                Method forName = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
                Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
                Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
                setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
                setHiddenApiExemptions.setAccessible(true);
                sVMRuntime = getRuntime.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //消除限制
    public static boolean clearLimit() {
        if (sVMRuntime == null || setHiddenApiExemptions == null) {
            return false;
        }
        try {
            setHiddenApiExemptions.invoke(sVMRuntime, new Object[]{new String[]{"L"}});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
