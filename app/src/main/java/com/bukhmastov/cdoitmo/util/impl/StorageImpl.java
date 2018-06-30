package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StorageLocalCache;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StorageImpl implements Storage {

    private static final String TAG = "Storage";
    private static final String APP_FOLDER = "app_data";

    //@Inject
    private StorageLocalCache storageLocalCache = StorageLocalCache.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();
    //@Inject
    private FirebasePerformanceProvider firebasePerformanceProvider = FirebasePerformanceProvider.instance();

    @Override
    public synchronized boolean put(@NonNull Context context, @NonNull String mode, @NonNull String type, @NonNull String path, String data) {
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.PUT);
        try {
            Log.v(TAG, "put | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#", path);
            if (context == null) {
                Log.v(TAG, "put | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return false;
            }
            if (CACHE.equals(mode) && !storagePref.get(context, "pref_use_cache", true)) {
                return false;
            }
            File file = new File(getFileLocation(context, mode, type, path, true));
            if (!file.exists() && !file.getParentFile().mkdirs() && !file.createNewFile()) {
                throw new Exception("Failed to create file: " + file.getPath());
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();
            storageLocalCache.push(file.getAbsolutePath(), data, 1);
            return true;
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return false;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public String get(@NonNull Context context, @NonNull String mode, @NonNull String type, @NonNull String path) {
        return get(context, mode, type, path, "");
    }

    @Override
    public String get(@NonNull Context context, @NonNull String mode, @NonNull String type, @NonNull String path, String def) {
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.GET);
        try {
            Log.v(TAG, "get | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#", path);
            if (context == null) {
                Log.v(TAG, "get | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return def;
            }
            File file = new File(getFileLocation(context, mode, type, path, true));
            path = file.getAbsolutePath();
            if (!file.exists() || file.isDirectory()) {
                throw new Exception("File does not exist: " + file.getPath());
            }
            storageLocalCache.access(path);
            String cache = storageLocalCache.get(path);
            if (cache != null) {
                Log.v(TAG, "get | mode=", mode, " | type=", type, " | path=", path, " | from local cache");
                return cache;
            }
            FileReader fileReader = new FileReader(file);
            StringBuilder data = new StringBuilder();
            int c;
            while ((c = fileReader.read()) != -1) {
                data.append((char) c);
            }
            fileReader.close();
            storageLocalCache.push(path, data.toString(), 1);
            return data.toString();
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return def;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public boolean exists(@NonNull Context context, @NonNull String mode, @NonNull String type, @NonNull String path) {
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.EXISTS);
        try {
            Log.v(TAG, "exists | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#",  path);
            if (context == null) {
                Log.v(TAG, "exists | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return false;
            }
            File file = new File(getFileLocation(context, mode, type, path, true));
            storageLocalCache.access(file.getAbsolutePath());
            return file.exists();
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return false;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public synchronized boolean delete(@NonNull Context context, @NonNull String mode, @NonNull String type, @NonNull String path) {
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.DELETE);
        try {
            Log.v(TAG, "delete | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#", path);
            if (context == null) {
                Log.v(TAG, "delete | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return false;
            }
            File file = new File(getFileLocation(context, mode, type, path, true));
            path = file.getAbsolutePath();
            storageLocalCache.delete(path);
            return file.exists() && deleteRecursive(file);
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return false;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public synchronized boolean clear(@NonNull Context context, @Nullable String mode) {
        if (mode == null) {
            return clear(context, Storage.CACHE) && clear(context, Storage.PERMANENT);
        }
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.CLEAR);
        try {
            Log.v(TAG, "clear | mode=", mode);
            firebasePerformanceProvider.putAttribute(trace, "path", mode);
            if (context == null) {
                Log.v(TAG, "clear | mode=", mode, " | context is null");
                return false;
            }
            File file = new File(getCoreLocation(context, mode));
            return file.exists() && deleteRecursive(file);
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return false;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public synchronized boolean clear(@NonNull Context context, @Nullable String mode, @NonNull String type) {
        if (mode == null) {
            return clear(context, Storage.CACHE, type) && clear(context, Storage.PERMANENT, type);
        }
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.CLEAR);
        try {
            Log.v(TAG, "clear | mode=", mode, " | type=", type);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type);
            if (context == null) {
                Log.v(TAG, "clear | mode=", mode, " | type=", type, " | context is null");
                return false;
            }
            File file = new File(getLocation(context, mode, type));
            return file.exists() && deleteRecursive(file);
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return false;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public synchronized boolean clear(@NonNull Context context, @Nullable String mode, @NonNull String type, @NonNull String path) {
        if (mode == null) {
            return clear(context, Storage.CACHE, type, path) && clear(context, Storage.PERMANENT, type, path);
        }
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.CLEAR);
        try {
            Log.v(TAG, "clear | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#", path);
            if (context == null) {
                Log.v(TAG, "clear | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return false;
            }
            File file = new File(getFileLocation(context, mode, type, path, false));
            return file.exists() && deleteRecursive(file);
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            return false;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    @Override
    public ArrayList<String> list(@NonNull Context context, @NonNull String mode, @NonNull String type, @NonNull String path) {
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.LIST);
        ArrayList<String> response = new ArrayList<>();
        try {
            Log.v(TAG, "list | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#", path);
            if (context == null) {
                Log.v(TAG, "list | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return response;
            }
            File dir = new File(getFileLocation(context, mode, type, path, false));
            if (dir.exists()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    response.add(file.getName().replaceAll("\\.txt$", ""));
                }
            }
        } catch (Exception e) {
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            Log.exception(e);
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
        return response;
    }

    @Override
    public long getDirSize(@NonNull Context context, @NonNull String mode, @NonNull String type,@NonNull  String path) {
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.Storage.SIZE);
        try {
            Log.v(TAG, "size | mode=", mode, " | type=", type, " | path=", path);
            firebasePerformanceProvider.putAttribute(trace, "path", mode, "#", type, "#", path);
            if (context == null) {
                Log.v(TAG, "size | mode=", mode, " | type=", type, " | path=", path, " | context is null");
                return 0L;
            }
            File file = new File(getFileLocation(context, mode, type, path, false));
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
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
        return 0L;
    }

    @Override
    public void cacheReset() {
        storageLocalCache.reset();
    }

    private boolean deleteRecursive(File fileOrDirectory) {
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
        storageLocalCache.delete(fileOrDirectory.getAbsolutePath());
        return result;
    }

    private String getFileLocation(Context context, @Mode String mode, @Type String type, String path, boolean isFile) throws Exception {
        if (!path.isEmpty()) path = ("#" + path + (isFile ? ".txt" : "")).replace("#", File.separator);
        return getLocation(context, mode, type) + path;
    }

    private String getLocation(Context context, @Mode String mode, @Type String type) throws Exception {
        String login;
        switch (type) {
            case GLOBAL: login = "general"; break;
            case USER: default: login = get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login"); break;
        }
        if (login == null || login.isEmpty()) {
            throw new Exception("getLocation | login is empty");
        }
        return getCoreLocation(context, mode) + File.separator + login;
    }

    private String getCoreLocation(Context context, @Mode String mode) {
        if (context == null) {
            Log.w(TAG, "file | getCoreLocation | context is null");
            return "";
        }
        return (CACHE.equals(mode) ? context.getCacheDir() : context.getFilesDir()) + File.separator + APP_FOLDER;
    }
}
