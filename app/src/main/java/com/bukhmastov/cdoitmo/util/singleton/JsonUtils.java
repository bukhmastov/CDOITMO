package com.bukhmastov.cdoitmo.util.singleton;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class JsonUtils {

    @Nullable
    public static JSONObject getJsonObject(JSONObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        try {
            Object object = json.get(key);
            if (object == null) {
                return null;
            }
            return (JSONObject) object;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static JSONObject getJsonObject(JSONArray json, int index) {
        try {
            Object object = json.get(index);
            if (object == null) {
                return null;
            }
            return (JSONObject) object;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static JSONArray getJsonArray(JSONObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        try {
            Object object = json.get(key);
            if (object == null) {
                return null;
            }
            return (JSONArray) object;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static JSONArray getJsonArray(JSONArray json, int index) {
        try {
            Object object = json.get(index);
            if (object == null) {
                return null;
            }
            return (JSONArray) object;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getString(JSONObject json, String key) {
        return getString(json, key, null);
    }

    public static String getString(JSONObject json, String key, String orElse) {
        if (!json.has(key)) {
            return orElse;
        }
        try {
            Object object = json.get(key);
            if (json.isNull(key) || object == null) {
                return orElse;
            }
            String value = (String) object;
            return value.equals("null") ? orElse : value;
        } catch (Exception e) {
            return null;
        }
    }

    public static int getInt(JSONObject json, String key) {
        return getInt(json, key, 0);
    }

    public static int getInt(JSONObject json, String key, int orElse) {
        if (!json.has(key)) {
            return orElse;
        }
        try {
            return json.getInt(key);
        } catch (Exception e) {
            return orElse;
        }
    }

    public static String getIntAsString(JSONObject json, String key) {
        return getIntAsString(json, key, null);
    }

    public static String getIntAsString(JSONObject json, String key, String orElse) {
        if (!json.has(key)) {
            return orElse;
        }
        try {
            int value = json.getInt(key);
            return String.valueOf(value);
        } catch (Exception e) {
            return orElse;
        }
    }

    public static boolean getBoolean(JSONObject json, String key) {
        return getBoolean(json, key, true);
    }

    public static boolean getBoolean(JSONObject json, String key, Boolean orElse) {
        if (!json.has(key)) {
            return orElse;
        }
        try {
            return json.getBoolean(key);
        } catch (Exception e) {
            return orElse;
        }
    }

    public static ArrayList<JSONObject> toArrayListOfJsonObjects(JSONObject json, String key) {
        return toArrayListOfJsonObjects(getJsonArray(json, key));
    }

    public static ArrayList<JSONObject> toArrayListOfJsonObjects(JSONArray json) {
        ArrayList<JSONObject> array = new ArrayList<>();
        if (json == null) {
            return array;
        }
        for (int i = 0; i < json.length(); i++) {
            array.add(getJsonObject(json, i));
        }
        return array;
    }
}
