package com.guard.networkcontrol.core;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.guard.networkcontrol.receiver.AdminReceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 阶段二：移动管控流量策略引擎
 * 封装 DevicePolicyManager#setMeteredDataDisabledPackages —— 一次性把【禁用名单】写入
 * 系统计费网络（移动数据）名单，Android 内核原生生效、静默无弹窗，其余应用在移动数据下静默受限。
 */
public final class PolicyManager {
    private static final String TAG = "PolicyManager";

    public static boolean isDeviceOwner(Context context) {
        return AdminReceiver.isDeviceOwnerApp(context);
    }

    public static boolean isActiveAdmin(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(AdminReceiver.adminComponent(context));
    }

    /**
     * 直接调用系统 API 写【移动数据禁用名单】（需要 Device Owner 且 API >= 28）。
     * 返回值：系统落座后的新禁用名单；失败返回 null。
     */
    public static List<String> setMeteredDataDisabledPackages(Context context, Collection<String> packages) {
        if (Build.VERSION.SDK_INT < 28) return null;
        if (!isDeviceOwner(context)) return null;
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) return null;
        try {
            return dpm.setMeteredDataDisabledPackages(AdminReceiver.adminComponent(context), new ArrayList<>(packages));
        } catch (Exception e) {
            Log.e(TAG, "setMeteredDataDisabledPackages failed", e);
            return null;
        }
    }

    /** 读取当前系统实际禁用的名单（用于显示/自检）。 */
    public static List<String> getMeteredDataDisabledPackages(Context context) {
        if (Build.VERSION.SDK_INT < 28) return new ArrayList<>();
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) return new ArrayList<>();
        try {
            return dpm.getMeteredDataDisabledPackages(AdminReceiver.adminComponent(context));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 策略应用结果。 */
    public static final class ApplyResult {
        public final boolean success;
        public final int deniedCount;
        public final int allowedCount;
        public final int totalInstalled;
        public static ApplyResult failed() { return new ApplyResult(false, 0, 0, 0); }
        public ApplyResult(boolean success, int denied, int allowed, int total) {
            this.success = success; this.deniedCount = denied; this.allowedCount = allowed; this.totalInstalled = total;
        }
    }

    /**
     * 全量下发策略：disabled = 全部已安装应用 - 白名单 - 本应用。
     * 白名单以外的（含所有新安装应用）一律禁用移动数据流量；幂等可随时重跑。
     */
    public static ApplyResult applyPolicy(Context context, Set<String> whitelist) {
        String self = context.getPackageName();
        Set<String> allow = (whitelist == null) ? new HashSet<>() : new HashSet<>(whitelist);
        allow.add(self);

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installed = new ArrayList<>();
        try {
            installed = pm.getInstalledApplications(0);
        } catch (Exception e) {
            Log.e(TAG, "getInstalledApplications failed", e);
        }

        Set<String> disabled = new HashSet<>();
        Set<String> allowedApps = new HashSet<>();
        for (ApplicationInfo ai : installed) {
            if (ai.packageName == null) continue;
            if (allow.contains(ai.packageName)) allowedApps.add(ai.packageName);
            else disabled.add(ai.packageName);
        }

        List<String> applied = setMeteredDataDisabledPackages(context, disabled);
        boolean ok = applied != null;
        return new ApplyResult(ok, disabled.size(), allowedApps.size(), installed.size());
    }

    /** 供开机 / 生命周期监听器随时重新落库（读本地白名单）。 */
    public static void reapplyPolicy(Context context) {
        applyPolicy(context, ConfigStore.loadWhitelist(context));
    }
}