package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class Storage {

    private static final String TAG = "Storage";
    private static final String APP_FOLDER = "app_data";
    private enum STORAGE {cache, permanent}

    public static class file {
        public static class cache {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, STORAGE.cache, false, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, STORAGE.cache, false, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, STORAGE.cache, false, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, STORAGE.cache, false, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, STORAGE.cache, false);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, STORAGE.cache, path, false);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.cache, false, path);
            }
            public static ArrayList<String> list(Context context, String path){
                return Storage.file.list(context, STORAGE.cache, false, path);
            }
        }
        public static class perm {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, STORAGE.permanent, false, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, STORAGE.permanent, false, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, STORAGE.permanent, false, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, STORAGE.permanent, false, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, STORAGE.permanent, false);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, STORAGE.permanent, path, false);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.permanent, false, path);
            }
            public static ArrayList<String> list(Context context, String path){
                return Storage.file.list(context, STORAGE.permanent, false, path);
            }
        }
        public static class all {
            public static boolean clear(Context context) {
                return Storage.file.clear(context, null, false);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, null, path, false);
            }
            public static boolean reset(Context context){
                return Storage.file.reset(context, null);
            }
        }
        public static class general {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, STORAGE.permanent, true, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, STORAGE.permanent, true, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, STORAGE.permanent, true, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, STORAGE.permanent, true, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, STORAGE.permanent, true);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.permanent, true, path);
            }
            public static ArrayList<String> list(Context context, String path){
                return Storage.file.list(context, STORAGE.permanent, true, path);
            }
        }

        private static synchronized boolean put(Context context, STORAGE storage, boolean general, String path, String data){
            Log.v(TAG, "file | put | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false") + " | path=" + path);
            try {
                if (storage == STORAGE.cache && !pref.get(context, "pref_use_cache", true)) {
                    return false;
                }
                File file = new File(getFileLocation(context, storage, general, path, true));
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    if (!file.createNewFile()) {
                        throw new Exception("Failed to create file: " + file.getPath());
                    }
                }
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(data);
                fileWriter.close();
                Storage.proxy.push(file.getAbsolutePath(), data, 1);
                if (storage == STORAGE.permanent && Objects.equals(path, "user#jsessionid")) {
                    Storage.file.perm.put(context, "user#jsessionid_ts", String.valueOf(System.currentTimeMillis()));
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        private static String get(Context context, STORAGE storage, boolean general, String path){
            return get(context, storage, general, path, "");
        }
        private static String get(Context context, STORAGE storage, boolean general, String path, String def){
            Log.v(TAG, "file | get | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false") + " | path=" + path);
            try {
                File file = new File(getFileLocation(context, storage, general, path, true));
                path = file.getAbsolutePath();
                if (!file.exists() || file.isDirectory()) {
                    throw new Exception("File does not exist: " + file.getPath());
                }
                Storage.proxy.access(path);
                String proxy = Storage.proxy.get(path);
                if (proxy != null) return proxy;
                FileReader fileReader = new FileReader(file);
                StringBuilder data = new StringBuilder();
                int c;
                while ((c = fileReader.read()) != -1) data.append((char) c);
                fileReader.close();
                Storage.proxy.push(path, data.toString(), 1);
                return data.toString();
            } catch (Exception e) {
                return def;
            }
        }
        private static boolean delete(Context context, STORAGE storage, boolean general, String path){
            Log.v(TAG, "file | delete | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false") + " | path=" + path);
            try {
                File file = new File(getFileLocation(context, storage, general, path, true));
                path = file.getAbsolutePath();
                Storage.proxy.delete(path);
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean clear(Context context, STORAGE storage, boolean general){
            Log.v(TAG, "file | clear | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false"));
            try {
                if (storage == null) {
                    return clear(context, STORAGE.cache, general) && clear(context, STORAGE.permanent, general);
                }
                File file = new File(getLocation(context, storage, general));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean clear(Context context, STORAGE storage, String path, boolean general){
            Log.v(TAG, "file | clear | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false") + " | path=" + path);
            try {
                if (storage == null) {
                    return clear(context, STORAGE.cache, path, general) && clear(context, STORAGE.permanent, path, general);
                }
                File file = new File(getFileLocation(context, storage, general, path, false));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean exists(Context context, STORAGE storage, boolean general, String path){
            Log.v(TAG, "file | exists | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false") + " | path=" + path);
            try {
                File file = new File(getFileLocation(context, storage, general, path, true));
                Storage.proxy.access(file.getAbsolutePath());
                return file.exists();
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean reset(Context context, STORAGE storage){
            Log.v(TAG, "file | reset | storage=" + (storage == null ? "both" : storage.toString()));
            try {
                if (storage == null) {
                    return reset(context, STORAGE.cache) && reset(context, STORAGE.permanent);
                }
                File file = new File(getCoreLocation(context, storage));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static ArrayList<String> list(Context context, STORAGE storage, boolean general, String path){
            Log.v(TAG, "file | list | storage=" + (storage == null ? "both" : storage.toString()) + " | general=" + (general ? "true" : "false") + " | path=" + path);
            ArrayList<String> response = new ArrayList<>();
            try {
                File file = new File(getFileLocation(context, storage, general, path, false));
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
            Storage.proxy.delete(fileOrDirectory.getAbsolutePath());
            return result;
        }
        private static String getFileLocation(Context context, STORAGE storage, boolean general, String path, boolean isFile) throws Exception {
            if (!path.isEmpty()) path = ("#" + path + (isFile ? ".txt" : "")).replace("#", File.separator);
            return getLocation(context, storage, general) + path;
        }
        private static String getLocation(Context context, STORAGE storage, boolean general) throws Exception {
            String current_login = general ? "general" : file.general.get(context, "users#current_login");
            if (current_login.isEmpty()) throw new Exception("current_login is empty");
            return getCoreLocation(context, storage) + File.separator + current_login;
        }
        private static String getCoreLocation(Context context, STORAGE storage) throws Exception {
            return (storage == STORAGE.cache ? context.getCacheDir() : context.getFilesDir()) + File.separator + APP_FOLDER;
        }
    }
    public static class pref {
        public static synchronized void put(Context context, String key, String value){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
        }
        public static synchronized void put(Context context, String key, int value){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
        }
        public static synchronized void put(Context context, String key, boolean value){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
        }
        public static String get(Context context, String key){
            return pref.get(context, key, "");
        }
        public static String get(Context context, String key, String def){
            return PreferenceManager.getDefaultSharedPreferences(context).getString(key, def);
        }
        public static int get(Context context, String key, int def){
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, def);
        }
        public static boolean get(Context context, String key, boolean def){
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, def);
        }
        public static void delete(Context context, String key){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.contains(key)) sharedPreferences.edit().remove(key).apply();
        }
        public static void clear(Context context){
            pref.clear(context, Pattern.compile(".*"));
        }
        public static void clearExceptPref(Context context){
            pref.clear(context, Pattern.compile("^(?!pref_).*$"));
        }
        public static void clear(Context context, Pattern pattern){
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

    private static class proxy {
        private static long requests = 0;
        private static int maxStack = 8;
        private static HashMap<String, ElementMeta> stackOfMeta = new HashMap<>();
        private static HashMap<String, ElementData> stackOfData = new HashMap<>();
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
        private static void push(String path, String data, double priority){
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
        private static void access(String path){
            if (stackOfMeta.containsKey(path)) {
                requests++;
                stackOfMeta.get(path).requests++;
            }
        }
        private static String get(String path){
            if (stackOfData.containsKey(path)) {
                return stackOfData.get(path).data;
            } else {
                return null;
            }
        }
        private static void delete(String path){
            if (stackOfMeta.containsKey(path)) stackOfMeta.remove(path);
            if (stackOfData.containsKey(path)) stackOfData.remove(path);
        }
        private static void check(){
            if (requests + 10 > Long.MAX_VALUE) reset();
            if (stackOfData.size() > maxStack) {
                for (Map.Entry<String, ElementMeta> entry : stackOfMeta.entrySet()) {
                    ElementMeta elementMeta = entry.getValue();
                    elementMeta.rate = ((double) elementMeta.requests / (double) requests) * elementMeta.priority;
                }
                List<ElementMeta> elementMetas = new ArrayList<>(stackOfMeta.values());
                Collections.sort(elementMetas, new Comparator<ElementMeta>() {
                    @Override
                    public int compare(ElementMeta s1, ElementMeta s2) {
                        return s1.rate > s2.rate ? -1 : (s1.rate < s2.rate ? 1 : 0);
                    }
                });
                for (int i = maxStack; i < elementMetas.size(); i++) {
                    stackOfData.remove(elementMetas.get(i).path);
                }
            }
        }
        static void reset(){
            requests = 0;
            stackOfMeta.clear();
            stackOfData.clear();
        }
    }

}