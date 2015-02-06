package com.hixos.smartwp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    public static void setBoolean(Context c, int key, boolean value) {
        SharedPreferences sp = getPreferences(c);
        sp.edit().putBoolean(c.getString(key), value).commit();
    }

    public static void setInt(Context c, int key, int value) {
        SharedPreferences sp = getPreferences(c);
        sp.edit().putInt(c.getString(key), value).commit();
    }

    public static void setLong(Context c, int key, long value) {
        SharedPreferences sp = getPreferences(c);
        sp.edit().putLong(c.getString(key), value).commit();
    }

    public static void setString(Context c, int key, String value) {
        SharedPreferences sp = getPreferences(c);
        sp.edit().putString(c.getString(key), value).commit();
    }

    public static long getLong(Context c, int key, long defaultVal) {
        SharedPreferences sp = getPreferences(c);
        return sp.getLong(c.getString(key), defaultVal);
    }

    public static int getInt(Context c, int key, int defaultVal) {
        SharedPreferences sp = getPreferences(c);
        return sp.getInt(c.getString(key), defaultVal);
    }

    public static boolean getBoolean(Context c, int key, boolean defaultVal) {
        SharedPreferences sp = getPreferences(c);
        return sp.getBoolean(c.getString(key), defaultVal);
    }

    public static String getString(Context c, int key, String defaultVal) {
        SharedPreferences sp = getPreferences(c);
        return sp.getString(c.getString(key), defaultVal);
    }

    public static SharedPreferences getPreferences(Context c) {
        return c.getSharedPreferences("palette_preferences", Context.MODE_MULTI_PROCESS);
    }

    public static void setFloat(Context c, int key, float value) {
        SharedPreferences sp = getPreferences(c);
        sp.edit().putFloat(c.getString(key), value).commit();
    }

    public static float getFloat(Context c, int key, float defaultVal) {
        SharedPreferences sp = getPreferences(c);
        return sp.getFloat(c.getString(key), defaultVal);
    }

    public static void clear(Context c) {
        SharedPreferences sp = getPreferences(c);
        sp.edit().clear().commit();
    }
}
