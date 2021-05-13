package com.mchenys.pluginloader.core.hook;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * @Author: mChenys
 * @Date: 2021/5/13
 * @Description: 找不到Activity会打开此Activity
 */
public class StubActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Go to the main activity
        Intent mainIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());

        if (mainIntent == null) {
            mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setPackage(getPackageName());

            ResolveInfo resolveInfo = getPackageManager().resolveActivity(mainIntent, 0);

            if (resolveInfo != null) {
                mainIntent.setClassName(this, resolveInfo.activityInfo.name);
            }
        }

        startActivity(mainIntent);

        finish();
    }
}