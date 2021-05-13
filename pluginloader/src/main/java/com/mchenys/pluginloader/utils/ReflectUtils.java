package com.mchenys.pluginloader.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description:
 */
public class ReflectUtils {

    static {
        ReflectionLimit.clearLimit();
    }

    /**
     * 获取DexPathList 的dexElements
     *
     * @param pathList:DexPathList
     * @return Element[]
     * @throws Exception
     */
    public static Object getDexElements(Object pathList) throws Exception {
        Field dexElements = pathList.getClass().getDeclaredField("dexElements");
        dexElements.setAccessible(true);
        return dexElements.get(pathList);
    }

    /**
     * 获取ClassLoader中的DexPathList
     *
     * @param classLoader
     * @return DexPathList
     * @throws Exception
     */
    public static Object getPathList(ClassLoader classLoader) throws Exception {
        Field pathList = classLoader.getClass().getDeclaredField("pathList");
        pathList.setAccessible(true);
        return pathList.get(classLoader);
    }

    /**
     * 获取ActivityThread的Application对象
     *
     * @return Application
     * @throws Exception
     */
    public static Object getActivityThreadApplication() {
        try {
            return ReflectUtils.invokeStaticMethod(Class.forName("android.app.ActivityThread"), "currentApplication");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取ActivityThread
     *
     * @return ActivityThread
     * @throws Exception
     */
    public static Object getActivityThread() throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        return activityThreadClass.getMethod("currentActivityThread").invoke(null);
    }

    /**
     * 获取DexPathList的nativeLibraryDirectories属性
     *
     * @param dexPathList:DexPathList
     * @return api22之后是List<File>, 之前是 File[]
     */
    public static Object getNativeLibraryDirectories(Object dexPathList) throws Exception {
        Field nativeLibraryDirectories = dexPathList.getClass().getDeclaredField("nativeLibraryDirectories");
        nativeLibraryDirectories.setAccessible(true);
        return nativeLibraryDirectories.get(dexPathList);
    }


    /**
     * 获取DexPathList的nativeLibraryPathElements属性
     *
     * @param dexPathList
     * @return NativeLibraryElement[]
     */
    public static Object getNativeLibraryPathElements(Object dexPathList) throws Exception {
        Field nativeLibraryPathElements = dexPathList.getClass().getDeclaredField("nativeLibraryPathElements");
        nativeLibraryPathElements.setAccessible(true);
        return nativeLibraryPathElements.get(dexPathList);
    }

    /**
     * 设置属性
     *
     * @param instance
     * @param fieldName
     * @param value
     * @throws Exception
     */
    public static void setField(Object instance, String fieldName, Object value) throws Exception {
        setField(instance.getClass(), instance, fieldName, value);
    }

    public static void setField(Class<?> clazz, Object instance, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * 执行方法
     *
     * @param instance
     * @param methodName
     * @return
     * @throws Exception
     */
    public static Object invokeMethod(Object instance, String methodName) throws Exception {
        return invokeMethod(instance, methodName, null, null);
    }

    /**
     * 执行方法
     *
     * @param instance
     * @param methodName
     * @param argsType
     * @param args
     * @return
     * @throws Exception
     */
    public static Object invokeMethod(Object instance, String methodName, Class<?>[] argsType, Object... args) throws Exception {
        return invokeMethod(instance.getClass(), instance, methodName, argsType, args);
    }

    public static Object invokeMethod(Class<?> clazz, Object instance, String methodName, Class<?>[] argsType, Object... args) throws Exception {
        Method method = clazz.getDeclaredMethod(methodName, argsType);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    /**
     * 执行方法
     *
     * @param clazz
     * @param methodName
     * @return
     * @throws Exception
     */
    public static Object invokeStaticMethod(Class<?> clazz, String methodName) throws Exception {
        return invokeStaticMethod(clazz, methodName, null, null);
    }

    /**
     * 执行静态方法
     *
     * @param clazz
     * @param methodName
     * @param argsType
     * @param args
     * @return
     * @throws Exception
     */
    public static Object invokeStaticMethod(Class<?> clazz, String methodName, Class<?>[] argsType, Object... args) throws Exception {
        Method method = clazz.getMethod(methodName, argsType);
        method.setAccessible(true);
        return method.invoke(null, args);
    }


    /**
     * 获取静态属性
     *
     * @param tClass
     * @param fieldName
     * @param <T>
     * @return
     */
    public static <T> T getStaticField(Class<?> tClass, String fieldName) {
        try {
            Field declaredField = tClass.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return (T)declaredField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取对象的属性
     *
     * @param instance
     * @param fieldName
     * @param <T>
     * @return
     */
    public static <T> T getField(Object instance, String fieldName) {
        return getField(instance.getClass(), instance, fieldName);
    }

    public static <T> T getField(Class<?> clazz, Object instance, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}