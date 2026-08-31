package com.guard.networkcontrol.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 阶段一：Device Admin / Device Owner 接收器。
 * 注册于 device_admin.xml，通过 adb dpm set-device-owner 激活为设备所有者后获得：
 * 1. setMeteredDataDisabledPackages（移动流量禁用名单）的控制权；
 * 2. 防卸载：普通用户在设置/桌面均无法直接卸载设备所有者应用。
 */
public class AdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "AdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.i(TAG, "device admin enabled; isDeviceOwner=" + isDeviceOwnerApp(context));
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.i(TAG, "device admin disabled");
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        Log.i(TAG, "device owner provisioning complete");
    }

    public static boolean isDeviceOwnerApp(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    public static ComponentName adminComponent(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }
}