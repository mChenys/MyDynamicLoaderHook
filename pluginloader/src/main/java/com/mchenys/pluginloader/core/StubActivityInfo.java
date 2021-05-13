package com.mchenys.pluginloader.core;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import java.util.HashMap;

/**
 * @Author: mChenys
 * @Date: 2021/5/12
 * @Description: 管理占坑的Activity
 */
public class StubActivityInfo {
    /**
     * 四种启动模式的占坑数量
     */
    public static final int MAX_COUNT_STANDARD = 1;
    public static final int MAX_COUNT_SINGLETOP = 8;
    public static final int MAX_COUNT_SINGLETASK = 8;
    public static final int MAX_COUNT_SINGLEINSTANCE = 8;

    /**
     * 占坑Activity的包名
     */
    public static final String corePackage = "com.mchenys.pluginloader";
    public static final String STUB_ACTIVITY_STANDARD = "%s.A$%d"; // 例如com.mchenys.pluginloader.A$1~2
    public static final String STUB_ACTIVITY_SINGLETOP = "%s.B$%d"; // 例如com.mchenys.pluginloader.B$1~8
    public static final String STUB_ACTIVITY_SINGLETASK = "%s.C$%d"; // 例如com.mchenys.pluginloader.C$1~8
    public static final String STUB_ACTIVITY_SINGLEINSTANCE = "%s.D$%d"; // 例如com.mchenys.pluginloader.D$1~8

    /**
     * 四种启动模式Activity类名后缀,1.2.3...
     */
    public final int usedStandardStubActivity = 1;
    public int usedSingleTopStubActivity = 0;
    public int usedSingleTaskStubActivity = 0;
    public int usedSingleInstanceStubActivity = 0;

    /**
     * 缓存,key=targetClassName, value=stubClassName
     */
    private HashMap<String, String> mCachedStubActivity = new HashMap<>();

    /**
     * 根据启动模式返回合适的占坑Activity的类名
     *
     * @param className
     * @param launchMode
     * @param theme
     * @return
     */
    public String getStubActivity(String className, int launchMode, Resources.Theme theme) {
        String stubActivity = mCachedStubActivity.get(className);
        if (stubActivity != null) {
            return stubActivity;
        }

        TypedArray array = theme.obtainStyledAttributes(new int[]{
                android.R.attr.windowIsTranslucent,
                android.R.attr.windowBackground
        });
        boolean windowIsTranslucent = array.getBoolean(0, false);
        array.recycle();
        if (Constants.DEBUG) {
            Log.d(Constants.TAG_PREFIX + "StubActivityInfo", "getStubActivity, is transparent theme ? " + windowIsTranslucent);
        }
        // 格式化占坑Activity全路径类名,默认是标准启动模式
        stubActivity = String.format(STUB_ACTIVITY_STANDARD, corePackage, usedStandardStubActivity);
        switch (launchMode) {
            case ActivityInfo.LAUNCH_MULTIPLE: { // standard模式
                stubActivity = String.format(STUB_ACTIVITY_STANDARD, corePackage, usedStandardStubActivity);
                if (windowIsTranslucent) {
                    // 如果采用透明主题的类名是com.mchenys.pluginloader.A$2
                    stubActivity = String.format(STUB_ACTIVITY_STANDARD, corePackage, 2);
                }
                break;
            }
            case ActivityInfo.LAUNCH_SINGLE_TOP: { //singleTop模式
                // com.mchenys.pluginloader.B$1~8
                usedSingleTopStubActivity = usedSingleTopStubActivity % MAX_COUNT_SINGLETOP + 1;
                stubActivity = String.format(STUB_ACTIVITY_SINGLETOP, corePackage, usedSingleTopStubActivity);
                break;
            }
            case ActivityInfo.LAUNCH_SINGLE_TASK: { // singleTask模式
                // com.mchenys.pluginloader.C$1~8
                usedSingleTaskStubActivity = usedSingleTaskStubActivity % MAX_COUNT_SINGLETASK + 1;
                stubActivity = String.format(STUB_ACTIVITY_SINGLETASK, corePackage, usedSingleTaskStubActivity);
                break;
            }
            case ActivityInfo.LAUNCH_SINGLE_INSTANCE: { // singleInstance模式
                // com.mchenys.pluginloader.D$1~8
                usedSingleInstanceStubActivity = usedSingleInstanceStubActivity % MAX_COUNT_SINGLEINSTANCE + 1;
                stubActivity = String.format(STUB_ACTIVITY_SINGLEINSTANCE, corePackage, usedSingleInstanceStubActivity);
                break;
            }
            default:
                break;
        }
        // 保存缓存
        mCachedStubActivity.put(className, stubActivity);
        return stubActivity;
    }

}
