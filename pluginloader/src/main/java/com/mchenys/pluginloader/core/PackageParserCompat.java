package com.mchenys.pluginloader.core;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import com.mchenys.pluginloader.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Constructor;

/**
 * @Author: mChenys
 * @Date: 2021/5/11
 * @Description: 解析apk管理类, 需要处理兼容性
 */
public class PackageParserCompat {

    public static int PARSE_MUST_BE_APK;
    private static Object sPackageParser; // PackageParser instance
    private static Class sPackageClass; // PackageParser$Package

    static {
        try {
            Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");
            sPackageParser = packageParserClass.newInstance();
            sPackageClass = Class.forName("android.content.pm.PackageParser$Package");
            PARSE_MUST_BE_APK = ReflectUtils.getStaticField(packageParserClass, "PARSE_MUST_BE_APK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param context
     * @param apk
     * @param flags
     * @return PackageParser.Package
     */
    public static final Object parsePackage(final Context context, final File apk, final int flags) {
        try {
            if (Build.VERSION.SDK_INT >= 28
                    || (Build.VERSION.SDK_INT == 27 && Build.VERSION.PREVIEW_SDK_INT != 0)) { // Android P Preview
                return PackageParserPPreview.parsePackage(context, apk, flags);
            } else if (Build.VERSION.SDK_INT >= 24) {
                return PackageParserV24.parsePackage(context, apk, flags);
            } else if (Build.VERSION.SDK_INT >= 21) {
                return PackageParserLollipop.parsePackage(context, apk, flags);
            } else {
                return PackageParserLegacy.parsePackage(context, apk, flags);
            }

        } catch (Throwable e) {
            throw new RuntimeException("error", e);
        }
    }

    private static final class PackageParserPPreview {
        /**
         * @param context
         * @param apk
         * @param flags
         * @return PackageParser.Package
         * @throws Throwable
         */
        static final Object parsePackage(Context context, File apk, int flags) throws Throwable {
            Object pkg = ReflectUtils.invokeMethod(sPackageParser, "parsePackage", new Class[]{File.class, int.class}, apk, flags);
            ReflectUtils.invokeMethod(sPackageParser, "collectCertificates", new Class[]{pkg.getClass(), boolean.class}, pkg, false);
            return pkg;
        }
    }

    private static final class PackageParserV24 {
        /**
         * @param context
         * @param apk
         * @param flags
         * @return PackageParser.Package
         * @throws Throwable
         */
        static final Object parsePackage(Context context, File apk, int flags) throws Throwable {
            Object pkg = ReflectUtils.invokeMethod(sPackageParser, "parsePackage", new Class[]{File.class, int.class}, apk, flags);
            ReflectUtils.invokeMethod(sPackageParser, "collectCertificates", new Class[]{pkg.getClass(), int.class}, pkg, flags);
            return pkg;
        }
    }

    private static final class PackageParserLollipop {
        /**
         * @param context
         * @param apk
         * @param flags
         * @return PackageParser.Package
         * @throws Throwable
         */
        static final Object parsePackage(final Context context, final File apk, final int flags) throws Throwable {
            Object pkg = ReflectUtils.invokeMethod(sPackageParser, "parsePackage", new Class[]{File.class, int.class}, apk, flags);
            ReflectUtils.invokeMethod(sPackageParser, "collectCertificates", new Class[]{pkg.getClass(), int.class}, pkg, flags);
            return pkg;
        }

    }

    private static final class PackageParserLegacy {
        /**
         * @param context
         * @param apk
         * @param flags
         * @return PackageParser.Package
         * @throws Throwable
         */
        static final Object parsePackage(Context context, File apk, int flags) throws Throwable {
            Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");
            Constructor<?> constructor = packageParserClass.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            sPackageParser = constructor.newInstance(apk.getAbsolutePath());
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            Object pkg = ReflectUtils.invokeMethod(sPackageParser, "parsePackage", new Class[]{File.class, String.class, displayMetrics.getClass(), int.class},
                    apk, apk.getAbsolutePath(), displayMetrics, flags);
            ReflectUtils.invokeMethod(sPackageParser, "collectCertificates", new Class[]{pkg.getClass(), int.class}, pkg, flags);
            return pkg;
        }

    }
}
