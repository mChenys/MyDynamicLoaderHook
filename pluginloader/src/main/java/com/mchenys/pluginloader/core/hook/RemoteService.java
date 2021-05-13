package com.mchenys.pluginloader.core.hook;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * @Author: mChenys
 * @Date: 2021/5/13
 * @Description:
 */
public class RemoteService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
