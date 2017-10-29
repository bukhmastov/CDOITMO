package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;

import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class Wipe {

    private static final String TAG = "Wipe";
    private static boolean first_launch = false;

    public static void check(Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int lastVersionCode = Storage.pref.get(context, "last_version", 0);
            first_launch = lastVersionCode == 0;
            if (lastVersionCode < versionCode) {
                for (int i = lastVersionCode + 1; i <= versionCode; i++) {
                    apply(context, i);
                }
                Storage.pref.put(context, "last_version", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Static.error(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void apply(final Context context, final int versionCode) {
        Log.i(TAG, "Wipe apply for versionCode " + versionCode);
        switch (versionCode) {
            case 26: {
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    jobScheduler.cancelAll();
                }
                Static.logout(context);
                Storage.pref.clearExceptPref(context);
                break;
            }
            case 29: {
                Storage.file.cache.delete(context, "eregister#core");
                break;
            }
            case 51: {
                Storage.file.cache.clear(context, "protocol#log");
                if (Storage.pref.get(context, "pref_protocol_changes_track", true)) {
                    Static.protocolChangesTrackSetup(context, 0);
                }
                break;
            }
            case 62: {
                Storage.pref.put(context, "pref_dynamic_refresh", Storage.pref.get(context, "pref_tab_refresh", "0"));
                Storage.pref.put(context, "pref_static_refresh", Storage.pref.get(context, "pref_schedule_refresh", "168"));
                Storage.pref.delete(context, "pref_tab_refresh");
                Storage.pref.delete(context, "pref_schedule_refresh");
                break;
            }
            case 71: {
                Storage.pref.delete(context, "pref_open_drawer_at_startup");
                Storage.pref.put(context, "pref_first_launch", Storage.file.general.get(context, "users#list", "").trim().isEmpty());
                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                    @Override
                    public void run() {
                        new ProtocolTracker(context).reset();
                    }
                });
                break;
            }
            case 78: {
                ArrayList<String> appWidgetIds = Storage.file.general.list(context, "widget_schedule_lessons");
                for (String appWidgetId : appWidgetIds) {
                    String settings = Storage.file.general.get(context, "widget_schedule_lessons#" + appWidgetId + "#settings", "");
                    boolean empty_settings = false;
                    if (settings != null) {
                        settings = settings.trim();
                        if (!settings.isEmpty()) {
                            try {
                                JSONObject settingsJson = new JSONObject(settings);
                                if (settingsJson.has("darkTheme")) {
                                    boolean darkTheme = settingsJson.getBoolean("darkTheme");
                                    JSONObject theme = new JSONObject();
                                    if (darkTheme) {
                                        theme.put("background", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.background);
                                        theme.put("text", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.text);
                                        theme.put("opacity", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.opacity);
                                    } else {
                                        theme.put("background", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Light.background);
                                        theme.put("text", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Light.text);
                                        theme.put("opacity", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Light.opacity);
                                    }
                                    settingsJson.remove("darkTheme");
                                    settingsJson.put("theme", theme);
                                    Storage.file.general.put(context, "widget_schedule_lessons#" + appWidgetId + "#settings", settingsJson.toString());
                                }
                            } catch (Exception e) {
                                Static.error(e);
                                Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#settings");
                            }
                        } else {
                            empty_settings = true;
                        }
                    } else {
                        empty_settings = true;
                    }
                    if (empty_settings) {
                        Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#settings");
                        Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#cache");
                        Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#cache_converted");
                    }
                }
                break;
            }
            case 83: {
                ArrayList<String> appWidgetIds = Storage.file.general.list(context, "widget_schedule_lessons");
                for (String appWidgetId : appWidgetIds) {
                    String settings = Storage.file.general.get(context, "widget_schedule_lessons#" + appWidgetId + "#settings", "");
                    if (settings != null) {
                        settings = settings.trim();
                        if (!settings.isEmpty()) {
                            try {
                                JSONObject settingsJson = new JSONObject(settings);
                                settingsJson.put("shift", 0);
                                Storage.file.general.put(context, "widget_schedule_lessons#" + appWidgetId + "#settings", settingsJson.toString());
                            } catch (Exception e) {
                                Static.error(e);
                                Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#settings");
                            }
                        }
                    }
                }
                break;
            }
            case 90: {
                boolean compact = Storage.pref.get(context, "pref_schedule_lessons_compact_view_of_reduced_lesson", true);
                Storage.pref.delete(context, "pref_schedule_lessons_compact_view_of_reduced_lesson");
                Storage.pref.put(context, "pref_schedule_lessons_view_of_reduced_lesson", compact ? "compact" : "full");
                break;
            }
            case 93: {
                // new theme
                final boolean dark_theme = Storage.pref.get(context, "pref_dark_theme", false);
                Storage.pref.delete(context, "pref_dark_theme");
                Storage.pref.put(context, "pref_theme", dark_theme ? "dark" : "light");
                // get rid of pref_allow_owner_notifications
                Storage.pref.delete(context, "pref_allow_owner_notifications");
                // move files
                Static.logoutCurrent(context);
                try {
                    String path = context.getFilesDir() + File.separator + "app_data";
                    File file = new File(path);
                    if (file.exists()) {
                        File[] files = file.listFiles();
                        for (File f : files) {
                            try {
                                String userPath = f.getPath();
                                if (userPath.endsWith("general")) continue;
                                String login = "";
                                String password = "";
                                File ff;
                                ff = new File(userPath + File.separator + "user" + File.separator + "login.txt");
                                if (ff.exists()) {
                                    try {
                                        FileReader fileReader = new FileReader(ff);
                                        StringBuilder data = new StringBuilder();
                                        int c;
                                        while ((c = fileReader.read()) != -1)
                                            data.append((char) c);
                                        fileReader.close();
                                        login = data.toString();
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                    ff.delete();
                                }
                                ff = new File(userPath + File.separator + "user" + File.separator + "password.txt");
                                if (ff.exists()) {
                                    try {
                                        FileReader fileReader = new FileReader(ff);
                                        StringBuilder data = new StringBuilder();
                                        int c;
                                        while ((c = fileReader.read()) != -1)
                                            data.append((char) c);
                                        fileReader.close();
                                        password = data.toString();
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                    ff.delete();
                                }
                                ff = new File(userPath + File.separator + "user" + File.separator + "jsessionid.txt");
                                if (ff.exists()) {
                                    ff.delete();
                                }
                                ff = new File(userPath + File.separator + "user" + File.separator + "jsessionid_ts.txt");
                                if (ff.exists()) {
                                    ff.delete();
                                }
                                ff = new File(userPath + File.separator + "user" + File.separator + "phpsessid.txt");
                                if (ff.exists()) {
                                    ff.delete();
                                }
                                ff = new File(userPath + File.separator + "user" + File.separator + "deifmo" + File.separator + "login.txt");
                                if (!ff.exists()) {
                                    try {
                                        ff.getParentFile().mkdirs();
                                        ff.createNewFile();
                                        FileWriter fileWriter = new FileWriter(ff);
                                        fileWriter.write(login);
                                        fileWriter.close();
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                }
                                ff = new File(userPath + File.separator + "user" + File.separator + "deifmo" + File.separator + "password.txt");
                                if (!ff.exists()) {
                                    try {
                                        ff.getParentFile().mkdirs();
                                        ff.createNewFile();
                                        FileWriter fileWriter = new FileWriter(ff);
                                        fileWriter.write(password);
                                        fileWriter.close();
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                }
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // ignore
                }
                break;
            }
            case 97: {
                if (!first_launch) {
                    Storage.pref.put(context, "pref_default_values_applied", true);
                }
                break;
            }
        }
    }
}
