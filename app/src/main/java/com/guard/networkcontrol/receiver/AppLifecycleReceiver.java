package com.guard.networkcontrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.guard.networkcontrol.core.ConfigStore;
import com.guard.networkcontrol.core.PolicyManager;

import java.util.Set;

/**
 * 阶段三：应用安装 / 卸载生命周期监听。
 * - PACKAGE_ADDED    : 新安装应用不在白名单，自动落入禁流量名单（毫秒级生效）；
 * - PACKAGE_REPLACED : 应用升级后重新落库，规则不丢失；
 * - PACKAGE_REMOVED  : 清理已卸载应用在白名单中的残留规则；
 * - PACKAGE_CHANGED  : 规则保持同步。
 */
public class AppLifecycleReceiver extends BroadcastReceiver {
    private static final String TAG = "AppLifecycleReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        // 仅 Device Owner 状态下才允许操作系统策略，普通安装一律跳过
        if (!AdminReceiver.isDeviceOwnerApp(context)) return;

        String action = intent.getAction();
        String self = context.getPackageName();
        Uri data = intent.getData();
        String pkg = (data != null) ? data.getSchemeSpecificPart() : null;
        if (pkg != null && pkg.equals(self)) return; // 忽略本应用自身的事件

        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action) && !replacing) {
            // 应用被卸载：清理白名单残留
            try {
                Set<String> wl = ConfigStore.loadWhitelist(context);
                if (pkg != null && wl.remove(pkg)) {
                    ConfigStore.saveWhitelist(context, wl);
                }
            } catch (Exception e) {
                Log.e(TAG, "cleanup whitelist failed", e);
            }
        }

        try {
            PolicyManager.applyPolicy(context, ConfigStore.loadWhitelist(context));
        } catch (Exception e) {
            Log.e(TAG, "applyPolicy failed on action=" + action, e);
        }
    }
}