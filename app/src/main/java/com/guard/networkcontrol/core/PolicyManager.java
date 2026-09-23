package com.guard.networkcontrol.core;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.guard.networkcontrol.receiver.AdminReceiver;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * 系统联网基础设施包：一旦进入禁用名单，DNS 解析 / 联网校验 / 门户登录 / 系统 UI
     * 会被一起切断，表现为「已连接 Wi-Fi 但所有应用都无法访问网络」，因此永不加入禁用名单。
     */
    private static final Set<String> PROTECTED_PACKAGES = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.shell",
            "com.android.settings",
            "com.android.providers.settings",
            "com.android.networkstack",
            "com.android.networkstack.tethering",
            "com.google.android.networkstack",
            "com.google.android.networkstack.tethering",
            "com.android.captiveportallogin",
            "com.google.android.captiveportallogin",
            "com.android.wifi.dialog",
            "com.android.vpndialogs"
    ));

    /** 本应用自身、联网关键包、以及与系统共享 UID 的平台组件（uid < 10000）都必须放行。 */
    private static boolean isProtected(Context context, ApplicationInfo ai) {
        if (ai.packageName.equals(context.getPackageName())) return true;
        if (PROTECTED_PACKAGES.contains(ai.packageName)) return true;
        return ai.uid < Process.FIRST_APPLICATION_UID;
    }

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
            if (allow.contains(ai.packageName)) {
                allowedApps.add(ai.packageName);
                continue;
            }
            // 系统联网基础设施不参与管控，避免整机断网
            if (isProtected(context, ai)) continue;
            disabled.add(ai.packageName);
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