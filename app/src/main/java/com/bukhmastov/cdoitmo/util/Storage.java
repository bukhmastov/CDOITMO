package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.bukhmastov.cdoitmo.data.StorageProxy;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Storage {

    private static final String TAG = "Storage";
    private static final String APP_FOLDER = "app_data";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({CACHE, PERMANENT})
    public @interface STORAGE {}
    public static final String CACHE = "cache";
    public static final String PERMANENT = "permanent";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({USER, GENERAL})
    public @interface TYPE {}
    public static final String USER = "user";
    public static final String GENERAL = "general";

    public enum StorageType {
        PER_USER,
        GLOBAL,
        PREFS
    }

    public static class Proxy implements StorageProxy {
        private Context context;

        public Proxy(Context context) { this.context = context; }

        @NonNull
        @Override
        public String get(StorageType type, String path) {
            switch (type) {
                case PER_USER: return file.perm.get(context, path, "");
                case GLOBAL: return file.general.perm.get(context, path, "");
                default: return pref.get(context, path, "");
            }
        }

        @Override
        public boolean put(StorageType type, String path, String data) {
            switch (type) {
                case PER_USER: return file.perm.put(context, path, data);
                case GLOBAL: return file.general.perm.put(context, path, data);
                default: pref.put(context, path, data); return true;
            }
        }

        @Override
        public boolean delete(StorageType type, String path) {
            switch (type) {
                case PER_USER: return file.perm.delete(context, path);
                case GLOBAL: return file.general.perm.delete(context, path);
                default: pref.delete(context, path); return true;
            }
        }
    }

    public static class file {
        public static class cache {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, CACHE, USER, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, CACHE, USER, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, CACHE, USER, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, CACHE, USER, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, CACHE, USER);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, CACHE, USER, path);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, CACHE, USER, path);
            }
            public static ArrayList<String> list(Context context, String path) {
                return Storage.file.list(context, CACHE, USER, path);
            }
            public static long getDirSize(Context context, String path) {
                return Storage.file.getDirSize(context, CACHE, USER, path);
            }
        }
        public static class perm {
            public static boolean put(Context context, String path, String data) {
                return Storage.file.put(context, PERMANENT, USER, path, data);
            }
            public static String get(Context context, String path) {
                return Storage.file.get(context, PERMANENT, USER, path);
            }
            public static String get(Context context, String path, String def) {
                return Storage.file.get(context, PERMANENT, USER, path, def);
            }
            public static boolean delete(Context context, String path) {
                return Storage.file.delete(context, PERMANENT, USER, path);
            }
            public static boolean clear(Context context) {
                return Storage.file.clear(context, PERMANENT, USER);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, PERMANENT, USER, path);
            }
            public static boolean exists(Context context, String path) {
                return Storage.file.exists(context, PERMANENT, USER, path);
            }
            public static ArrayList<String> list(Context context, String path) {
                return Storage.file.list(context, PERMANENT, USER, path);
            }
        }
        public static class general {
            public static class cache {
                public static boolean put(Context context, String path, String data) {
                    return Storage.file.put(context, CACHE, GENERAL, path, data);
                }
                public static String get(Context context, String path) {
                    return Storage.file.get(context, CACHE, GENERAL, path);
                }
                public static String get(Context context, String path, String def) {
                    return Storage.file.get(context, CACHE, GENERAL, path, def);
                }
                public static boolean delete(Context context, String path) {
                    return Storage.file.delete(context, CACHE, GENERAL, path);
                }
                public static boolean clear(Context context) {
                    return Storage.file.clear(context, CACHE, GENERAL);
                }
                public static boolean clear(Context context, String path) {
                    return Storage.file.clear(context, CACHE, GENERAL, path);
                }
                public static boolean exists(Context context, String path) {
                    return Storage.file.exists(context, CACHE, GENERAL, path);
                }
                public static ArrayList<String> list(Context context, String path) {
                    return Storage.file.list(context, CACHE, GENERAL, path);
                }
                public static long getDirSize(Context context, String path) {
                    return Storage.file.getDirSize(context, CACHE, GENERAL, path);
                }
            }
            public static class perm {
                public static boolean put(Context context, String path, String data) {
                    return Storage.file.put(context, PERMANENT, GENERAL, path, data);
                }
                public static String get(Context context, String path) {
                    return Storage.file.get(context, PERMANENT, GENERAL, path);
                }
                public static String get(Context context, String path, String def) {
                    return Storage.file.get(context, PERMANENT, GENERAL, path, def);
                }
                public static boolean delete(Context context, String path) {
                    return Storage.file.delete(context, PERMANENT, GENERAL, path);
                }
                public static boolean clear(Context context) {
                    return Storage.file.clear(context, PERMANENT, GENERAL);
                }
                public static boolean clear(Context context, String path) {
                    return Storage.file.clear(context, PERMANENT, GENERAL, path);
                }
                public static boolean exists(Context context, String path) {
                    return Storage.file.exists(context, PERMANENT, GENERAL, path);
                }
                public static ArrayList<String> list(Context context, String path) {
                    return Storage.file.list(context, PERMANENT, GENERAL, path);
                }
            }
        }
        public static class all {
            public static boolean clear(Context context) {
                return Storage.file.clear(context, null, USER);
            }
            public static boolean clear(Context context, String path) {
                return Storage.file.clear(context, null, USER, path);
            }
            public static boolean reset(Context context) {
                return Storage.file.reset(context, null);
            }
        }

        private static synchronized boolean put(Context context, @STORAGE String storage, @TYPE String type, String path, String data) {
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.PUT);
            try {
                Log.v(TAG, "file | put | storage=", storage, " | type=", type, " | path=", path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#", path);
                if (context == null) {
                    Log.w(TAG, "file | put | context is null");
                    return false;
                }
                if (storage.equals(CACHE) && !pref.get(context, "pref_use_cache", true)) {
                    return false;
                }
                File file = new File(getFileLocation(context, storage, type, path, true));
                if (!file.exists() && !file.getParentFile().mkdirs() && !file.createNewFile()) {
                    throw new Exception("Failed to create file: " + file.getPath());
                }
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(data);
                fileWriter.close();
                Storage.cache.push(file.getAbsolutePath(), data, 1);
                return true;
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return false;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static String get(Context context, @STORAGE String storage, @TYPE String type, String path) {
            return get(context, storage, type, path, "");
        }
        private static String get(Context context, @STORAGE String storage, @TYPE String type, String path, String def) {
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.GET);
            try {
                Log.v(TAG, "file | get | storage=", storage, " | type=", type, " | path=", path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#", path);
                if (context == null) {
                    Log.w(TAG, "file | get | context is null");
                    return def;
                }
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
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return def;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static boolean delete(Context context, @STORAGE String storage, @TYPE String type, String path) {
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.DELETE);
            try {
                Log.v(TAG, "file | delete | storage=", storage, " | type=", type, " | path=", path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#", path);
                if (context == null) {
                    Log.w(TAG, "file | delete | context is null");
                    return false;
                }
                File file = new File(getFileLocation(context, storage, type, path, true));
                path = file.getAbsolutePath();
                Storage.cache.delete(path);
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return false;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static boolean clear(Context context, @STORAGE String storage, @TYPE String type) {
            if (storage == null) {
                return clear(context, CACHE, type) && clear(context, PERMANENT, type);
            }
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.CLEAR);
            try {
                Log.v(TAG, "file | clear | storage=", storage, " | type=", type);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type);
                if (context == null) {
                    Log.w(TAG, "file | clear | context is null");
                    return false;
                }
                File file = new File(getLocation(context, storage, type));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return false;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static boolean clear(Context context, @STORAGE String storage, @TYPE String type, String path) {
            if (storage == null) {
                return clear(context, CACHE, type, path) && clear(context, PERMANENT, type, path);
            }
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.CLEAR);
            try {
                Log.v(TAG, "file | clear | storage=", storage, " | type=", type, " | path=", path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#", path);
                if (context == null) {
                    Log.w(TAG, "file | clear | context is null");
                    return false;
                }
                File file = new File(getFileLocation(context, storage, type, path, false));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return false;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static boolean exists(Context context, @STORAGE String storage, @TYPE String type, String path) {
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.EXISTS);
            try {
                Log.v(TAG, "file | exists | storage=", storage, " | type=", type, " | path=", path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#",  path);
                if (context == null) {
                    Log.w(TAG, "file | exists | context is null");
                    return false;
                }
                File file = new File(getFileLocation(context, storage, type, path, true));
                Storage.cache.access(file.getAbsolutePath());
                return file.exists();
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return false;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static boolean reset(Context context, @STORAGE String storage) {
            if (storage == null) {
                return reset(context, CACHE) && reset(context, PERMANENT);
            }
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.RESET);
            try {
                Log.v(TAG, "file | reset | storage=" + storage);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage);
                if (context == null) {
                    Log.w(TAG, "file | reset | context is null");
                    return false;
                }
                File file = new File(getCoreLocation(context, storage));
                return file.exists() && deleteRecursive(file);
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                return false;
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
        }
        private static ArrayList<String> list(Context context, @STORAGE String storage, @TYPE String type, String path) {
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.LIST);
            ArrayList<String> response = new ArrayList<>();
            try {
                Log.v(TAG, "file | list | storage=" + storage + " | type=" + type + " | path=" + path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#", path);
                if (context == null) {
                    Log.w(TAG, "file | list | context is null");
                    return response;
                }
                File file = new File(getFileLocation(context, storage, type, path, false));
                if (file.exists()) {
                    File[] files = file.listFiles();
                    for (File f : files) {
                        response.add(f.getName().replaceAll("\\.txt$", ""));
                    }
                }
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
                Log.exception(e);
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
            return response;
        }
        private static long getDirSize(Context context, @STORAGE String storage, @TYPE String type, String path) {
            String trace = FirebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.SIZE);
            try {
                Log.v(TAG, "file | size | storage=", storage, " | type=", type, " | path=", path);
                FirebasePerformanceProvider.putAttribute(trace, "path", storage, "#", type, "#", path);
                if (context == null) {
                    Log.w(TAG, "file | size | context is null");
                    return 0L;
                }
                File file = new File(getFileLocation(context, storage, type, path, false));
                if (!file.exists() || !file.isDirectory()) {
                    throw new Exception("Dir does not exist: " + file.getPath());
                }
                // calculate dir tree size
                final List<File> dirs = new LinkedList<>();
                dirs.add(file);
                long size = 0; // bytes
                while (!dirs.isEmpty()) {
                    final File dir = dirs.remove(0);
                    if (!dir.exists()) {
                        continue;
                    }
                    final File[] subFiles = dir.listFiles();
                    if (subFiles == null || subFiles.length == 0) {
                        continue;
                    }
                    for (final File subFile : subFiles) {
                        size += subFile.length();
                        if (subFile.isDirectory()) {
                            dirs.add(subFile);
                        }
                    }
                }
                return size;
            } catch (Exception e) {
                FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            } finally {
                FirebasePerformanceProvider.stopTrace(trace);
            }
            return 0L;
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
        private static String getFileLocation(Context context, @STORAGE String storage, @TYPE String type, String path, boolean isFile) throws Exception {
            if (!path.isEmpty()) path = ("#" + path + (isFile ? ".txt" : "")).replace("#", File.separator);
            return getLocation(context, storage, type) + path;
        }
        private static String getLocation(Context context, @STORAGE String storage, @TYPE String type) throws Exception {
            String login;
            switch (type) {
                case GENERAL: login = "general"; break;
                case USER: default: login = general.perm.get(context, "users#current_login"); break;
            }
            if (login == null || login.isEmpty()) {
                throw new Exception("getLocation | login is empty");
            }
            return getCoreLocation(context, storage) + File.separator + login;
        }
        private static String getCoreLocation(Context context, @STORAGE String storage) {
            if (context == null) {
                Log.w(TAG, "file | getCoreLocation | context is null");
                return "";
            }
            return (storage.equals(CACHE) ? context.getCacheDir() : context.getFilesDir()) + File.separator + APP_FOLDER;
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
                Collections.sort(elementMetas, (s1, s2) -> Double.compare(s2.rate, s1.rate));
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
