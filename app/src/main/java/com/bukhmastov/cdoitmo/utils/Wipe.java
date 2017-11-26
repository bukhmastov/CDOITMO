package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;

import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Wipe {

    private static final String TAG = "Wipe";
    private static boolean first_launch = false;

    public static void check(Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int lastVersionCode = Storage.pref.get(context, "last_version", 0);
            first_launch = lastVersionCode == 0;
            if (lastVersionCode < versionCode) {
                // Skip wipe for first launch. Users with version <28 become deprecated and unsupported (actually, its 10 users at the beginning of december 2017).
                if (!first_launch) {
                    for (int i = lastVersionCode + 1; i <= versionCode; i++) {
                        apply(context, i);
                    }
                }
                Storage.pref.put(context, "last_version", versionCode);
            }
            // TODO remove it
            apply97(context);
        } catch (PackageManager.NameNotFoundException e) {
            Static.error(e);
        }
    }

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
                apply97(context);
                break;
            }
        }
    }

    // version 97
    private static void apply97(final Context context) {
        // Backwards compatibility | upgrade to version 2.0
        // convert cache
        try {
            String cacheRootPath = context.getCacheDir() + File.separator + "app_data";
            File cacheRoot = new File(cacheRootPath);
            if (cacheRoot.exists()) {
                File[] users = cacheRoot.listFiles();
                for (File user : users) {
                    try {
                        if (!user.isDirectory()) continue;
                        String userLogin = user.getName();
                        if (userLogin.equals("general")) continue;
                        // convert lessons cache
                        try {
                            String lessonsPath = cacheRootPath + File.separator + userLogin + File.separator + "schedule_lessons" + File.separator + "lessons";
                            File schedule_lessons = new File(lessonsPath);
                            if (schedule_lessons.exists()) {
                                File[] lessons = schedule_lessons.listFiles();
                                for (File lesson : lessons) {
                                    try {
                                        String token = lesson.getName();
                                        if (token.startsWith("group_")) {
                                            apply97convertLesson(lesson, lessonsPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            apply97convertLesson(lesson, lessonsPath, token, "room_");
                                        } else if (token.matches("^\\d{6}\\.txt$")) {
                                            apply97convertLesson(lesson, lessonsPath, token, "");
                                        }
                                        // "teacher_picker_" needs to be removed without converting
                                        lesson.delete();
                                    } catch (Exception e) {
                                        try {
                                            lesson.delete();
                                        } catch (Exception ignore) {/* ignore */}
                                    }
                                }
                            }
                        } catch (Exception ignore) {/* ignore */}
                        // convert exams cache
                        try {
                            String examsPath = cacheRootPath + File.separator + userLogin + File.separator + "schedule_exams" + File.separator + "lessons";
                            File schedule_exams = new File(examsPath);
                            if (schedule_exams.exists()) {
                                File[] exams = schedule_exams.listFiles();
                                for (File exam : exams) {
                                    try {
                                        String token = exam.getName();
                                        if (token.startsWith("group_")) {
                                            apply97convertExam(exam, examsPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            apply97convertExam(exam, examsPath, token, "room_");
                                        } else if (token.startsWith("teacher")) {
                                            apply97convertExam(exam, examsPath, token, "teacher");
                                        }
                                        // "teacher_picker_" needs to be removed without converting
                                        exam.delete();
                                    } catch (Exception e) {
                                        try {
                                            exam.delete();
                                        } catch (Exception ignore) {/* ignore */}
                                    }
                                }
                            }
                        } catch (Exception ignore) {/* ignore */}
                    } catch (Exception ignore) {/* ignore */}
                }
            }
        } catch (Exception ignore) {/* ignore */}
        // convert permanent files
        try {
            String filesRootPath = context.getFilesDir() + File.separator + "app_data";
            File filesRoot = new File(filesRootPath);
            if (filesRoot.exists()) {
                File[] users = filesRoot.listFiles();
                for (File user : users) {
                    try {
                        if (!user.isDirectory()) continue;
                        String userLogin = user.getName();
                        if (userLogin.equals("general")) continue;
                        // convert added lessons
                        try {
                            String addedPath = filesRootPath + File.separator + userLogin + File.separator + "schedule_lessons" + File.separator + "added";
                            File schedule_added = new File(addedPath);
                            if (schedule_added.exists()) {
                                File[] addedList = schedule_added.listFiles();
                                for (File added : addedList) {
                                    try {
                                        String token = added.getName();
                                        if (token.startsWith("group_")) {
                                            apply97convertAdded(added, addedPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            apply97convertAdded(added, addedPath, token, "room_");
                                        } else if (token.matches("^\\d{6}\\.txt$")) {
                                            apply97convertLesson(added, addedPath, token, "");
                                        }
                                        added.delete();
                                    } catch (Exception e) {
                                        try {
                                            added.delete();
                                        } catch (Exception ignore) {/* ignore */}
                                    }
                                }
                            }
                        } catch (Exception ignore) {/* ignore */}
                        // convert reduced lessons
                        try {
                            String reducedPath = filesRootPath + File.separator + userLogin + File.separator + "schedule_lessons" + File.separator + "reduced";
                            File schedule_reduced = new File(reducedPath);
                            if (schedule_reduced.exists()) {
                                File[] reducedList = schedule_reduced.listFiles();
                                for (File reduced : reducedList) {
                                    try {
                                        String token = reduced.getName();
                                        if (token.startsWith("group_")) {
                                            apply97convertReduced(reduced, reducedPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            apply97convertReduced(reduced, reducedPath, token, "room_");
                                        } else if (token.matches("^\\d{6}\\.txt$")) {
                                            apply97convertReduced(reduced, reducedPath, token, "");
                                        }
                                        reduced.delete();
                                    } catch (Exception e) {
                                        try {
                                            reduced.delete();
                                        } catch (Exception ignore) {/* ignore */}
                                    }
                                }
                            }
                        } catch (Exception ignore) {/* ignore */}
                    } catch (Exception ignore) {/* ignore */}
                }
            }
        } catch (Exception ignore) {/* ignore */}
        // delete widget schedule cache
        try {
            String widgetRootPath = context.getFilesDir() + File.separator + "app_data" + File.separator + "general" + File.separator + "widget_schedule_lessons";
            File widgetRoot = new File(widgetRootPath);
            if (widgetRoot.exists()) {
                File[] widgets = widgetRoot.listFiles();
                for (File widget : widgets) {
                    if (!widget.isDirectory()) continue;
                    File[] components = widget.listFiles();
                    for (File component : components) {
                        String token = component.getName();
                        if (token.equals("cache.txt") || token.equals("cache_converted.txt")) component.delete();
                    }
                }
            }
        } catch (Exception ignore) {/* ignore */}
    }
    private static void apply97convertLesson(final File lesson, final String lessonsPath, final String token, final String type) throws Exception {
        final Matcher m = Pattern.compile("^" + type + "(.*)\\.txt$").matcher(token);
        if (m.find()) {
            // read lesson data
            FileReader fileReader = new FileReader(lesson);
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = fileReader.read()) != -1) sb.append((char) c);
            fileReader.close();
            String data = sb.toString();
            // convert lesson data
            // OLD: query, type, title, label, pid, timestamp, cache_token, schedule
            // NEW: schedule_type, query, type, title, timestamp, schedule
            // DAY: index -> weekday
            JSONObject lessonJson = new JSONObject(data);
            String lType = lessonJson.getString("type");
            String lLabel = lessonJson.getString("label");
            if (lType.equals("teacher_picker")) lType = "teachers";
            JSONArray lSchedule = lessonJson.getJSONArray("schedule");
            for (int i = 0; i < lSchedule.length(); i++) {
                JSONObject day = lSchedule.getJSONObject(i);
                int dWeekday = day.getInt("index");
                day.remove("index");
                day.remove("title");
                day.put("weekday", dWeekday);
                lSchedule.put(i, day);
            }
            lessonJson.remove("type");
            lessonJson.remove("title");
            lessonJson.remove("label");
            lessonJson.remove("pid");
            lessonJson.remove("cache_token");
            lessonJson.put("schedule_type", ScheduleLessons.TYPE);
            lessonJson.put("type", lType);
            lessonJson.put("title", lLabel);
            lessonJson.put("schedule", lSchedule);
            data = lessonJson.toString();
            // write converted lesson data
            // token: query.toLowerCase() instead [type + "_" + query]
            File newLesson = new File(lessonsPath + File.separator + m.group(1).toLowerCase() + ".txt");
            if (!newLesson.exists()) {
                newLesson.getParentFile().mkdirs();
                if (!newLesson.createNewFile()) {
                    throw new Exception("Failed to create file: " + newLesson.getPath());
                }
            }
            FileWriter fileWriter = new FileWriter(newLesson);
            fileWriter.write(data);
            fileWriter.close();
        }
    }
    private static void apply97convertExam(final File exam, final String examsPath, final String token, final String type) throws Exception {
        final Matcher m = Pattern.compile("^" + type + "(.*)\\.txt$").matcher(token);
        if (m.find()) {
            // read exam data
            FileReader fileReader = new FileReader(exam);
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = fileReader.read()) != -1) sb.append((char) c);
            fileReader.close();
            String data = sb.toString();
            // convert exam data
            // OLD: type, timestamp, scope(w/o teacher_picker), cache_token, schedule
            // NEW: schedule_type, query, type, title, timestamp, schedule
            // schedule OLD: subject, teacher/group, exam{date, time, room}, consult{date, time, room}
            // schedule NEW: subject, group, teacher, teacher_id, exam{date, time, room, building}, advice{date, time, room, building}
            JSONObject examJson = new JSONObject(data);
            String eType = examJson.getString("type");
            String eScope = examJson.getString("scope");
            String eToken = examJson.getString("cache_token");
            if (eType.equals("teacher_picker")) eType = "teachers";
            Matcher eTokenMatcher;
            eTokenMatcher = Pattern.compile("^group_(.*)$").matcher(eToken);
            if (eTokenMatcher.find()) {
                eToken = eTokenMatcher.group(1);
            }
            eTokenMatcher = Pattern.compile("^teacher(.*)$").matcher(eToken);
            if (eTokenMatcher.find()) {
                eToken = eTokenMatcher.group(1);
            }
            JSONArray eSchedule = examJson.getJSONArray("schedule");
            for (int i = 0; i < eSchedule.length(); i++) {
                JSONObject eExam = eSchedule.getJSONObject(i);
                String eGroup = eExam.has("group") ? eExam.getString("group") : "";
                String eTeacher = eExam.has("teacher") ? eExam.getString("teacher") : "";
                String eTeacherId = "";
                JSONObject eeExam = eExam.getJSONObject("exam");
                JSONObject eeAdvice = eExam.getJSONObject("consult");
                eeExam.put("building", "");
                eeAdvice.put("building", "");
                eExam.remove("group");
                eExam.remove("teacher");
                eExam.remove("exam");
                eExam.remove("consult");
                eExam.put("group", eGroup);
                eExam.put("teacher", eTeacher);
                eExam.put("teacher_id", eTeacherId);
                eExam.put("exam", eeExam);
                eExam.put("advice", eeAdvice);
                eSchedule.put(i, eExam);
            }
            examJson.remove("type");
            examJson.remove("scope");
            examJson.remove("cache_token");
            examJson.put("schedule_type", ScheduleExams.TYPE);
            examJson.put("query", eToken);
            examJson.put("type", eType);
            examJson.put("title", eScope);
            examJson.put("schedule", eSchedule);
            data = examJson.toString();
            // write converted lesson data
            // token: query.toLowerCase() instead [type + "_" + query]
            File newExam = new File(examsPath + File.separator + m.group(1).toLowerCase() + ".txt");
            if (!newExam.exists()) {
                newExam.getParentFile().mkdirs();
                if (!newExam.createNewFile()) {
                    throw new Exception("Failed to create file: " + newExam.getPath());
                }
            }
            FileWriter fileWriter = new FileWriter(newExam);
            fileWriter.write(data);
            fileWriter.close();
        }
    }
    private static void apply97convertAdded(final File added, final String addedPath, final String token, final String type) throws Exception {
        final Matcher m = Pattern.compile("^" + type + "(.*)\\.txt$").matcher(token);
        if (m.find()) {
            // read added data
            FileReader fileReader = new FileReader(added);
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = fileReader.read()) != -1) sb.append((char) c);
            fileReader.close();
            String data = sb.toString();
            // write converted lesson data
            // DAY: index -> weekday
            JSONArray addedJson = new JSONArray(data);
            for (int i = 0; i < addedJson.length(); i++) {
                JSONObject day = addedJson.getJSONObject(i);
                int dWeekday = day.getInt("index");
                day.remove("index");
                day.put("weekday", dWeekday);
                addedJson.put(i, day);
            }
            data = addedJson.toString();
            // write converted lesson data
            // token: query.toLowerCase() instead [type + "_" + query]
            File newAdded = new File(addedPath + File.separator + m.group(1).toLowerCase() + ".txt");
            if (!newAdded.exists()) {
                newAdded.getParentFile().mkdirs();
                if (!newAdded.createNewFile()) {
                    throw new Exception("Failed to create file: " + newAdded.getPath());
                }
            }
            FileWriter fileWriter = new FileWriter(newAdded);
            fileWriter.write(data);
            fileWriter.close();
        }
    }
    private static void apply97convertReduced(final File reduced, final String reducedPath, final String token, final String type) throws Exception {
        final Matcher m = Pattern.compile("^" + type + "(.*)\\.txt$").matcher(token);
        if (m.find()) {
            // read added data
            FileReader fileReader = new FileReader(reduced);
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = fileReader.read()) != -1) sb.append((char) c);
            fileReader.close();
            String data = sb.toString();
            // write converted lesson data
            // DAY: index -> weekday
            JSONArray reducedJson = new JSONArray(data);
            for (int i = 0; i < reducedJson.length(); i++) {
                JSONObject day = reducedJson.getJSONObject(i);
                int dWeekday = day.getInt("index");
                day.remove("index");
                day.put("weekday", dWeekday);
                reducedJson.put(i, day);
            }
            data = reducedJson.toString();
            // write converted lesson data
            // token: query.toLowerCase() instead [type + "_" + query]
            File newReduced = new File(reducedPath + File.separator + m.group(1).toLowerCase() + ".txt");
            if (!newReduced.exists()) {
                newReduced.getParentFile().mkdirs();
                if (!newReduced.createNewFile()) {
                    throw new Exception("Failed to create file: " + newReduced.getPath());
                }
            }
            FileWriter fileWriter = new FileWriter(newReduced);
            fileWriter.write(data);
            fileWriter.close();
        }
    }
}
