package com.bukhmastov.cdoitmo.util.singleton;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Keep;

import com.bukhmastov.cdoitmo.activity.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

    public static void migrate(final Context context, final InjectProvider injectProvider) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int lastVersionCode = injectProvider.getStoragePref().get(context, "last_version", 0);
            first_launch = lastVersionCode == 0;
            if (lastVersionCode < versionCode) {
                // Skip migration for first launch. Users with version <28 become deprecated and unsupported.
                // (actually, its 10 users at the beginning of december 2017)
                // (5 users at the start of may 2018)
                // (3 users at the end of june 2018)
                if (!first_launch) {
                    try {
                        Class<?> migration = Class.forName("com.bukhmastov.cdoitmo.util.singleton.Migration");
                        for (int i = lastVersionCode + 1; i <= versionCode; i++) {
                            try {
                                Method method = migration.getDeclaredMethod("migrate" + i, Context.class, InjectProvider.class);
                                method.invoke(null, context, injectProvider);
                                injectProvider.getLog().i(TAG, "Migration applied for versionCode ", i);
                            } catch (NoSuchMethodException e) {
                                // migration not needed
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ExceptionInInitializerError | SecurityException | NullPointerException e) {
                                // migration failed
                                injectProvider.getLog().e(TAG, "Migration failed for versionCode ", i, " | ", e.getMessage());
                            }
                        }
                    } catch (Exception ignore) {
                        // failed to get migration class
                    }
                }
                injectProvider.getStoragePref().put(context, "last_version", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            injectProvider.getLog().exception(e);
        }
    }

    // -----------------------------------
    // Methods for migrations
    // migrateXX - migration to version XX
    // -----------------------------------

    @Keep
    private static void migrate115(final Context context, final InjectProvider injectProvider) {
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
    }

    @Keep
    private static void migrate111(final Context context, final InjectProvider injectProvider) {
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
    private static void migrate109(final Context context, final InjectProvider injectProvider) {
        try {
            final String rootPath = context.getFilesDir() + File.separator + "app_data";
            getUsersFolder(context, rootPath, (file, user) -> {
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
    private static void migrate106(final Context context, final InjectProvider injectProvider) {
        injectProvider.getStoragePref().put(context, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
    }

    @Keep
    private static void migrate103(final Context context, final InjectProvider injectProvider) {
        migrate103convertERegister(context);
        migrate103moveCacheToGeneral(context);
        String pref_e_journal_term = injectProvider.getStoragePref().get(context, "pref_e_journal_term", "0");
        if (pref_e_journal_term.equals("1")) {
            injectProvider.getStoragePref().put(context, "pref_e_journal_term", "3");
        }
    }
    @Keep
    private static void migrate103convertERegister(final Context context) {
        try {
            final String rootPath = context.getCacheDir() + File.separator + "app_data";
            getUsersFolder(context, rootPath, (file, user) -> {
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
    private static void migrate103moveCacheToGeneral(final Context context) {
        try {
            final String rootPath = context.getCacheDir() + File.separator + "app_data";
            final String destinationPath = rootPath + File.separator + "general" + File.separator;
            getUsersFolder(context, rootPath, (file, user) -> {
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
    private static void migrate97(final Context context, final InjectProvider injectProvider) {
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
    private static void migrate97convertLesson(final File lesson, final String lessonsPath, final String token, final String type) throws Exception {
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
    private static void migrate97convertExam(final File exam, final String examsPath, final String token, final String type) throws Exception {
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
    private static void migrate97convertAdded(final File added, final String addedPath, final String token, final String type) throws Exception {
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
    private static void migrate93(final Context context, final InjectProvider injectProvider) {
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
    private static void migrate90(final Context context, final InjectProvider injectProvider) {
        boolean compact = injectProvider.getStoragePref().get(context, "pref_schedule_lessons_compact_view_of_reduced_lesson", true);
        injectProvider.getStoragePref().delete(context, "pref_schedule_lessons_compact_view_of_reduced_lesson");
        injectProvider.getStoragePref().put(context, "pref_schedule_lessons_view_of_reduced_lesson", compact ? "compact" : "full");
    }

    @Keep
    private static void migrate83(final Context context, final InjectProvider injectProvider) {
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
    private static void migrate78(final Context context, final InjectProvider injectProvider) {
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
    private static void migrate71(final Context context, final InjectProvider injectProvider) {
        injectProvider.getStoragePref().delete(context, "pref_open_drawer_at_startup");
        injectProvider.getStoragePref().put(context, "pref_first_launch", injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", "").trim().isEmpty());
        injectProvider.getThread().run(Thread.BACKGROUND, () -> injectProvider.getProtocolTracker().reset(context));
    }

    @Keep
    private static void migrate62(final Context context, final InjectProvider injectProvider) {
        injectProvider.getStoragePref().put(context, "pref_dynamic_refresh", injectProvider.getStoragePref().get(context, "pref_tab_refresh", "0"));
        injectProvider.getStoragePref().put(context, "pref_static_refresh", injectProvider.getStoragePref().get(context, "pref_schedule_refresh", "168"));
        injectProvider.getStoragePref().delete(context, "pref_tab_refresh");
        injectProvider.getStoragePref().delete(context, "pref_schedule_refresh");
    }

    // This migration no longer supported (5 users will be affected (28.06.2018))
    //@Keep
    //private static void migrate51(final Context context, final InjectProvider injectProvider) {
    //    injectProvider.getStorage().clear(context, Storage.CACHE, Storage.USER, "protocol#log");
    //    if (injectProvider.getStoragePref().get(context, "pref_protocol_changes_track", true)) {
    //        ProtocolTracker.setup(context, injectProvider.getStoragePref(), 0);
    //    }
    //}

    @Keep
    private static void migrate29(final Context context, final InjectProvider injectProvider) {
        injectProvider.getStorage().delete(context, Storage.CACHE, Storage.USER, "eregister#core");
    }

    @Keep
    private static void migrate26(final Context context, final InjectProvider injectProvider) {
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
    private interface Callback {
        void onUserFolder(final File file, String user);
    }
    @Keep
    private static void getUsersFolder(final Context context, final String rootPath, final Callback callback) throws Exception {
        File root = new File(rootPath);
        if (root.exists() && root.isDirectory()) {
            File[] users = root.listFiles();
            for (File user : users) {
                try {
                    if (!user.isDirectory()) continue;
                    String uLogin = user.getName();
                    callback.onUserFolder(user, uLogin);
                } catch (Exception ignore) {/* ignore */}
            }
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
        File file = new File(path);
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
}
