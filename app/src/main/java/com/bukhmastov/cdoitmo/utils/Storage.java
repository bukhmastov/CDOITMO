package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Storage {

    private static final String TAG = "Storage";
    private static final String APP_FOLDER = "app_data";
    private enum STORAGE {CACHE, PERMANENT}
    private enum TYPE {USER, GENERAL}

    public static class file {
        public static class cache {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, STORAGE.CACHE, TYPE.USER, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, STORAGE.CACHE, TYPE.USER, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, STORAGE.CACHE, TYPE.USER, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, STORAGE.CACHE, TYPE.USER, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, STORAGE.CACHE, TYPE.USER);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, STORAGE.CACHE, path, TYPE.USER);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.CACHE, TYPE.USER, path);
            }
            public static ArrayList<String> list(Context context, String path){
                return Storage.file.list(context, STORAGE.CACHE, TYPE.USER, path);
            }
        }
        public static class perm {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, STORAGE.PERMANENT, TYPE.USER, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, STORAGE.PERMANENT, TYPE.USER, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, STORAGE.PERMANENT, TYPE.USER, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, STORAGE.PERMANENT, TYPE.USER, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, STORAGE.PERMANENT, TYPE.USER);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, STORAGE.PERMANENT, path, TYPE.USER);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.PERMANENT, TYPE.USER, path);
            }
            public static ArrayList<String> list(Context context, String path){
                return Storage.file.list(context, STORAGE.PERMANENT, TYPE.USER, path);
            }
        }
        public static class all {
            public static boolean clear(Context context) {
                return Storage.file.clear(context, null, TYPE.USER);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, null, path, TYPE.USER);
            }
            public static boolean reset(Context context) {
                return Storage.file.reset(context, null);
            }
        }
        public static class general {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, STORAGE.PERMANENT, TYPE.GENERAL, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, STORAGE.PERMANENT, TYPE.GENERAL, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, STORAGE.PERMANENT, TYPE.GENERAL, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, STORAGE.PERMANENT, TYPE.GENERAL, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, STORAGE.PERMANENT, TYPE.GENERAL);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.PERMANENT, TYPE.GENERAL, path);
            }
            public static ArrayList<String> list(Context context, String path) {
                return Storage.file.list(context, STORAGE.PERMANENT, TYPE.GENERAL, path);
            }
        }

        private static synchronized boolean put(Context context, STORAGE storage, TYPE type, String path, String data) {
            Log.v(TAG, "file | put | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString() + " | path=" + path);
            if (context == null) {
                Log.w(TAG, "file | put | context is null");
                return false;
            }
            try {
                if (storage == STORAGE.CACHE && !pref.get(context, "pref_use_cache", true)) {
                    return false;
                }
                File file = new File(getFileLocation(context, storage, type, path, true));
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    if (!file.createNewFile()) {
                        throw new Exception("Failed to create file: " + file.getPath());
                    }
                }
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(data);
                fileWriter.close();
                Storage.cache.push(file.getAbsolutePath(), data, 1);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        private static String get(Context context, STORAGE storage, TYPE type, String path) {
            return get(context, storage, type, path, "");
        }
        private static String get(Context context, STORAGE storage, TYPE type, String path, String def) {
            Log.v(TAG, "file | get | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString() + " | path=" + path);
            if (context == null) {
                Log.w(TAG, "file | get | context is null");
                return def;
            }
            try {
                File file = new File(getFileLocation(context, storage, type, path, true));
                path = file.getAbsolutePath();
                if (!file.exists() || file.isDirectory()) {
                    throw new Exception("File does not exist: " + file.getPath());
                }
                Storage.cache.access(path);
                String proxy = Storage.cache.get(path);
                if (proxy != null) return proxy;
                FileReader fileReader = new FileReader(file);
                StringBuilder data = new StringBuilder();
                int c;
                while ((c = fileReader.read()) != -1) data.append((char) c);
                fileReader.close();
                Storage.cache.push(path, data.toString(), 1);
                return data.toString();
            } catch (Exception e) {
                return def;
            }
        }
        private static boolean delete(Context context, STORAGE storage, TYPE type, String path) {
            Log.v(TAG, "file | delete | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString() + " | path=" + path);
            if (context == null) {
                Log.w(TAG, "file | delete | context is null");
                return false;
            }
            try {
                File file = new File(getFileLocation(context, storage, type, path, true));
                path = file.getAbsolutePath();
                Storage.cache.delete(path);
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean clear(Context context, STORAGE storage, TYPE type) {
            Log.v(TAG, "file | clear | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString());
            if (context == null) {
                Log.w(TAG, "file | clear | context is null");
                return false;
            }
            try {
                if (storage == null) {
                    return clear(context, STORAGE.CACHE, type) && clear(context, STORAGE.PERMANENT, type);
                }
                File file = new File(getLocation(context, storage, type));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean clear(Context context, STORAGE storage, String path, TYPE type) {
            Log.v(TAG, "file | clear | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString() + " | path=" + path);
            if (context == null) {
                Log.w(TAG, "file | clear | context is null");
                return false;
            }
            try {
                if (storage == null) {
                    return clear(context, STORAGE.CACHE, path, type) && clear(context, STORAGE.PERMANENT, path, type);
                }
                File file = new File(getFileLocation(context, storage, type, path, false));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean exists(Context context, STORAGE storage, TYPE type, String path) {
            Log.v(TAG, "file | exists | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString() + " | path=" + path);
            if (context == null) {
                Log.w(TAG, "file | exists | context is null");
                return false;
            }
            try {
                File file = new File(getFileLocation(context, storage, type, path, true));
                Storage.cache.access(file.getAbsolutePath());
                return file.exists();
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean reset(Context context, STORAGE storage) {
            Log.v(TAG, "file | reset | storage=" + (storage == null ? "both" : storage.toString()));
            if (context == null) {
                Log.w(TAG, "file | reset | context is null");
                return false;
            }
            try {
                if (storage == null) {
                    return reset(context, STORAGE.CACHE) && reset(context, STORAGE.PERMANENT);
                }
                File file = new File(getCoreLocation(context, storage));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static ArrayList<String> list(Context context, STORAGE storage, TYPE type, String path) {
            Log.v(TAG, "file | list | storage=" + (storage == null ? "both" : storage.toString()) + " | type=" + type.toString() + " | path=" + path);
            ArrayList<String> response = new ArrayList<>();
            if (context == null) {
                Log.w(TAG, "file | list | context is null");
                return response;
            }
            try {
                File file = new File(getFileLocation(context, storage, type, path, false));
                if (file.exists()) {
                    File[] files = file.listFiles();
                    for (File f : files) {
                        response.add(f.getName().replaceAll("\\.txt$", ""));
                    }
                }
            } catch (Exception e) {
                Static.error(e);
            }
            return response;
        }
        private static boolean deleteRecursive(File fileOrDirectory) {
            boolean result = true;
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    if (!deleteRecursive(child)) {
                        result = false;
                    }
                }
            }
            if (!fileOrDirectory.delete()) {
                result = false;
            }
            Storage.cache.delete(fileOrDirectory.getAbsolutePath());
            return result;
        }
        private static String getFileLocation(Context context, STORAGE storage, TYPE type, String path, boolean isFile) throws Exception {
            if (!path.isEmpty()) path = ("#" + path + (isFile ? ".txt" : "")).replace("#", File.separator);
            return getLocation(context, storage, type) + path;
        }
        private static String getLocation(Context context, STORAGE storage, TYPE type) throws Exception {
            String login;
            switch (type) {
                case GENERAL: login = "general"; break;
                case USER: default: login = file.general.get(context, "users#current_login"); break;
            }
            if (login == null || login.isEmpty()) {
                throw new Exception("getLocation | login is empty");
            }
            return getCoreLocation(context, storage) + File.separator + login;
        }
        private static String getCoreLocation(Context context, STORAGE storage) throws Exception {
            if (context == null) {
                Log.w(TAG, "file | getCoreLocation | context is null");
                return "";
            }
            return (storage == STORAGE.CACHE ? context.getCacheDir() : context.getFilesDir()) + File.separator + APP_FOLDER;
        }
    }
    public static class pref {
        public static synchronized void put(Context context, String key, String value) {
            if (context == null) {
                Log.w(TAG, "pref | put | context is null");
                return;
            }
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
        }
        public static synchronized void put(Context context, String key, int value) {
            if (context == null) {
                Log.w(TAG, "pref | put | context is null");
                return;
            }
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
        }
        public static synchronized void put(Context context, String key, boolean value) {
            if (context == null) {
                Log.w(TAG, "pref | put | context is null");
                return;
            }
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
        }
        public static String get(Context context, String key) {
            return pref.get(context, key, "");
        }
        public static String get(Context context, String key, String def) {
            if (context == null) {
                Log.w(TAG, "pref | get | context is null");
                return def;
            }
            return PreferenceManager.getDefaultSharedPreferences(context).getString(key, def);
        }
        public static int get(Context context, String key, int def) {
            if (context == null) {
                Log.w(TAG, "pref | get | context is null");
                return def;
            }
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, def);
        }
        public static boolean get(Context context, String key, boolean def) {
            if (context == null) {
                Log.w(TAG, "pref | get | context is null");
                return def;
            }
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, def);
        }
        public static boolean exists(Context context, String key) {
            if (context == null) {
                Log.w(TAG, "pref | get | context is null");
                return false;
            }
            return PreferenceManager.getDefaultSharedPreferences(context).contains(key);
        }
        public static void delete(Context context, String key) {
            if (context == null) {
                Log.w(TAG, "pref | delete | context is null");
                return;
            }
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.contains(key)) sharedPreferences.edit().remove(key).apply();
        }
        public static void clear(Context context) {
            pref.clear(context, Pattern.compile(".*"));
        }
        public static void clearExceptPref(Context context) {
            pref.clear(context, Pattern.compile("^(?!pref_).*$"));
        }
        public static void clear(Context context, Pattern pattern) {
            if (context == null) {
                Log.w(TAG, "pref | clear | context is null");
                return;
            }
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> list = sharedPreferences.getAll();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (Map.Entry<String, ?> entry : list.entrySet()) {
                if (pattern.matcher(entry.getKey()).find()) {
                    String key = entry.getKey();
                    editor.remove(key);
                }
            }
            editor.apply();
        }
    }

    public static class cache {
        private static long requests = 0;
        private static final int maxStack = 8;
        private static final HashMap<String, ElementMeta> stackOfMeta = new HashMap<>();
        private static final HashMap<String, ElementData> stackOfData = new HashMap<>();
        private static class ElementMeta {
            String path = "";
            double priority = 1;
            long requests = 0;
            double rate = 0;
            ElementMeta(String path, double priority){
                this.path = path;
                this.priority = priority;
            }
        }
        private static class ElementData {
            String path = "";
            String data = "";
            ElementData(String path, String data){
                this.path = path;
                this.data = data;
            }
        }
        private static void push(String path, String data, double priority) {
            if (stackOfMeta.containsKey(path)) {
                stackOfMeta.get(path).priority = priority;
            } else {
                stackOfMeta.put(path, new ElementMeta(path, priority));
            }
            if (stackOfData.containsKey(path)) {
                stackOfData.get(path).data = data;
            } else {
                stackOfData.put(path, new ElementData(path, data));
            }
            check();
        }
        private static void access(String path) {
            if (stackOfMeta.containsKey(path)) {
                requests++;
                stackOfMeta.get(path).requests++;
            }
        }
        private static String get(String path) {
            if (stackOfData.containsKey(path)) {
                return stackOfData.get(path).data;
            } else {
                return null;
            }
        }
        private static void delete(String path) {
            if (stackOfMeta.containsKey(path)) stackOfMeta.remove(path);
            if (stackOfData.containsKey(path)) stackOfData.remove(path);
        }
        private static void check() {
            if (requests + 10 > Long.MAX_VALUE) reset();
            if (stackOfData.size() > maxStack) {
                for (Map.Entry<String, ElementMeta> entry : stackOfMeta.entrySet()) {
                    ElementMeta elementMeta = entry.getValue();
                    elementMeta.rate = ((double) elementMeta.requests / (double) requests) * elementMeta.priority;
                }
                List<ElementMeta> elementMetas = new ArrayList<>(stackOfMeta.values());
                Collections.sort(elementMetas, (s1, s2) -> s1.rate > s2.rate ? -1 : (s1.rate < s2.rate ? 1 : 0));
                for (int i = maxStack; i < elementMetas.size(); i++) {
                    stackOfData.remove(elementMetas.get(i).path);
                }
            }
        }
        public static void reset() {
            requests = 0;
            stackOfMeta.clear();
            stackOfData.clear();
        }
    }
}
