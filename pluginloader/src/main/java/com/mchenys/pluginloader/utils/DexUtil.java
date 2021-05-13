package com.mchenys.pluginloader.utils;

import android.os.Build;

import com.mchenys.pluginloader.core.Constants;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description:
 */
public class DexUtil {

    private static boolean sHasInsertedNativeLibrary = false;

    /**
     * 合并插件dex到宿主中
     *
     * @param dexClassLoader
     * @param baseClassLoader
     * @param nativeLibsDir
     * @throws Exception
     */
    public static void insertDex(DexClassLoader dexClassLoader, ClassLoader baseClassLoader, File nativeLibsDir) throws Exception {
        Object hostDexElements = ReflectUtils.getDexElements(ReflectUtils.getPathList(baseClassLoader));
        Object pluginDexElements = ReflectUtils.getDexElements(ReflectUtils.getPathList(dexClassLoader));
        Object allDexElements = combineArray(hostDexElements, pluginDexElements);
        applyDexElement(baseClassLoader, allDexElements);
        insertNativeLibrary(dexClassLoader, baseClassLoader, nativeLibsDir);
    }


    /**
     * 合并dexElements
     *
     * @param hostDexElements
     * @param pluginDexElements
     * @return
     */
    private static Object combineArray(Object hostDexElements, Object pluginDexElements) {
        int hostSize = Array.getLength(hostDexElements);
        int pluginSize = Array.getLength(pluginDexElements);
        Object newDexElements = Array.newInstance(hostDexElements.getClass().getComponentType(), hostSize + pluginSize);
        System.arraycopy(hostDexElements, 0, newDexElements, 0, hostSize);
        System.arraycopy(pluginDexElements, 0, newDexElements, hostSize, pluginSize);
        return newDexElements;
    }

    /**
     * 使用合并后的dexElement
     *
     * @param baseClassLoader
     * @param allDexElements
     * @throws Exception
     */
    private static void applyDexElement(ClassLoader baseClassLoader, Object allDexElements) throws Exception {
        Object pathList = ReflectUtils.getPathList(baseClassLoader);
        Field dexElements = pathList.getClass().getDeclaredField("dexElements");
        dexElements.setAccessible(true);
        dexElements.set(pathList, allDexElements);
    }

    /**
     * 合并插件的so
     *
     * @param dexClassLoader
     * @param baseClassLoader
     * @param nativeLibsDir
     * @throws Exception
     */
    private static synchronized void insertNativeLibrary(DexClassLoader dexClassLoader, ClassLoader baseClassLoader, File nativeLibsDir) throws Exception {
        if (sHasInsertedNativeLibrary) {
            return;
        }
        sHasInsertedNativeLibrary = true;

        // 宿主的DexPathList
        Object hostDexPathList = ReflectUtils.getPathList(baseClassLoader);
        // 插件的DexPathList
        Object pluginDexPathList = ReflectUtils.getPathList(dexClassLoader);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            // 获取宿主的nativeLibraryDirectories,添加插件的libs
            List<File> nativeLibraryDirectories = (List<File>) ReflectUtils.getNativeLibraryDirectories(hostDexPathList);
            nativeLibraryDirectories.add(nativeLibsDir);

            // 获取宿主的NativeLibraryElement[]
            Object hostNativeLibraryPathElements = ReflectUtils.getNativeLibraryPathElements(hostDexPathList);
            final int baseArrayLength = Array.getLength(hostNativeLibraryPathElements);

            // 获取插件的NativeLibraryElement[]
            Object pluginNativeLibraryPathElements = ReflectUtils.getNativeLibraryPathElements(pluginDexPathList);
            Class<?> elementClass = pluginNativeLibraryPathElements.getClass().getComponentType();
            Object allNativeLibraryPathElements = Array.newInstance(elementClass, baseArrayLength + 1);
            System.arraycopy(hostNativeLibraryPathElements, 0, allNativeLibraryPathElements, 0, baseArrayLength);

            // 从插件的NativeLibraryElement 中获取path或者dir属性
            Field soPathField;
            if (Build.VERSION.SDK_INT >= 26) {
                soPathField = elementClass.getDeclaredField("path");
            } else {
                soPathField = elementClass.getDeclaredField("dir");
            }
            soPathField.setAccessible(true);
            final int newArrayLength = Array.getLength(pluginNativeLibraryPathElements);
            for (int i = 0; i < newArrayLength; i++) {
                Object element = Array.get(pluginNativeLibraryPathElements, i);
                String dir = ((File) soPathField.get(element)).getAbsolutePath();
                if (dir.contains(Constants.NATIVE_DIR)) {
                    // 取出NativeLibraryElement 并插入到allNativeLibraryPathElements数组中
                    Array.set(allNativeLibraryPathElements, baseArrayLength, element);
                    break;
                }
            }
            // 更新宿主DexPathList的NativeLibraryElement[]
            ReflectUtils.setField(hostDexPathList, "nativeLibraryPathElements", allNativeLibraryPathElements);
        } else {
            File[] nativeLibraryDirectories = (File[]) ReflectUtils.getNativeLibraryDirectories(hostDexPathList);
            final int N = nativeLibraryDirectories.length;
            File[] newNativeLibraryDirectories = new File[N + 1];
            System.arraycopy(nativeLibraryDirectories, 0, newNativeLibraryDirectories, 0, N);
            // 直接追加
            newNativeLibraryDirectories[N] = nativeLibsDir;
            ReflectUtils.setField(hostDexPathList, "nativeLibraryDirectories", newNativeLibraryDirectories);
        }
    }

}
