package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.regex.Pattern;

public class Cache {
    static private final String KEY_PREFIX = "cache_";
    static public boolean enabled = true;
    static public String get(Context context, String key){
        check(context);
        if(enabled){
            return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PREFIX + key, "");
        } else {
            return "";
        }
    }
    static public void put(Context context, String key, String value){
        check(context);
        if(enabled) PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PREFIX + key, value).apply();
    }
    static public void check(Context context){
        enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_use_cache", true);
        if(!enabled) clear(context);
    }
    static public void delete(Context context, String key){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences.contains(KEY_PREFIX + key)) sharedPreferences.edit().remove(KEY_PREFIX + key).apply();
    }
    static public void clear(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> list = sharedPreferences.getAll();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for(Map.Entry<String, ?> entry : list.entrySet()) {
            if(Pattern.compile("^"+KEY_PREFIX+".*").matcher(entry.getKey()).find()) editor.remove(entry.getKey());
        }
        editor.apply();
    }
}