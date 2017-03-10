package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.regex.Pattern;

public class Storage {
    static private final String KEY_PREFIX = "storage_";
    static public String get(Context context, String key){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PREFIX + key, "");
    }
    static public void put(Context context, String key, String value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PREFIX + key, value).apply();
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