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
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.cache, false, path);
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
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, STORAGE.permanent, false, path);
            }
        }
        public static class all {
            public static boolean clear(Context context) {
                return Storage.file.clear(context, false);
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
        }

        private static synchronized boolean put(Context context, STORAGE storage, boolean general, String path, String data){
            try {
                if (storage == STORAGE.cache && !pref.get(context, "pref_use_cache", true)) {
                    return false;
                }
                File file = new File(getFileLocation(context, storage, general, path));
                path = file.getAbsolutePath();
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    if (!file.createNewFile()) {
                        throw new Exception("Failed to create file: " + file.getPath());
                    }
                }
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(data);
                fileWriter.close();
                Storage.proxy.push(path, data, 1);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        private static String get(Context context, STORAGE storage, boolean general, String path){
            return get(context, storage, general, path, "");
        }
        private static String get(Context context, STORAGE storage, boolean general, String path, String def){
            try {
                File file = new File(getFileLocation(context, storage, general, path));
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
            try {
                File file = new File(getFileLocation(context, storage, general, path));
                path = file.getAbsolutePath();
                Storage.proxy.delete(path);
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                return false;
            }
        }
        private static boolean clear(Context context, boolean general){
            return clear(context, null, general);
        }
        private static boolean clear(Context context, STORAGE storage, boolean general){
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
        private static boolean exists(Context context, STORAGE storage, boolean general, String path){
            try {
                File file = new File(getFileLocation(context, storage, general, path));
                Storage.proxy.access(file.getAbsolutePath());
                return file.exists();
            } catch (Exception e) {
                return false;
            }
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
        private static String getFileLocation(Context context, STORAGE storage, boolean general, String path) throws Exception {
            if (!path.isEmpty()) path = ("#" + path + ".txt").replace("#", File.separator);
            return getLocation(context, storage, general) + path;
        }
        private static String getLocation(Context context, STORAGE storage, boolean general) throws Exception {
            String current_login = general ? "general" : file.general.get(context, "users#current_login");
            if (current_login.isEmpty()) throw new Exception("current_login is empty");
            return (storage == STORAGE.cache ? context.getCacheDir() : context.getFilesDir()) + File.separator + APP_FOLDER + File.separator + current_login;
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
        private static ArrayList<element> stack = new ArrayList<>();
        private static class element {
            public String path = "";
            public String data = "";
            public double priority = 1;
            public long requests = 0;
            public double rate = 0;
            public element(String path, double priority, String data){
                this.path = path;
                this.data = data;
                this.priority = priority;
            }
        }
        private static void push(String path, String data, double priority){
            boolean found = false;
            for (element item : stack) {
                if (Objects.equals(item.path, path)) {
                    found = true;
                    item.data = data;
                    item.priority = priority;
                    break;
                }
            }
            if (!found) {
                stack.add(new element(path, priority, data));
            }
            empty();
        }
        private static void access(String path){
            for (element item : stack) {
                if (Objects.equals(item.path, path)) {
                    requests++;
                    item.requests++;
                    break;
                }
            }
        }
        private static String get(String path){
            for (element item : stack) {
                if (Objects.equals(item.path, path)) return item.data;
            }
            return null;
        }
        private static void delete(String path){
            for (int i = 0; i < stack.size(); i++) {
                element item = stack.get(i);
                if (Objects.equals(item.path, path)) {
                    stack.remove(i);
                    break;
                }
            }
        }
        private static void empty(){
            if (requests + 10 > Long.MAX_VALUE) reset();
            if (stack.size() > maxStack) {
                for (element item : stack) {
                    item.rate = ((double) item.requests / (double) requests) * item.priority;
                }
                Collections.sort(stack, new Comparator<element>() {
                    @Override
                    public int compare(element s1, element s2) {
                        return s1.rate > s2.rate ? -1 : (s1.rate < s2.rate ? 1 : 0);
                    }
                });
                for (int i = maxStack; i < stack.size(); i++) stack.remove(i);
            }
        }
        static void reset(){
            requests = 0;
            stack.clear();
        }
    }

}