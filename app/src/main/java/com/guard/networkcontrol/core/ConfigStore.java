package com.guard.networkcontrol.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

/** 策略本地持久化（SharedPreferences）：移动数据白名单。 */
public final class ConfigStore {

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---------- 移动数据白名单 ----------
    public static Set<String> loadWhitelist(Context context) {
        String raw = sp(context).getString(Constants.KEY_WHITELIST, null);
        if (raw == null) {
            Set<String> def = new HashSet<>();
            for (String p : Constants.DEFAULT_WHITELIST) def.add(p);
            return def;
        }
        return parseSet(raw);
    }

    public static void saveWhitelist(Context context, Set<String> whitelist) {
        sp(context).edit()
                .putString(Constants.KEY_WHITELIST, toJson(whitelist))
                .putBoolean(Constants.KEY_INITIALIZED, true)
                .apply();
    }

    private static String toJson(Set<String> set) {
        JSONArray arr = new JSONArray();
        for (String p : set) arr.put(p);
        return arr.toString();
    }

    private static Set<String> parseSet(String raw) {
        Set<String> set = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) set.add(arr.getString(i));
        } catch (Exception ignored) {}
        return set;
    }

    public static boolean isInitialized(Context context) {
        return sp(context).getBoolean(Constants.KEY_INITIALIZED, false);
    }
}