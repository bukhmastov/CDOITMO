package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.objects.entities.LessonUnit;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonsModifyFragment extends ConnectedFragment {

    private static final String TAG = "SLModifyFragment";
    public enum TYPE {create, edit}
    private TYPE type;
    private LessonUnit lessonUnit;
    private int index = 0;
    private String hash = null;
    private boolean block_time_start = false;
    private boolean block_time_end = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons_modify, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            Bundle extras = getArguments();
            if (extras == null) {
                throw new NullPointerException("extras cannot be null");
            }
            type = (TYPE) extras.getSerializable("action_type");
            if (type == null) {
                throw new NullPointerException("type cannot be null");
            }
            switch (type) {
                case create: activity.updateToolbar(activity.getString(R.string.lesson_creation), R.drawable.ic_schedule_lessons); break;
                case edit: activity.updateToolbar(activity.getString(R.string.lesson_editing), R.drawable.ic_schedule_lessons); break;
                default:
                    Exception exception = new Exception("got wrong type from arguments bundle: " + type.toString());
                    Log.wtf(exception);
                    throw exception;
            }
            display();
        } catch (Exception e) {
            Static.error(e);
            activity.back();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    private void display() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    lessonUnit = retrieveExtras(getArguments());
                    if (type == TYPE.edit) {
                        retrieveEditingLesson();
                    }
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TextInputEditText lesson_title = activity.findViewById(R.id.lesson_title);
                                if (lessonUnit.title != null) lesson_title.setText(lessonUnit.title);
                                lesson_title.requestFocus();
                                lesson_title.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        lessonUnit.title = s.toString();
                                    }
                                });

                                final TextInputEditText lesson_time_start = activity.findViewById(R.id.lesson_time_start);
                                final TextInputEditText lesson_time_end = activity.findViewById(R.id.lesson_time_end);
                                if (lessonUnit.timeStart != null) lesson_time_start.setText(lessonUnit.timeStart);
                                if (lessonUnit.timeEnd != null) lesson_time_end.setText(lessonUnit.timeEnd);
                                lesson_time_start.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        if (block_time_start) {
                                            block_time_start = false;
                                            return;
                                        }
                                        String st = s.toString().trim();
                                        Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(st);
                                        if (time.find()) {
                                            Calendar st_calendar = Calendar.getInstance();
                                            st_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                                            st_calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                                            st_calendar.set(Calendar.SECOND, 0);
                                            st = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + ldgZero(st_calendar.get(Calendar.MINUTE));
                                            if (lesson_time_end.getText().toString().isEmpty()) {
                                                Calendar nt_calendar = Calendar.getInstance();
                                                nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                                                block_time_end = true;
                                                String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + ldgZero(nt_calendar.get(Calendar.MINUTE));
                                                lessonUnit.timeEnd = insert;
                                                int selection = lesson_time_end.getSelectionStart();
                                                lesson_time_end.setText(insert);
                                                lesson_time_end.setSelection(selection);
                                            } else {
                                                String nt = lesson_time_end.getText().toString();
                                                Matcher next_time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(nt);
                                                if (next_time.find()) {
                                                    Calendar nt_calendar = Calendar.getInstance();
                                                    nt_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(next_time.group(1)));
                                                    nt_calendar.set(Calendar.MINUTE, Integer.parseInt(next_time.group(2)));
                                                    nt_calendar.set(Calendar.SECOND, 0);
                                                    if (nt_calendar.getTimeInMillis() <= st_calendar.getTimeInMillis()) {
                                                        nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                                                        block_time_end = true;
                                                        String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + ldgZero(nt_calendar.get(Calendar.MINUTE));
                                                        lessonUnit.timeEnd = insert;
                                                        int selection = lesson_time_end.getSelectionStart();
                                                        lesson_time_end.setText(insert);
                                                        lesson_time_end.setSelection(selection);
                                                    }
                                                }
                                            }
                                            block_time_start = true;
                                            int selection = lesson_time_start.getSelectionStart();
                                            lesson_time_start.setText(st);
                                            lesson_time_start.setSelection(selection);
                                        }
                                        lessonUnit.timeStart = st;
                                    }
                                });
                                lesson_time_end.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        if (block_time_end) {
                                            block_time_end = false;
                                            return;
                                        }
                                        String et = s.toString().trim();
                                        Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(et);
                                        if (time.find()) {
                                            Calendar et_calendar = Calendar.getInstance();
                                            et_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                                            et_calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                                            et_calendar.set(Calendar.SECOND, 0);
                                            et = et_calendar.get(Calendar.HOUR_OF_DAY) + ":" + ldgZero(et_calendar.get(Calendar.MINUTE));
                                            if (lesson_time_start.getText().toString().isEmpty()) {
                                                Calendar st_calendar = Calendar.getInstance();
                                                st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                                                block_time_start = true;
                                                String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + ldgZero(st_calendar.get(Calendar.MINUTE));
                                                lessonUnit.timeStart = insert;
                                                int selection = lesson_time_start.getSelectionStart();
                                                lesson_time_start.setText(insert);
                                                lesson_time_start.setSelection(selection);
                                            } else {
                                                String st = lesson_time_start.getText().toString();
                                                Matcher previous_time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(st);
                                                if (previous_time.find()) {
                                                    Calendar st_calendar = Calendar.getInstance();
                                                    st_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(previous_time.group(1)));
                                                    st_calendar.set(Calendar.MINUTE, Integer.parseInt(previous_time.group(2)));
                                                    st_calendar.set(Calendar.SECOND, 0);
                                                    if (st_calendar.getTimeInMillis() >= et_calendar.getTimeInMillis()) {
                                                        st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                                                        block_time_start = true;
                                                        String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + ldgZero(st_calendar.get(Calendar.MINUTE));
                                                        lessonUnit.timeStart = insert;
                                                        int selection = lesson_time_start.getSelectionStart();
                                                        lesson_time_start.setText(insert);
                                                        lesson_time_start.setSelection(selection);
                                                    }
                                                }
                                            }
                                            block_time_end = true;
                                            int selection = lesson_time_end.getSelectionStart();
                                            lesson_time_end.setText(et);
                                            lesson_time_end.setSelection(selection);
                                        }
                                        lessonUnit.timeEnd = et;
                                    }
                                });

                                Spinner lesson_week = activity.findViewById(R.id.lesson_week);
                                ArrayAdapter<?> lesson_week_adapter = ArrayAdapter.createFromResource(activity, R.array.week_types_titles, R.layout.spinner_layout_simple);
                                lesson_week_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                lesson_week.setAdapter(lesson_week_adapter);
                                switch (lessonUnit.week) {
                                    case 2: lesson_week.setSelection(0); break;
                                    case 0: lesson_week.setSelection(1); break;
                                    case 1: lesson_week.setSelection(2); break;
                                }
                                lesson_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                        String[] week_types_values = getResources().getStringArray(R.array.week_types_values);
                                        lessonUnit.week = Integer.parseInt(week_types_values[position]);
                                    }
                                    public void onNothingSelected(AdapterView<?> parent) {}
                                });

                                Spinner lesson_day_of_week = activity.findViewById(R.id.lesson_day_of_week);
                                ArrayAdapter<?> lesson_day_of_week_adapter = ArrayAdapter.createFromResource(activity, R.array.days_of_week_titles, R.layout.spinner_layout_simple);
                                lesson_day_of_week_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                lesson_day_of_week.setAdapter(lesson_day_of_week_adapter);
                                lesson_day_of_week.setSelection(lessonUnit.day);
                                lesson_day_of_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                        String[] week_types_values = getResources().getStringArray(R.array.days_of_week_values);
                                        lessonUnit.day = Integer.parseInt(week_types_values[position]);
                                    }
                                    public void onNothingSelected(AdapterView<?> parent) {}
                                });

                                final AutoCompleteTextView lesson_type = activity.findViewById(R.id.lesson_type);
                                if (lessonUnit.type != null) lesson_type.setText(lessonUnit.type);
                                lesson_type.setThreshold(1);
                                lesson_type.setAdapter(ArrayAdapter.createFromResource(activity, R.array.lessons_types, android.R.layout.simple_dropdown_item_1line));
                                lesson_type.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        lessonUnit.type = s.toString();
                                    }
                                });

                                TextInputEditText lesson_group = activity.findViewById(R.id.lesson_group);
                                if (lessonUnit.group != null) lesson_group.setText(lessonUnit.group);
                                lesson_group.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        lessonUnit.group = s.toString();
                                    }
                                });

                                final AutoCompleteTextView lesson_teacher = activity.findViewById(R.id.lesson_teacher);
                                final ProgressBar lesson_teacher_bar = activity.findViewById(R.id.lesson_teacher_bar);
                                final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<JSONObject>());
                                if (lessonUnit.teacher != null) {
                                    TeacherSearch.blocked = true;
                                    lesson_teacher.setText(lessonUnit.teacher);
                                }
                                teacherPickerAdapter.setNotifyOnChange(true);
                                lesson_teacher.setThreshold(1);
                                lesson_teacher.setAdapter(teacherPickerAdapter);
                                lesson_teacher.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        final String query = s.toString().trim();
                                        teacherPickerAdapter.clear();
                                        lesson_teacher.dismissDropDown();
                                        if (!query.isEmpty()) {
                                            TeacherSearch.search(activity, query, lesson_teacher_bar, new TeacherSearch.response() {
                                                @Override
                                                public void onPermitted() {
                                                    lessonUnit.teacher = query;
                                                    lessonUnit.teacher_id = null;
                                                }
                                                @Override
                                                public void onSuccess(final JSONObject json) {
                                                    Static.T.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            teacherPickerAdapter.clear();
                                                            try {
                                                                if (Objects.equals(json.getString("type"), "teacher_picker")) {
                                                                    JSONArray jsonArray = json.getJSONArray("list");
                                                                    ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                                    for (int i = 0; i < jsonArray.length(); i++) {
                                                                        arrayList.add(jsonArray.getJSONObject(i));
                                                                    }
                                                                    teacherPickerAdapter.addAll(arrayList);
                                                                    teacherPickerAdapter.addTeachers(arrayList);
                                                                    if (arrayList.size() > 0) {
                                                                        lesson_teacher.showDropDown();
                                                                    }
                                                                }
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    });
                                                }
                                                @Override
                                                public void onProgress(int state) {}
                                                @Override
                                                public void onFailure(int state) {}
                                            });
                                        }
                                    }
                                });
                                lesson_teacher.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        try {
                                            JSONObject item = teacherPickerAdapter.getItem(position);
                                            if (item == null) throw new Exception("Teacher item is null");
                                            lessonUnit.teacher = item.getString("person");
                                            lessonUnit.teacher_id = String.valueOf(item.getInt("pid"));
                                            TeacherSearch.blocked = true;
                                            TeacherSearch.lastQuery = lessonUnit.teacher;
                                            lesson_teacher.setText(lessonUnit.teacher);
                                            lesson_teacher.dismissDropDown();
                                        } catch (Exception e) {
                                            Static.error(e);
                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    }
                                });

                                TextInputEditText lesson_room = activity.findViewById(R.id.lesson_room);
                                if (lessonUnit.room != null) lesson_room.setText(lessonUnit.room);
                                lesson_room.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        lessonUnit.room = s.toString();
                                    }
                                });

                                AutoCompleteTextView lesson_building = activity.findViewById(R.id.lesson_building);
                                if (lessonUnit.building != null) lesson_building.setText(lessonUnit.building);
                                lesson_building.setThreshold(1);
                                lesson_building.setAdapter(ArrayAdapter.createFromResource(activity, R.array.buildings, android.R.layout.simple_dropdown_item_1line));
                                lesson_building.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        lessonUnit.building = s.toString();
                                    }
                                });

                                Button lesson_create_button = activity.findViewById(R.id.lesson_create_button);
                                switch (type) {
                                    case create: lesson_create_button.setText(activity.getString(R.string.create)); break;
                                    case edit: lesson_create_button.setText(activity.getString(R.string.save)); break;
                                }
                                lesson_create_button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Static.T.runThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.v(TAG, "create_button clicked");
                                                if (lessonUnit.title == null || lessonUnit.title.isEmpty()) {
                                                    Log.v(TAG, "lessonUnit.title required");
                                                    Static.snackBar(activity, activity.getString(R.string.lesson_title_required));
                                                    return;
                                                }
                                                if (lessonUnit.timeStart == null || lessonUnit.timeStart.isEmpty()) {
                                                    Log.v(TAG, "lessonUnit.timeStart required");
                                                    Static.snackBar(activity, activity.getString(R.string.lesson_time_start_required));
                                                    return;
                                                }
                                                if (lessonUnit.type != null) {
                                                    if (Objects.equals(lessonUnit.type.toLowerCase(), activity.getString(R.string.lecture).toLowerCase())) lessonUnit.type = "lecture";
                                                    if (Objects.equals(lessonUnit.type.toLowerCase(), activity.getString(R.string.practice).toLowerCase())) lessonUnit.type = "practice";
                                                    if (Objects.equals(lessonUnit.type.toLowerCase(), activity.getString(R.string.lab).toLowerCase())) lessonUnit.type = "lab";
                                                }
                                                switch (type) {
                                                    case create: {
                                                        if (ScheduleLessons.createLesson(activity, lessonUnit)) {
                                                            ScheduleLessonsFragment.reScheduleRequired = true;
                                                            activity.back();
                                                        } else {
                                                            Log.w(TAG, "failed to create lesson");
                                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                        }
                                                        break;
                                                    }
                                                    case edit: {
                                                        if (ScheduleLessons.editLesson(activity, lessonUnit.cache_token, index, hash, lessonUnit)) {
                                                            ScheduleLessonsFragment.reScheduleRequired = true;
                                                            activity.back();
                                                        } else {
                                                            Log.w(TAG, "failed to edit lesson");
                                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                        });
                                    }
                                });
                            } catch (Exception e) {
                                Static.error(e);
                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                activity.back();
                            }
                        }
                    });
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    activity.back();
                }
            }
        });
    }
    private LessonUnit retrieveExtras(Bundle extras) throws Exception {
        if (extras == null) {
            throw new NullPointerException("extras cannot be null");
        }
        String header = getStringExtra(extras, "header", true);
        TextView slc_title = activity.findViewById(R.id.slc_title);
        TextView slc_desc = activity.findViewById(R.id.slc_desc);
        if (slc_title != null) {
            slc_title.setText(header);
        }
        if (slc_desc != null) {
            int week = Static.getWeek(activity);
            if (week >= 0) {
                slc_desc.setText(week + " " + activity.getString(R.string.school_week));
            } else {
                slc_desc.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            }
        }
        if (type == TYPE.edit) {
            hash = getStringExtra(extras, "hash", true);
            index = extras.getInt("day");
        }
        LessonUnit lessonUnit = new LessonUnit();
        lessonUnit.day = extras.getInt("day");
        lessonUnit.week = extras.getInt("week");
        lessonUnit.cache_token = getStringExtra(extras, "cache_token", true);
        lessonUnit.title = getStringExtra(extras, "title", false);
        lessonUnit.timeStart = getStringExtra(extras, "timeStart", false);
        lessonUnit.timeEnd = getStringExtra(extras, "timeEnd", false);
        lessonUnit.type = getStringExtra(extras, "type", false);
        lessonUnit.group = getStringExtra(extras, "group", false);
        lessonUnit.teacher = getStringExtra(extras, "teacher", false);
        lessonUnit.teacher_id = getStringExtra(extras, "teacher_id", false);
        lessonUnit.room = getStringExtra(extras, "room", false);
        lessonUnit.building = getStringExtra(extras, "building", false);
        switch (lessonUnit.type) {
            case "practice": lessonUnit.type = activity.getString(R.string.practice); break;
            case "lecture": lessonUnit.type = activity.getString(R.string.lecture); break;
            case "lab": lessonUnit.type = activity.getString(R.string.lab); break;
        }
        return lessonUnit;
    }
    private String getStringExtra(Bundle extras, String key, boolean restrict) throws Exception {
        String value = extras.getString(key);
        if (value == null) {
            if (restrict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                value = "";
            }
        }
        return value;
    }
    private void retrieveEditingLesson() throws Exception {
        String addedStr = Storage.file.perm.get(activity, "schedule_lessons#added#" + lessonUnit.cache_token, "");
        JSONArray added;
        if (addedStr.isEmpty()) {
            added = new JSONArray();
        } else {
            added = new JSONArray(addedStr);
        }
        boolean found = false;
        for (int i = 0; i < added.length(); i++) {
            JSONObject day = added.getJSONObject(i);
            if (day.getInt("index") == lessonUnit.day) {
                JSONArray lessons = day.getJSONArray("lessons");
                for (int j = 0; j < lessons.length(); j++) {
                    JSONObject lesson = lessons.getJSONObject(j);
                    if (Objects.equals(hash, Static.crypt(lesson.toString()))) {
                        if (lesson.getString("subject") != null) lessonUnit.title = lesson.getString("subject");
                        if (lesson.getString("timeStart") != null) lessonUnit.timeStart = lesson.getString("timeStart");
                        if (lesson.getString("timeEnd") != null) lessonUnit.timeEnd = lesson.getString("timeEnd");
                        if (lesson.getString("type") != null) lessonUnit.type = lesson.getString("type");
                        if (lesson.getString("group") != null) lessonUnit.group = lesson.getString("group");
                        if (lesson.getString("teacher") != null) lessonUnit.teacher = lesson.getString("teacher");
                        if (lesson.getString("teacher_id") != null) lessonUnit.teacher_id = lesson.getString("teacher_id");
                        if (lesson.getString("room") != null) lessonUnit.room = lesson.getString("room");
                        if (lesson.getString("building") != null) lessonUnit.building = lesson.getString("building");
                        if (lessonUnit.type != null) {
                            switch (lessonUnit.type) {
                                case "practice": lessonUnit.type = activity.getString(R.string.practice); break;
                                case "lecture": lessonUnit.type = activity.getString(R.string.lecture); break;
                                case "lab": lessonUnit.type = activity.getString(R.string.lab); break;
                            }
                        }
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) throw new Exception("Custom lesson not found");
    }
    private String ldgZero(int time){
        return time % 10 == time ? "0" + String.valueOf(time) : String.valueOf(time);
    }

    private static class TeacherSearch {

        private static final String TAG = "SLModifyFragment.TS";
        interface response {
            void onPermitted();
            void onProgress(int state);
            void onFailure(int state);
            void onSuccess(JSONObject json);
        }
        private static RequestHandle request = null;
        static boolean blocked = false;
        private static String lastQuery = "";

        public static void search(final Context context, final String query, final ProgressBar progressBar, final response delegate) {
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    if (request != null) {
                        request.cancel(true);
                        request = null;
                    }
                    boolean allowed = true;
                    if (Objects.equals(lastQuery, query)) {
                        allowed = false;
                    }
                    if (allowed) {
                        try {
                            new JSONObject(query);
                            allowed = false;
                        } catch (Exception ignore) {
                            // ignore
                        }
                    }
                    if (blocked || !allowed) {
                        blocked = false;
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                        return;
                    }
                    Log.v(TAG, "search | query=" + query);
                    delegate.onPermitted();
                    ScheduleLessons scheduleLessons = new ScheduleLessons(context);
                    scheduleLessons.setHandler(new ScheduleLessons.response() {
                        @Override
                        public void onProgress(int state) {
                            Log.v(TAG, "search | progress " + state);
                            delegate.onProgress(state);
                        }
                        @Override
                        public void onFailure(int state) {
                            Log.v(TAG, "search | failure " + state);
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                            delegate.onFailure(state);
                        }
                        @Override
                        public void onSuccess(JSONObject json) {
                            Log.v(TAG, "search | success");
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                            delegate.onSuccess(json);
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            request = requestHandle;
                        }
                    });
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    });
                    scheduleLessons.search(query);
                    lastQuery = query;
                }
            });
        }
    }
}
