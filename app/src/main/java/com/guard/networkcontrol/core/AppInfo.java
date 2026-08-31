package com.guard.networkcontrol.core;

import android.graphics.drawable.Drawable;

/** 应用列表行数据。 */
public final class AppInfo {
    public final String packageName;
    public final String label;
    public Drawable icon;
    public boolean allowMetered;

    public AppInfo(String packageName, String label, boolean allowMetered) {
        this.packageName = packageName;
        this.label = label;
        this.allowMetered = allowMetered;
    }
}