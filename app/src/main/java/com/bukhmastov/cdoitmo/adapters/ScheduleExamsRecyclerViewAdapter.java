package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ScheduleExamsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    private static final String TAG = "SERVAdapter";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EXAM = 1;
    private static final int TYPE_UPDATE_TIME = 2;
    private static final int TYPE_NO_EXAMS = 3;
    private static final int TYPE_PICKER_HEADER = 4;
    private static final int TYPE_PICKER_ITEM = 5;
    private static final int TYPE_PICKER_NO_TEACHERS = 6;
    public static class Item {
        public int type;
        public JSONObject data;
        public Item (int type, JSONObject data) {
            this.type = type;
            this.data = data;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        protected final ViewGroup container;
        ViewHolder(ViewGroup container) {
            super(container);
            this.container = container;
        }
    }
    private final ConnectedActivity activity;
    private final JSONObject data;
    private final ArrayList<Item> dataset;
    private final Static.StringCallback callback;
    private String type = "";
    private String query = null;

    public ScheduleExamsRecyclerViewAdapter(final ConnectedActivity activity, JSONObject data, final Static.StringCallback callback) {
        this.activity = activity;
        this.data = data;
        this.callback = callback;
        this.dataset = new ArrayList<>();
        try {
            type = data.getString("type");
            query = data.getString("query");
            addItems(json2dataset(activity, data));
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataset.get(position).type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        switch (viewType) {
            case TYPE_HEADER: layout = R.layout.layout_schedule_both_header; break;
            case TYPE_EXAM: layout = R.layout.layout_schedule_exams_item; break;
            case TYPE_UPDATE_TIME: layout = R.layout.layout_schedule_both_update_time; break;
            case TYPE_NO_EXAMS: layout = R.layout.nothing_to_display; break;
            case TYPE_PICKER_HEADER: layout = R.layout.layout_schedule_teacher_picker_header; break;
            case TYPE_PICKER_ITEM: layout = R.layout.layout_schedule_teacher_picker_item; break;
            case TYPE_PICKER_NO_TEACHERS: layout = R.layout.nothing_to_display; break;
            default: return null;
        }
        return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = dataset.get(position);
        switch (item.type) {
            case TYPE_HEADER: {
                bindHeader(holder, item);
                break;
            }
            case TYPE_EXAM: {
                bindExam(holder, item);
                break;
            }
            case TYPE_UPDATE_TIME: {
                bindUpdateTime(holder, item);
                break;
            }
            case TYPE_NO_EXAMS: {
                bindNoExams(holder, item);
                break;
            }
            case TYPE_PICKER_HEADER: {
                bindPickerHeader(holder, item);
                break;
            }
            case TYPE_PICKER_ITEM: {
                bindPickerItem(holder, item);
                break;
            }
            case TYPE_PICKER_NO_TEACHERS: {
                bindPickerNoTeachers(holder, item);
                break;
            }
        }
    }

    private void bindHeader(RecyclerView.ViewHolder holder, Item item) {
        try {
            final String title = getString(item.data, "title");
            final String week = getString(item.data, "week");
            ViewHolder viewHolder = (ViewHolder) holder;
            TextView schedule_lessons_header = viewHolder.container.findViewById(R.id.schedule_lessons_header);
            if (title != null && !title.isEmpty()) {
                schedule_lessons_header.setText(title);
            } else {
                ((ViewGroup) schedule_lessons_header.getParent()).removeView(schedule_lessons_header);
            }
            TextView schedule_lessons_week = viewHolder.container.findViewById(R.id.schedule_lessons_week);
            if (week != null && !week.isEmpty()) {
                schedule_lessons_week.setText(week);
            } else {
                ((ViewGroup) schedule_lessons_week.getParent()).removeView(schedule_lessons_week);
            }
            viewHolder.container.findViewById(R.id.schedule_lessons_menu).setOnClickListener(view -> Static.T.runThread(() -> {
                final String cache_token = query == null ? null : query.toLowerCase();
                final boolean cached = cache_token != null && !Storage.file.cache.get(activity, "schedule_exams#lessons#" + cache_token, "").isEmpty();
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
                                            if (Storage.file.cache.exists(activity, "schedule_exams#lessons#" + cache_token)) {
                                                if (Storage.file.cache.delete(activity, "schedule_exams#lessons#" + cache_token)) {
                                                    Static.snackBar(activity, activity.getString(R.string.cache_false));
                                                } else {
                                                    Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                }
                                            } else {
                                                if (data == null) {
                                                    Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                } else {
                                                    if (Storage.file.cache.put(activity, "schedule_exams#lessons#" + cache_token, data.toString())) {
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
                                    activity.openActivityOrFragment(ConnectedActivity.TYPE.stackable, SettingsScheduleExamsFragment.class, null);
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
    private void bindExam(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            final String subject = item.data.getString("subject");
            final String group = item.data.getString("group");
            final String teacher = item.data.getString("teacher");
            final String teacher_id = item.data.getString("teacher_id");
            final JSONObject exam = item.data.getJSONObject("exam");
            final JSONObject advice = item.data.getJSONObject("advice");
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
            ((TextView) viewHolder.container.findViewById(R.id.exam_header)).setText(subject.toUpperCase());
            if (desc != null && !desc.trim().isEmpty()) {
                ((TextView) viewHolder.container.findViewById(R.id.exam_desc)).setText(desc);
                viewHolder.container.findViewById(R.id.exam_desc).setVisibility(View.VISIBLE);
            } else {
                viewHolder.container.findViewById(R.id.exam_desc).setVisibility(View.GONE);
            }
            // badges (actually, only one)
            View exam_touch_icon = viewHolder.container.findViewById(R.id.exam_touch_icon);
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
                String date_format = "dd.MM.yyyy";
                String advice_date = advice.getString("date");
                String advice_time = advice.getString("time");
                String advice_room = advice.getString("room");
                String advice_building = advice.getString("building");
                if (advice_date != null && !advice_date.isEmpty()) {
                    String date = advice_date;
                    if (advice_time != null && !advice_time.isEmpty()) {
                        date += " " + advice_time;
                        date_format += " HH:mm";
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
                    try {
                        date = Static.cuteDate(activity, date_format, date);
                    } catch (Exception ignore) {/* ignore */}
                    ((TextView) viewHolder.container.findViewById(R.id.exam_info_advice_date)).setText(date);
                    TextView exam_info_advice_place = viewHolder.container.findViewById(R.id.exam_info_advice_place);
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
                String date_format = "dd.MM.yyyy";
                String exam_date = exam.getString("date");
                String exam_time = exam.getString("time");
                String exam_room = exam.getString("room");
                String exam_building = exam.getString("building");
                if (exam_date != null && !exam_date.isEmpty()) {
                    String date = exam_date;
                    if (exam_time != null && !exam_time.isEmpty()) {
                        date += " " + exam_time;
                        date_format += " HH:mm";
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
                    try {
                        date = Static.cuteDate(activity, date_format, date);
                    } catch (Exception ignore) {/* ignore */}
                    ((TextView) viewHolder.container.findViewById(R.id.exam_info_exam_date)).setText(date);
                    TextView exam_info_exam_place = viewHolder.container.findViewById(R.id.exam_info_exam_place);
                    if (!place.isEmpty()) {
                        exam_info_exam_place.setText(place);
                        exam_info_exam_place.setVisibility(View.VISIBLE);
                    } else {
                        exam_info_exam_place.setVisibility(View.GONE);
                    }
                    isExamExists = true;
                }
            }
            viewHolder.container.findViewById(R.id.exam_info_advice).setVisibility(isAdviceExists ? View.VISIBLE : View.GONE);
            viewHolder.container.findViewById(R.id.exam_info_exam).setVisibility(isExamExists ? View.VISIBLE : View.GONE);
            viewHolder.container.findViewById(R.id.separator_small).setVisibility((isAdviceExists && !isExamExists) || (!isAdviceExists && isExamExists) ? View.GONE : View.VISIBLE);
            viewHolder.container.findViewById(R.id.exam_info).setVisibility(isAdviceExists || isExamExists ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindUpdateTime(RecyclerView.ViewHolder holder, Item item) {
        try {
            final String text = getString(item.data, "text");
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.update_time)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNoExams(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(R.string.no_exams);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerHeader(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.choose_teacher) + ":";
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.teachers_found) + ":";
            }
            ((TextView) viewHolder.container.findViewById(R.id.teacher_picker_header)).setText(text);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerItem(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            final String pid = item.data.getString("pid");
            String teacher = item.data.getString("person");
            String post = item.data.getString("post");
            if (post != null && !post.isEmpty()) {
                teacher += " (" + post + ")";
            }
            ((TextView) viewHolder.container.findViewById(R.id.teacher_picker_title)).setText(teacher);
            viewHolder.container.findViewById(R.id.teacher_picker_item).setOnClickListener(view -> {
                if (pid != null && !pid.isEmpty()) {
                    callback.onCall(pid);
                }
            });
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerNoTeachers(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.no_teachers);
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.no_teachers).toLowerCase();
            }
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(text);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    public ArrayList<Item> json2dataset(ConnectedActivity activity, JSONObject json) throws JSONException {
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
    public void addItem(Item item) {
        this.dataset.add(item);
        this.notifyItemInserted(this.dataset.size() - 1);
    }
    public void addItems(ArrayList<Item> dataset) {
        int itemStart = this.dataset.size() - 1;
        this.dataset.addAll(dataset);
        this.notifyItemRangeInserted(itemStart, dataset.size() - 1);
    }
    public void removeItem(int position) {
        this.dataset.remove(position);
        this.notifyItemRemoved(position);
        this.notifyItemRangeChanged(position, this.dataset.size() - 1);
    }

    protected JSONObject getJsonObject(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONObject) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected JSONArray getJsonArray(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONArray) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
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
    private View inflate(int layout) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
