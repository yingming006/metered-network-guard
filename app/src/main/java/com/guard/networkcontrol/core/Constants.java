package com.guard.networkcontrol.core;

public final class Constants {
    private Constants() {}

    public static final String PREFS_NAME = "guard_policy";
    public static final String KEY_WHITELIST = "whitelist";
    public static final String KEY_INITIALIZED = "initialized";

    public static final String[] DEFAULT_WHITELIST = {
        "com.android.phone",
        "com.android.providers.telephony",
        "com.android.mms",
        "com.android.messaging",
        "com.android.sms",
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.settings",
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.autonavi.minimap",
        "com.baidu.BaiduMap"
    };
}