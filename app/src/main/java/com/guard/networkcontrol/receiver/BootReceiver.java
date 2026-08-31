package com.guard.networkcontrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.guard.networkcontrol.core.PolicyManager;

/**
 * 开机自检：系统启动后重新下发一次流量管控策略（幂等），
 * 覆盖 BOOT_COMPLETED / LOCKED_BOOT_COMPLETED / MY_PACKAGE_REPLACED。
 * 设备重启后策略依旧在系统底层生效（同时 setMeteredDataDisabledPackages
 * 本身已持久化到系统，此处为兜底与自愈）。
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!AdminReceiver.isDeviceOwnerApp(context)) return;

        Log.i(TAG, "reapply policy on action=" + intent.getAction());
        try {
            PolicyManager.reapplyPolicy(context);
        } catch (Exception e) {
            Log.e(TAG, "reapply failed", e);
        }
    }
}