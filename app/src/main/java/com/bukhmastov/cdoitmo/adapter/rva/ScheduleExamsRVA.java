package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsRVA extends RVA {

    private static final String TAG = "ScheduleExamsRVA";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EXAM = 1;
    private static final int TYPE_UPDATE_TIME = 2;
    private static final int TYPE_NO_EXAMS = 3;
    private static final int TYPE_PICKER_HEADER = 4;
    private static final int TYPE_PICKER_ITEM = 5;
    private static final int TYPE_PICKER_NO_TEACHERS = 6;

    private static Pattern patternBrokenDate = Pattern.compile("^(\\d{1,2})(\\s\\S*)(.*)$", Pattern.CASE_INSENSITIVE);

    private final ConnectedActivity activity;
    private final JSONObject data;
    private final int mode; // 0 - exam, 1 - credit
    private final Static.StringCallback callback;
    private String type = "";
    private String query = null;

    public ScheduleExamsRVA(final ConnectedActivity activity, JSONObject data, int mode, final Static.StringCallback callback) {
        this.activity = activity;
        this.data = data;
        this.mode = mode;
        this.callback = callback;
        try {
            type = data.getString("type");
            query = data.getString("query");
            addItems(json2dataset(activity, data));
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_schedule_both_header; break;
            case TYPE_EXAM: layout = R.layout.layout_schedule_exams_item; break;
            case TYPE_UPDATE_TIME: layout = R.layout.layout_schedule_both_update_time; break;
            case TYPE_NO_EXAMS: layout = R.layout.state_nothing_to_display_compact; break;
            case TYPE_PICKER_HEADER: layout = R.layout.layout_schedule_teacher_picker_header; break;
            case TYPE_PICKER_ITEM: layout = R.layout.layout_schedule_teacher_picker_item; break;
            case TYPE_PICKER_NO_TEACHERS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, RVA.Item item) {
        switch (item.type) {
            case TYPE_HEADER: {
                bindHeader(container, item);
                break;
            }
            case TYPE_EXAM: {
                bindExam(container, item);
                break;
            }
            case TYPE_UPDATE_TIME: {
                bindUpdateTime(container, item);
                break;
            }
            case TYPE_NO_EXAMS: {
                bindNoExams(container, item);
                break;
            }
            case TYPE_PICKER_HEADER: {
                bindPickerHeader(container, item);
                break;
            }
            case TYPE_PICKER_ITEM: {
                bindPickerItem(container, item);
                break;
            }
            case TYPE_PICKER_NO_TEACHERS: {
                bindPickerNoTeachers(container, item);
                break;
            }
        }
    }

    private void bindHeader(View container, Item item) {
        try {
            final String title = getString(item.data, "title");
            final String week = getString(item.data, "week");
            TextView schedule_lessons_header = container.findViewById(R.id.schedule_lessons_header);
            if (title != null && !title.isEmpty()) {
                schedule_lessons_header.setText(title);
            } else {
                ((ViewGroup) schedule_lessons_header.getParent()).removeView(schedule_lessons_header);
            }
            TextView schedule_lessons_week = container.findViewById(R.id.schedule_lessons_week);
            if (week != null && !week.isEmpty()) {
                schedule_lessons_week.setText(week);
            } else {
                ((ViewGroup) schedule_lessons_week.getParent()).removeView(schedule_lessons_week);
            }
            container.findViewById(R.id.schedule_lessons_menu).setOnClickListener(view -> Static.T.runThread(() -> {
                final String cache_token = query == null ? null : query.toLowerCase();
                final boolean cached = cache_token != null && !Storage.file.general.cache.get(activity, "schedule_exams#lessons#" + cache_token, "").isEmpty();
                Static.T.runOnUiThread(() -> {
                    try {
                        final PopupMenu popup = new PopupMenu(activity, view);
                        final Menu menu = popup.getMenu();
                        popup.getMenuInflater().inflate(R.menu.schedule_exams, menu);
                        menu.findItem(cached ? R.id.add_to_cache : R.id.remove_from_cache).setVisible(false);
                        popup.setOnMenuItemClickListener(item1 -> {
                            Log.v(TAG, "menu | popup item | clicked | " + item1.getTitle().toString());
                            switch (item1.getItemId()) {
                                case R.id.add_to_cache:
                                case R.id.remove_from_cache: {
                                    try {
                                        if (cache_token == null) {
                                            Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                        } else {
                                            if (Storage.file.general.cache.exists(activity, "schedule_exams#lessons#" + cache_token)) {
                                                if (Storage.file.general.cache.delete(activity, "schedule_exams#lessons#" + cache_token)) {
                                                    Static.snackBar(activity, activity.getString(R.string.cache_false));
                                                } else {
                                                    Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                }
                                            } else {
                                                if (data == null) {
                                                    Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                } else {
                                                    if (Storage.file.general.cache.put(activity, "schedule_exams#lessons#" + cache_token, data.toString())) {
                                                        Static.snackBar(activity, activity.getString(R.string.cache_true));
                                                    } else {
                                                        Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                    }
                                    break;
                                }
                                case R.id.open_settings: {
                                    activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleExamsFragment.class, null);
                                    break;
                                }
                            }
                            return false;
                        });
                        popup.show();
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                });
            }));
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindExam(View container, Item item) {
        try {
            final String t = item.data.has("type") ? item.data.getString("type") : "exam";
            final String subject = item.data.has("subject") ? item.data.getString("subject") : "";
            final String group = item.data.has("group") ? item.data.getString("group") : "";
            final String teacher = item.data.has("teacher") ? item.data.getString("teacher") : "";
            final String teacher_id = item.data.has("teacher_id") ? item.data.getString("teacher_id") : "";
            final JSONObject exam = item.data.has("exam") ? item.data.getJSONObject("exam") : null;
            final JSONObject advice = item.data.has("advice") ? item.data.getJSONObject("advice") : null;
            final String desc;
            final boolean touch_icon_enabled;
            switch (type) {
                case "group": {
                    desc = teacher;
                    touch_icon_enabled = (teacher_id != null && !teacher_id.isEmpty()) || (teacher != null && !teacher.isEmpty());
                    break;
                }
                case "teacher": {
                    desc = group;
                    touch_icon_enabled = group != null && !group.isEmpty();
                    break;
                }
                default: {
                    desc = null;
                    touch_icon_enabled = false;
                    break;
                }
            }
            // title and description
            ((TextView) container.findViewById(R.id.exam_header)).setText(subject.toUpperCase());
            if (desc != null && !desc.trim().isEmpty()) {
                ((TextView) container.findViewById(R.id.exam_desc)).setText(desc);
                container.findViewById(R.id.exam_desc).setVisibility(View.VISIBLE);
            } else {
                container.findViewById(R.id.exam_desc).setVisibility(View.GONE);
            }
            // badges (actually, only one)
            View exam_touch_icon = container.findViewById(R.id.exam_touch_icon);
            exam_touch_icon.setVisibility(touch_icon_enabled ? View.VISIBLE : View.GONE);
            if (touch_icon_enabled) {
                exam_touch_icon.setOnClickListener(view -> {
                    try {
                        Log.v(TAG, "exam_touch_icon clicked");
                        PopupMenu popup = new PopupMenu(activity, view);
                        Menu menu = popup.getMenu();
                        popup.getMenuInflater().inflate(R.menu.schedule_exams_item, menu);
                        switch (type) {
                            case "group": {
                                menu.findItem(R.id.open_group).setVisible(false);
                                menu.findItem(R.id.open_teacher).setTitle(teacher);
                                menu.findItem(R.id.open_teacher).setVisible(true);
                                break;
                            }
                            case "teacher": {
                                menu.findItem(R.id.open_group).setTitle(activity.getString(R.string.group) + " " + group);
                                menu.findItem(R.id.open_group).setVisible(true);
                                menu.findItem(R.id.open_teacher).setVisible(false);
                                break;
                            }
                            default: {
                                menu.findItem(R.id.open_group).setVisible(false);
                                menu.findItem(R.id.open_teacher).setVisible(false);
                                break;
                            }
                        }
                        popup.setOnMenuItemClickListener(item1 -> {
                            Log.v(TAG, "popup.MenuItem clicked | " + item1.getTitle().toString());
                            switch (item1.getItemId()) {
                                case R.id.open_group: if (group != null && !group.isEmpty()) callback.onCall(group); break;
                                case R.id.open_teacher: {
                                    if (teacher_id != null && !teacher_id.isEmpty()) {
                                        callback.onCall(teacher_id);
                                    } else if (teacher != null && !teacher.isEmpty()) {
                                        callback.onCall(teacher);
                                    }
                                    break;
                                }
                            }
                            return false;
                        });
                        popup.show();
                    } catch (Exception e){
                        Static.error(e);
                    }
                });
            }
            // advice
            boolean isAdviceExists = false;
            if (advice != null) {
                String date_format_append = "";
                String advice_date = advice.getString("date");
                String advice_time = advice.getString("time");
                String advice_room = advice.getString("room");
                String advice_building = advice.getString("building");
                if (advice_date != null && !advice_date.isEmpty()) {
                    String date = advice_date;
                    if (advice_time != null && !advice_time.isEmpty()) {
                        date += " " + advice_time;
                        date_format_append = " HH:mm";
                    }
                    String place = "";
                    if (advice_room != null && !advice_room.isEmpty()) {
                        place += advice_room;
                    }
                    if (advice_building != null && !advice_building.isEmpty()) {
                        place += " " + advice_building;
                    }
                    place = place.trim();
                    if (!place.isEmpty()) {
                        place = activity.getString(R.string.place) + ": " + place;
                    }
                    ((TextView) container.findViewById(R.id.exam_info_advice_date)).setText(cuteDate(activity, date, date_format_append));
                    TextView exam_info_advice_place = container.findViewById(R.id.exam_info_advice_place);
                    if (!place.isEmpty()) {
                        exam_info_advice_place.setText(place);
                        exam_info_advice_place.setVisibility(View.VISIBLE);
                    } else {
                        exam_info_advice_place.setVisibility(View.GONE);
                    }
                    isAdviceExists = true;
                }
            }
            // exam
            boolean isExamExists = false;
            if (exam != null) {
                String date_format_append = "";
                String exam_date = exam.getString("date");
                String exam_time = exam.getString("time");
                String exam_room = exam.getString("room");
                String exam_building = exam.getString("building");
                if (exam_date != null && !exam_date.isEmpty()) {
                    String date = exam_date;
                    if (exam_time != null && !exam_time.isEmpty()) {
                        date += " " + exam_time;
                        date_format_append = " HH:mm";
                    }
                    String place = "";
                    if (exam_room != null && !exam_room.isEmpty()) {
                        place += exam_room;
                    }
                    if (exam_building != null && !exam_building.isEmpty()) {
                        place += " " + exam_building;
                    }
                    place = place.trim();
                    if (!place.isEmpty()) {
                        place = activity.getString(R.string.place) + ": " + place;
                    }
                    ((TextView) container.findViewById(R.id.exam_info_exam_title)).setText("credit".equals(t) ? R.string.credit : R.string.exam);
                    ((TextView) container.findViewById(R.id.exam_info_exam_date)).setText(cuteDate(activity, date, date_format_append));
                    TextView exam_info_exam_place = container.findViewById(R.id.exam_info_exam_place);
                    if (!place.isEmpty()) {
                        exam_info_exam_place.setText(place);
                        exam_info_exam_place.setVisibility(View.VISIBLE);
                    } else {
                        exam_info_exam_place.setVisibility(View.GONE);
                    }
                    isExamExists = true;
                }
            }
            container.findViewById(R.id.exam_info_advice).setVisibility(isAdviceExists ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.exam_info_exam).setVisibility(isExamExists ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.separator_small).setVisibility((isAdviceExists && !isExamExists) || (!isAdviceExists && isExamExists) ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.exam_info).setVisibility(isAdviceExists || isExamExists ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindUpdateTime(View container, Item item) {
        try {
            final String text = getString(item.data, "text");
            ((TextView) container.findViewById(R.id.update_time)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNoExams(View container, Item item) {
        try {
            String info = "";
            Calendar calendar = Static.getCalendar();
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            if ((month >= Calendar.SEPTEMBER && (month <= Calendar.DECEMBER && day < 20)) || (month >= Calendar.FEBRUARY && (month <= Calendar.MAY && day < 20))) {
                info = "\n" + activity.getString(R.string.no_exams_info);
            }
            ((TextView) container.findViewById(R.id.ntd_text)).setText((mode == 0 ? activity.getText(R.string.no_exams) : activity.getText(R.string.no_credits)) + info);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerHeader(View container, Item item) {
        try {
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.choose_teacher) + ":";
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.teachers_found) + ":";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_header)).setText(text);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerItem(View container, Item item) {
        try {
            final String pid = item.data.getString("pid");
            String teacher = item.data.getString("person");
            String post = item.data.getString("post");
            if (post != null && !post.isEmpty()) {
                teacher += " (" + post + ")";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_title)).setText(teacher);
            container.findViewById(R.id.teacher_picker_item).setOnClickListener(view -> {
                if (pid != null && !pid.isEmpty()) {
                    callback.onCall(pid);
                }
            });
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerNoTeachers(View container, Item item) {
        try {
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.no_teachers);
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.no_teachers).toLowerCase();
            }
            ((TextView) container.findViewById(R.id.ntd_text)).setText(text);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private ArrayList<Item> json2dataset(ConnectedActivity activity, JSONObject json) throws JSONException {
        final ArrayList<Item> dataset = new ArrayList<>();
        // check
        if (!ScheduleExams.TYPE.equals(json.getString("schedule_type"))) {
            return dataset;
        }
        if (type.equals("teachers")) {
            // teacher picker mode
            final JSONArray schedule = json.getJSONArray("schedule");
            if (schedule.length() > 0) {
                dataset.add(new Item(TYPE_PICKER_HEADER, new JSONObject().put("query", query)));
                for (int i = 0; i < schedule.length(); i++) {
                    final JSONObject teacher = schedule.getJSONObject(i);
                    dataset.add(new Item(TYPE_PICKER_ITEM, teacher));
                }
            } else {
                dataset.add(new Item(TYPE_PICKER_NO_TEACHERS, new JSONObject().put("query", query)));
            }
        } else {
            // regular schedule mode
            // header
            dataset.add(new Item(TYPE_HEADER, new JSONObject()
                    .put("title", ScheduleExams.getScheduleHeader(activity, json.getString("title"), json.getString("type")))
                    .put("week", ScheduleExams.getScheduleWeek(activity, -1))
            ));
            // schedule
            final JSONArray exams = json.getJSONArray("schedule");
            int exams_count = 0;
            for (int i = 0; i < exams.length(); i++) {
                final JSONObject exam = exams.getJSONObject(i);
                dataset.add(new Item(TYPE_EXAM, exam));
                exams_count++;
            }
            if (exams_count == 0) {
                dataset.add(new Item(TYPE_NO_EXAMS, null));
            } else {
                // update time
                dataset.add(new Item(TYPE_UPDATE_TIME, new JSONObject().put("text", activity.getString(R.string.update_date) + " " + Static.getUpdateTime(activity, json.getLong("timestamp")))));
            }
        }
        // that's all
        return dataset;
    }

    protected static String cuteDate(Context context, String date, String date_format_append) {
        try {
            String date_format = "dd.MM.yyyy" + date_format_append;
            if (isValidFormat(context, date, date_format)) {
                date = Static.cuteDate(context, date_format, date);
            } else {
                Matcher m = patternBrokenDate.matcher(date);
                if (m.find()) {
                    String d = m.group(2);
                    String dt = d.trim();
                    if (dt.startsWith("янв")) d = ".01";
                    if (dt.startsWith("фев")) d = ".02";
                    if (dt.startsWith("мар")) d = ".03";
                    if (dt.startsWith("апр")) d = ".04";
                    if (dt.startsWith("май")) d = ".05";
                    if (dt.startsWith("июн")) d = ".06";
                    if (dt.startsWith("июл")) d = ".07";
                    if (dt.startsWith("авг")) d = ".08";
                    if (dt.startsWith("сен")) d = ".09";
                    if (dt.startsWith("окт")) d = ".10";
                    if (dt.startsWith("ноя")) d = ".11";
                    if (dt.startsWith("дек")) d = ".12";
                    date = m.group(1) + d + m.group(3);
                }
                date_format = "dd.MM" + date_format_append;
                if (isValidFormat(context, date, date_format)) {
                    date = cuteDateWOYear(context, date_format, date);
                }
            }
        } catch (Exception ignore) {/* ignore */}
        return date;
    }
    protected static String cuteDateWOYear(Context context, String date_format, String date_string) throws Exception {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, Static.getLocale(context));
        Calendar date = Static.getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(Static.getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(Static.ldgZero(date.get(Calendar.HOUR_OF_DAY)))
                .append(":")
                .append(Static.ldgZero(date.get(Calendar.MINUTE)))
                .toString();
    }
    protected static boolean isValidFormat(Context context, String value, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Static.getLocale(context));
            Date date = sdf.parse(value);
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
            return date != null;
        } catch (Exception e) {
            return false;
        }
    }
    protected String getString(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (json.isNull(key) || object == null) {
                return null;
            } else {
                try {
                    String value = (String) object;
                    return value.equals("null") ? null : value;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected int getInt(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
