package com.bukhmastov.cdoitmo.util.singleton;

import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.Keep;

import com.bukhmastov.cdoitmo.activity.presenter.ScheduleLessonsWidgetConfigureActivityPresenter;
import com.bukhmastov.cdoitmo.model.entity.Suggestions;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.added.SLessonsAdded;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SDayReduced;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SLessonsReduced;
import com.bukhmastov.cdoitmo.model.user.UsersList;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidget;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Keep
@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class Migration {

    private static final String TAG = "Migration";
    private static boolean first_launch = false;

    // ----------------------------------------
    // Call migrate to initiate migration check
    // ----------------------------------------

    public static void migrate(Context context, InjectProvider injectProvider) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int lastVersionCode = injectProvider.getStoragePref().get(context, "last_version", 0);
            first_launch = lastVersionCode == 0;
            if (first_launch || lastVersionCode >= versionCode) {
                return;
            }
            try {
                Class<?> migration = Class.forName("com.bukhmastov.cdoitmo.util.singleton.Migration");
                for (int version = lastVersionCode + 1; version <= versionCode; version++) {
                    try {
                        Method method = migration.getDeclaredMethod("migrate" + version, Context.class, InjectProvider.class);
                        method.invoke(null, context, injectProvider);
                        injectProvider.getLog().i(TAG, "Migration applied for versionCode ", version);
                    } catch (NoSuchMethodException e) {
                        // migration not needed
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ExceptionInInitializerError | SecurityException | NullPointerException e) {
                        // migration failed
                        injectProvider.getLog().e(TAG, "Migration failed for versionCode ", version, " | ", e.getMessage());
                    }
                }
            } catch (Exception ignore) {
                // failed to get migration class
            }
            injectProvider.getStoragePref().put(context, "last_version", versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            injectProvider.getLog().exception(e);
        }
    }

    // -----------------------------------
    // Methods for migrations
    // migrateXX - migration to version XX
    // -----------------------------------

    @Keep
    private static void migrate121(Context context, InjectProvider injectProvider) {
        // Clear all cache storage
        try {
            String cacheRootPath = context.getCacheDir() + File.separator + "app_data";
            getUsersFolder(cacheRootPath, (folder, login) -> deleteRecursive(folder));
        } catch (Throwable ignore) {
            // ignore
        }
        // Modify permanent storage
        try {
            String rootPath = context.getFilesDir() + File.separator + "app_data";
            getUsersFolder(rootPath, (folder, login) -> {
                if (Storage.GLOBAL.equals(login)) {
                    return;
                }
                // Convert "schedule_lessons#added#" + token
                listDir(getFile(folder.getPath(), "schedule_lessons", "added"), file -> {
                    try {
                        if (file == null || !file.isFile()) {
                            return;
                        }
                        JSONArray json = new JSONArray(readFile(file));
                        SLessonsAdded added = new SLessonsAdded();
                        added.setTimestamp(injectProvider.getTime().getTimeInMillis());
                        added.setSchedule(new ArrayList<>());
                        for (int i = 0; i < json.length(); i++) {
                            try {
                                SDay sDay = new SDay().fromJson(json.getJSONObject(i));
                                added.getSchedule().add(sDay);
                            } catch (Throwable ignore) {
                                // ignore
                            }
                        }
                        writeFile(file, added.toJsonString());
                    } catch (Throwable throwable) {
                        deleteRecursive(file);
                    }
                });
                // Convert "schedule_lessons#reduced#" + token
                listDir(getFile(folder.getPath(), "schedule_lessons", "reduced"), file -> {
                    try {
                        if (file == null || !file.isFile()) {
                            return;
                        }
                        JSONArray json = new JSONArray(readFile(file));
                        SLessonsReduced reduced = new SLessonsReduced();
                        reduced.setTimestamp(injectProvider.getTime().getTimeInMillis());
                        reduced.setSchedule(new ArrayList<>());
                        for (int i = 0; i < json.length(); i++) {
                            try {
                                SDayReduced sDayReduced = new SDayReduced().fromJson(json.getJSONObject(i));
                                reduced.getSchedule().add(sDayReduced);
                            } catch (Throwable ignore) {
                                // ignore
                            }
                        }
                        writeFile(file, reduced.toJsonString());
                    } catch (Throwable throwable) {
                        deleteRecursive(file);
                    }
                });
                // Convert "schedule_" + getType() + "#recent"
                readFile(getFile(folder.getPath(), "schedule_lessons", "recent"), (file, data) -> {
                    JSONArray json = new JSONArray(data);
                    Suggestions suggestions = new Suggestions();
                    suggestions.setSuggestions(new ArrayList<>());
                    for (int i = 0; i < json.length(); i++) {
                        try {
                            suggestions.getSuggestions().add(json.getString(i));
                        } catch (Throwable ignore) {
                            // ignore
                        }
                    }
                    writeFile(file, suggestions.toJsonString());
                });
                readFile(getFile(folder.getPath(), "schedule_exams", "recent"), (file, data) -> {
                    JSONArray json = new JSONArray(data);
                    Suggestions suggestions = new Suggestions();
                    suggestions.setSuggestions(new ArrayList<>());
                    for (int i = 0; i < json.length(); i++) {
                        try {
                            suggestions.getSuggestions().add(json.getString(i));
                        } catch (Throwable ignore) {
                            // ignore
                        }
                    }
                    writeFile(file, suggestions.toJsonString());
                });
                readFile(getFile(folder.getPath(), "schedule_attestations", "recent"), (file, data) -> {
                    JSONArray json = new JSONArray(data);
                    Suggestions suggestions = new Suggestions();
                    suggestions.setSuggestions(new ArrayList<>());
                    for (int i = 0; i < json.length(); i++) {
                        try {
                            suggestions.getSuggestions().add(json.getString(i));
                        } catch (Throwable ignore) {
                            // ignore
                        }
                    }
                    writeFile(file, suggestions.toJsonString());
                });
            });
        } catch (Throwable ignore) {
            // ignore
        }
        // Convert "users#list"
        try {
            String accounts = injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", "");
            if (StringUtils.isNotBlank(accounts)) {
                UsersList usersList = new UsersList();
                usersList.setLogins(new ArrayList<>());
                JSONArray json = new JSONArray(accounts);
                for (int i = 0; i < json.length(); i++) {
                    try {
                        usersList.getLogins().add(json.getString(i));
                    } catch (Throwable ignore) {
                        // ignore
                    }
                }
                injectProvider.getStorage().put(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", usersList.toJsonString());
            }
        } catch (Throwable ignore) {
            // ignore
        }
        // Reset widgets cache
        List<String> appWidgetIds = injectProvider.getStorage().list(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons");
        for (String appWidgetId : appWidgetIds) {
            try {
                injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#cache");
                injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#cache_converted");
                Intent intent = new Intent(context, ScheduleLessonsWidget.class);
                intent.setAction(ScheduleLessonsWidget.ACTION_WIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.parseInt(appWidgetId));
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                context.sendBroadcast(intent);
            } catch (Throwable ignore) {
                // ignore
            }
        }
        // Reset protocol tracker
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
    }

    @Keep
    private static void migrate115(Context context, InjectProvider injectProvider) {
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
        try {
            final String rootPath = context.getCacheDir() + File.separator + "app_data";
            getUsersFolder(rootPath, (file, user) -> {
                try {
                    if (!user.equals("general")) return;
                    File exams = new File(rootPath + File.separator + user + File.separator + "schedule_exams" + File.separator + "lessons");
                    if (exams.isDirectory() && exams.exists()) {
                        deleteRecursive(exams);
                    }
                } catch (Exception ignore) {/* ignore */}
            });
        } catch (Exception ignore) {/* ignore */}
    }

    @Keep
    private static void migrate111(Context context, InjectProvider injectProvider) {
        ArrayList<String> appWidgetIds = injectProvider.getStorage().list(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons");
        for (String appWidgetId : appWidgetIds) {
            String settings = injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings", "");
            if (settings != null) {
                settings = settings.trim();
                if (!settings.isEmpty()) {
                    try {
                        JSONObject settingsJson = new JSONObject(settings);
                        settingsJson.put("shiftAutomatic", 0);
                        settingsJson.put("useShiftAutomatic", true);
                        injectProvider.getStorage().put(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings", settingsJson.toString());
                    } catch (Exception e) {
                        injectProvider.getLog().exception(e);
                    }
                } else {
                    injectProvider.getStorage().clear(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId);
                }
            } else {
                injectProvider.getStorage().clear(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId);
            }
        }
    }

    @Keep
    private static void migrate109(Context context, InjectProvider injectProvider) {
        try {
            final String rootPath = context.getFilesDir() + File.separator + "app_data";
            getUsersFolder(rootPath, (file, user) -> {
                try {
                    if (user.equals("general")) return;
                    File lessonsRecent = new File(rootPath + File.separator + user + File.separator + "schedule_lessons" + File.separator + "recent.txt");
                    if (lessonsRecent.exists()) {
                        lessonsRecent.delete();
                    }
                    File examsRecent = new File(rootPath + File.separator + user + File.separator + "schedule_exams" + File.separator + "recent.txt");
                    if (examsRecent.exists()) {
                        examsRecent.delete();
                    }
                    File attestationsRecent = new File(rootPath + File.separator + user + File.separator + "schedule_attestations" + File.separator + "recent.txt");
                    if (attestationsRecent.exists()) {
                        attestationsRecent.delete();
                    }
                } catch (Exception ignore) {/* ignore */}
            });
        } catch (Exception ignore) {/* ignore */}
    }

    @Keep
    private static void migrate106(Context context, InjectProvider injectProvider) {
        injectProvider.getStoragePref().put(context, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
    }

    @Keep
    private static void migrate103(Context context, InjectProvider injectProvider) {
        migrate103convertERegister(context);
        migrate103moveCacheToGeneral(context);
        String pref_e_journal_term = injectProvider.getStoragePref().get(context, "pref_e_journal_term", "0");
        if (pref_e_journal_term.equals("1")) {
            injectProvider.getStoragePref().put(context, "pref_e_journal_term", "3");
        }
    }
    @Keep
    private static void migrate103convertERegister(Context context) {
        try {
            final String rootPath = context.getCacheDir() + File.separator + "app_data";
            getUsersFolder(rootPath, (file, user) -> {
                try {
                    if (user.equals("general")) return;
                    String eregisterCorePath = rootPath + File.separator + user + File.separator + "eregister" + File.separator + "core.txt";
                    File eregisterCore = new File(eregisterCorePath);
                    if (eregisterCore.exists()) {
                        // get old data from file
                        String data = readFile(eregisterCore);
                        // delete old file
                        eregisterCore.delete();
                        // modify old data
                        JSONObject json = new JSONObject(data);
                        JSONArray groups = json.getJSONArray("groups");
                        for (int i = 0; i < groups.length(); i++) {
                            JSONArray terms = groups.getJSONObject(i).getJSONArray("terms");
                            for (int j = 0; j < terms.length(); j++) {
                                JSONArray subjects = terms.getJSONObject(j).getJSONArray("subjects");
                                for (int k = 0; k < subjects.length(); k++) {
                                    JSONObject subject = subjects.getJSONObject(k);
                                    subjects.put(k, new JSONObject()
                                            .put("name", subject.getString("name"))
                                            .put("attestations", new JSONArray().put(new JSONObject()
                                                    .put("name", subject.getString("type"))
                                                    .put("mark", subject.getString("mark"))
                                                    .put("markdate", subject.getString("markDate"))
                                                    .put("value", subject.getDouble("currentPoints"))
                                                    .put("points", subject.getJSONArray("points"))
                                            ))
                                    );
                                }
                            }
                        }
                        data = json.toString();
                        // create new file and write updated eregister data
                        writeFile(eregisterCorePath, data);
                    }
                } catch (Exception ignore) {/* ignore */}
            });
        } catch (Exception ignore) {/* ignore */}
    }
    @Keep
    private static void migrate103moveCacheToGeneral(Context context) {
        try {
            final String rootPath = context.getCacheDir() + File.separator + "app_data";
            final String destinationPath = rootPath + File.separator + "general" + File.separator;
            getUsersFolder(rootPath, (file, user) -> {
                try {
                    if (user.equals("general")) return;
                    // move schedule_lessons
                    final String scheduleLessonsCorePath = rootPath + File.separator + user + File.separator + "schedule_lessons" + File.separator + "lessons";
                    final File fileSL = new File(scheduleLessonsCorePath);
                    if (fileSL.exists() && fileSL.isDirectory()) {
                        final File[] fileListSL = fileSL.listFiles();
                        for (File fileItemSL : fileListSL) {
                            try {
                                if (fileItemSL.isDirectory()) {
                                    fileItemSL.delete();
                                    continue;
                                }
                                String name = fileItemSL.getName();
                                String data = readFile(fileItemSL);
                                fileItemSL.delete();
                                writeFile(destinationPath + "schedule_lessons" + File.separator + "lessons" + File.separator + name, data);
                            } catch (Exception ignore) {/* ignore */}
                        }
                        fileSL.delete();
                        final File fileSLcore = new File(rootPath + File.separator + user + File.separator + "schedule_lessons");
                        if (fileSLcore.exists()) {
                            fileSLcore.delete();
                        }
                    }
                    // move schedule_exams
                    final String scheduleExamsCorePath = rootPath + File.separator + user + File.separator + "schedule_exams" + File.separator + "lessons";
                    final File fileSE = new File(scheduleExamsCorePath);
                    if (fileSE.exists() && fileSE.isDirectory()) {
                        final File[] fileListSE = fileSE.listFiles();
                        for (File fileItemSE : fileListSE) {
                            try {
                                if (fileItemSE.isDirectory()) {
                                    fileItemSE.delete();
                                    continue;
                                }
                                String name = fileItemSE.getName();
                                String data = readFile(fileItemSE);
                                fileItemSE.delete();
                                writeFile(destinationPath + "schedule_exams" + File.separator + "lessons" + File.separator + name, data);
                            } catch (Exception ignore) {/* ignore */}
                        }
                        fileSE.delete();
                        final File fileSEcore = new File(rootPath + File.separator + user + File.separator + "schedule_exams");
                        if (fileSEcore.exists()) {
                            fileSEcore.delete();
                        }
                    }
                    // move university
                    final String universityCorePath = rootPath + File.separator + user + File.separator + "university";
                    final File fileU = new File(universityCorePath);
                    if (fileU.exists() && fileU.isDirectory()) {
                        final File[] fileListU = fileU.listFiles();
                        for (File fileItemU : fileListU) {
                            try {
                                if (fileItemU.isDirectory()) {
                                    final String folderName = fileItemU.getName();
                                    final File[] fileItemListU = fileItemU.listFiles();
                                    for (File fileItemItemU : fileItemListU) {
                                        try {
                                            if (fileItemItemU.isDirectory()) {
                                                fileItemItemU.delete();
                                                continue;
                                            }
                                            String name = fileItemItemU.getName();
                                            String data = readFile(fileItemItemU);
                                            fileItemItemU.delete();
                                            writeFile(destinationPath + "university" + File.separator + folderName + File.separator + name, data);
                                        } catch (Exception ignore) {/* ignore */}
                                    }
                                    fileItemU.delete();
                                } else {
                                    String name = fileItemU.getName();
                                    String data = readFile(fileItemU);
                                    fileItemU.delete();
                                    writeFile(destinationPath + "university" + File.separator + name, data);
                                }
                            } catch (Exception ignore) {/* ignore */}
                        }
                        fileU.delete();
                    }
                } catch (Exception ignore) {/* ignore */}
            });
        } catch (Exception ignore) {/* ignore */}
    }

    @Keep
    private static void migrate97(Context context, InjectProvider injectProvider) {
        if (!first_launch) {
            injectProvider.getStoragePref().put(context, "pref_default_values_applied", true);
        }
        // Backwards compatibility
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
                                            migrate97convertLesson(lesson, lessonsPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            migrate97convertLesson(lesson, lessonsPath, token, "room_");
                                        } else if (token.matches("^\\d{6}\\.txt$")) {
                                            migrate97convertLesson(lesson, lessonsPath, token, "");
                                        } else {
                                            // "teacher_picker_" and any broken schedules needs to be removed without converting
                                            lesson.delete();
                                        }
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
                                            migrate97convertExam(exam, examsPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            migrate97convertExam(exam, examsPath, token, "room_");
                                        } else if (token.startsWith("teacher")) {
                                            migrate97convertExam(exam, examsPath, token, "teacher");
                                        } else {
                                            // "teacher_picker_" and any broken schedules needs to be removed without converting
                                            exam.delete();
                                        }
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
                                            migrate97convertAdded(added, addedPath, token, "group_");
                                        } else if (token.startsWith("room_")) {
                                            migrate97convertAdded(added, addedPath, token, "room_");
                                        } else if (token.matches("^\\d{6}\\.txt$")) {
                                            migrate97convertAdded(added, addedPath, token, "");
                                        } else {
                                            added.delete();
                                        }
                                    } catch (Exception e) {
                                        try {
                                            added.delete();
                                        } catch (Exception ignore) {/* ignore */}
                                    }
                                }
                            }
                        } catch (Exception ignore) {/* ignore */}
                        // delete reduced lessons
                        try {
                            String reducedPath = filesRootPath + File.separator + userLogin + File.separator + "schedule_lessons" + File.separator + "reduced";
                            File schedule_reduced = new File(reducedPath);
                            if (schedule_reduced.exists()) {
                                File[] reducedList = schedule_reduced.listFiles();
                                for (File reduced : reducedList) {
                                    reduced.delete();
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
    @Keep
    private static void migrate97convertLesson(File lesson, String lessonsPath, String token, String type) throws Exception {
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
            // delete old file
            lesson.delete();
            // create new file and write converted lesson data
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
    @Keep
    private static void migrate97convertExam(File exam, String examsPath, String token, String type) throws Exception {
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
            // delete old file
            exam.delete();
            // create new file and write converted exam data
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
    @Keep
    private static void migrate97convertAdded(File added, String addedPath, String token, String type) throws Exception {
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
            // delete old file
            added.delete();
            // create new file and write converted added lesson data
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

    @Keep
    private static void migrate93(Context context, InjectProvider injectProvider) {
        // new theme
        final boolean dark_theme = injectProvider.getStoragePref().get(context, "pref_dark_theme", false);
        injectProvider.getStoragePref().delete(context, "pref_dark_theme");
        injectProvider.getStoragePref().put(context, "pref_theme", dark_theme ? "dark" : "light");
        // get rid of pref_allow_owner_notifications
        injectProvider.getStoragePref().delete(context, "pref_allow_owner_notifications");
        // move files
        injectProvider.getAccount().logoutTemporarily(context, null);
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
    }

    @Keep
    private static void migrate90(Context context, InjectProvider injectProvider) {
        boolean compact = injectProvider.getStoragePref().get(context, "pref_schedule_lessons_compact_view_of_reduced_lesson", true);
        injectProvider.getStoragePref().delete(context, "pref_schedule_lessons_compact_view_of_reduced_lesson");
        injectProvider.getStoragePref().put(context, "pref_schedule_lessons_view_of_reduced_lesson", compact ? "compact" : "full");
    }

    @Keep
    private static void migrate83(Context context, InjectProvider injectProvider) {
        ArrayList<String> appWidgetIds = injectProvider.getStorage().list(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons");
        for (String appWidgetId : appWidgetIds) {
            String settings = injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings", "");
            if (settings != null) {
                settings = settings.trim();
                if (!settings.isEmpty()) {
                    try {
                        JSONObject settingsJson = new JSONObject(settings);
                        settingsJson.put("shift", 0);
                        injectProvider.getStorage().put(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings", settingsJson.toString());
                    } catch (Exception e) {
                        injectProvider.getLog().exception(e);
                        injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings");
                    }
                }
            }
        }
    }

    @Keep
    private static void migrate78(Context context, InjectProvider injectProvider) {
        ArrayList<String> appWidgetIds = injectProvider.getStorage().list(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons");
        for (String appWidgetId : appWidgetIds) {
            String settings = injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings", "");
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
                                theme.put("background", ScheduleLessonsWidgetConfigureActivityPresenter.Default.Theme.Dark.background);
                                theme.put("text", ScheduleLessonsWidgetConfigureActivityPresenter.Default.Theme.Dark.text);
                                theme.put("opacity", ScheduleLessonsWidgetConfigureActivityPresenter.Default.Theme.Dark.opacity);
                            } else {
                                theme.put("background", ScheduleLessonsWidgetConfigureActivityPresenter.Default.Theme.Light.background);
                                theme.put("text", ScheduleLessonsWidgetConfigureActivityPresenter.Default.Theme.Light.text);
                                theme.put("opacity", ScheduleLessonsWidgetConfigureActivityPresenter.Default.Theme.Light.opacity);
                            }
                            settingsJson.remove("darkTheme");
                            settingsJson.put("theme", theme);
                            injectProvider.getStorage().put(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings", settingsJson.toString());
                        }
                    } catch (Exception e) {
                        injectProvider.getLog().exception(e);
                        injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings");
                    }
                } else {
                    empty_settings = true;
                }
            } else {
                empty_settings = true;
            }
            if (empty_settings) {
                injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#settings");
                injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#cache");
                injectProvider.getStorage().delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#cache_converted");
            }
        }
    }

    @Keep
    private static void migrate71(Context context, InjectProvider injectProvider) {
        injectProvider.getStoragePref().delete(context, "pref_open_drawer_at_startup");
        injectProvider.getStoragePref().put(context, "pref_first_launch", injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", "").trim().isEmpty());
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
    }

    @Keep
    private static void migrate62(Context context, InjectProvider injectProvider) {
        injectProvider.getStoragePref().put(context, "pref_dynamic_refresh", injectProvider.getStoragePref().get(context, "pref_tab_refresh", "0"));
        injectProvider.getStoragePref().put(context, "pref_static_refresh", injectProvider.getStoragePref().get(context, "pref_schedule_refresh", "168"));
        injectProvider.getStoragePref().delete(context, "pref_tab_refresh");
        injectProvider.getStoragePref().delete(context, "pref_schedule_refresh");
    }

    // This migration no longer supported (5 users will be affected (28.06.2018))
    //@Keep
    //private static void migrate51(Context context, InjectProvider injectProvider) {
    //    injectProvider.getStorage().clear(context, Storage.CACHE, Storage.USER, "protocol#log");
    //    if (injectProvider.getStoragePref().get(context, "pref_protocol_changes_track", true)) {
    //        ProtocolTracker.setup(context, injectProvider.getStoragePref(), 0);
    //    }
    //}

    @Keep
    private static void migrate29(Context context, InjectProvider injectProvider) {
        injectProvider.getStorage().delete(context, Storage.CACHE, Storage.USER, "eregister#core");
    }

    @Keep
    private static void migrate26(Context context, InjectProvider injectProvider) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancelAll();
        }
        injectProvider.getAccount().logoutPermanently(context, null);
        injectProvider.getStoragePref().clearExceptPref(context);
    }

    // ---------------------
    // Tools to modify files
    // ---------------------

    @Keep
    private interface UserFolderCallback {
        void onUserFolder(File folder, String login) throws Throwable;
    }
    @Keep
    private interface FileCallback {
        void onFile(File file) throws Throwable;
    }
    @Keep
    private interface StringCallback {
        void onData(File file, String data) throws Throwable;
    }
    @Keep
    private static void getUsersFolder(String rootPath, UserFolderCallback callback) {
        File root = new File(rootPath);
        if (root.exists() && root.isDirectory()) {
            File[] userFolders = root.listFiles();
            for (File userFolder : userFolders) {
                try {
                    if (userFolder == null || !userFolder.isDirectory()) {
                        continue;
                    }
                    String login = userFolder.getName();
                    callback.onUserFolder(userFolder, login);
                } catch (Throwable ignore) {
                    // ignore
                }
            }
        }
    }
    @Keep
    private static void listDir(File dir, FileCallback callback) {
        if (dir.exists() && dir.isDirectory()) {
            File[] items = dir.listFiles();
            for (File item : items) {
                try {
                    callback.onFile(item);
                } catch (Throwable ignore) {
                    // ignore
                }
            }
        }
    }
    @Keep
    private static File getFile(String...path) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (String p : path) {
            if (index++ > 0) {
                sb.append(File.separator);
            }
            sb.append(p);
        }
        return new File(sb.toString());
    }
    @Keep
    private static void readFile(File file, StringCallback callback) {
        try {
            callback.onData(file, readFile(file));
        } catch (Throwable ignore) {
            // ignore
        }
    }
    @Keep
    private static String readFile(File file) throws Exception {
        FileReader fileReader = new FileReader(file);
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = fileReader.read()) != -1) sb.append((char) c);
        fileReader.close();
        return sb.toString();
    }
    @Keep
    private static void writeFile(String path, String data) throws Exception {
        writeFile(new File(path), data);
    }
    @Keep
    private static void writeFile(File file, String data) throws Exception {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            if (!file.createNewFile()) {
                throw new Exception("Failed to create file: " + file.getPath());
            }
        }
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(data);
        fileWriter.close();
    }
    @Keep
    private static void deleteRecursive(File fileOrDirectory) {
        boolean result = true;
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        } else {
            fileOrDirectory.delete();
        }
    }
}
