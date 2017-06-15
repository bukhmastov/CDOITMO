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
                case create: activity.updateToolbar(getString(R.string.lesson_creation), R.drawable.ic_schedule_lessons); break;
                case edit: activity.updateToolbar(getString(R.string.lesson_editing), R.drawable.ic_schedule_lessons); break;
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

    private void display() {
        try {
            lessonUnit = retrieveExtras(getArguments());
            if (type == TYPE.edit) {
                retrieveEditingLesson();
            }


            /*final AutoCompleteAsyncTextView test = (AutoCompleteAsyncTextView) activity.findViewById(R.id.test);
            test.setThreshold(2);
            test.setAdapter(new AutoCompleteTeacherLessonsSearchAdapter(activity));
            test.setLoadingIndicator((ProgressBar) activity.findViewById(R.id.testProgressBar));
            test.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    try {
                        JSONObject teacher = (JSONObject) adapterView.getItemAtPosition(position);
                        lessonUnit.teacher = teacher.getString("person");
                        lessonUnit.teacher_id = String.valueOf(teacher.getInt("pid"));
                        test.setText(lessonUnit.teacher);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            });*/


            TextInputEditText lesson_title = (TextInputEditText) activity.findViewById(R.id.lesson_title);
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

            TextInputEditText lesson_time_start = (TextInputEditText) activity.findViewById(R.id.lesson_time_start);
            if (lessonUnit.timeStart != null) lesson_time_start.setText(lessonUnit.timeStart);
            lesson_time_start.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.timeStart = s.toString();
                    TextInputEditText lesson_time_end = (TextInputEditText) activity.findViewById(R.id.lesson_time_end);
                    if (lesson_time_end.getText().toString().isEmpty()) {
                        Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(s.toString());
                        if (time.find()) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                            calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                            calendar.set(Calendar.SECOND, 0);
                            long timestamp = calendar.getTimeInMillis();
                            calendar = Calendar.getInstance();
                            calendar.setTime(new Date(timestamp + 5400000));
                            lesson_time_end.setText(ldgZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + ldgZero(calendar.get(Calendar.MINUTE)));
                        }
                    }
                }
            });

            TextInputEditText lesson_time_end = (TextInputEditText) activity.findViewById(R.id.lesson_time_end);
            if (lessonUnit.timeEnd != null) lesson_time_end.setText(lessonUnit.timeEnd);
            lesson_time_end.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.timeEnd = s.toString();
                    TextInputEditText lesson_time_start = (TextInputEditText) activity.findViewById(R.id.lesson_time_start);
                    if (lesson_time_start.getText().toString().isEmpty()) {
                        Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(s.toString());
                        if (time.find()) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                            calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                            calendar.set(Calendar.SECOND, 0);
                            long timestamp = calendar.getTimeInMillis();
                            calendar = Calendar.getInstance();
                            calendar.setTime(new Date(timestamp - 5400000));
                            lesson_time_start.setText(ldgZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + ldgZero(calendar.get(Calendar.MINUTE)));
                        }
                    }
                }
            });

            Spinner lesson_week = (Spinner) activity.findViewById(R.id.lesson_week);
            ArrayAdapter<?> lesson_week_adapter = ArrayAdapter.createFromResource(getContext(), R.array.week_types_titles, R.layout.spinner_layout_simple);
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

            Spinner lesson_day_of_week = (Spinner) activity.findViewById(R.id.lesson_day_of_week);
            ArrayAdapter<?> lesson_day_of_week_adapter = ArrayAdapter.createFromResource(getContext(), R.array.days_of_week_titles, R.layout.spinner_layout_simple);
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

            final AutoCompleteTextView lesson_type = (AutoCompleteTextView) activity.findViewById(R.id.lesson_type);
            if (lessonUnit.type != null) lesson_type.setText(lessonUnit.type);
            lesson_type.setThreshold(1);
            lesson_type.setAdapter(ArrayAdapter.createFromResource(getContext(), R.array.lessons_types, android.R.layout.simple_dropdown_item_1line));
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

            TextInputEditText lesson_group = (TextInputEditText) activity.findViewById(R.id.lesson_group);
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

            final AutoCompleteTextView lesson_teacher = (AutoCompleteTextView) activity.findViewById(R.id.lesson_teacher);
            final ProgressBar lesson_teacher_bar = (ProgressBar) activity.findViewById(R.id.lesson_teacher_bar);
            final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(getContext(), new ArrayList<JSONObject>());
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
                    String query = s.toString().trim();
                    teacherPickerAdapter.clear();
                    lesson_teacher.dismissDropDown();
                    if (!query.isEmpty()) {
                        TeacherSearch.search(getContext(), query, lesson_teacher_bar, new TeacherSearch.response() {
                            @Override
                            public void onSuccess(JSONObject json) {
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
                            @Override
                            public void onProgress(int state) {
                                lessonUnit.teacher_id = null;
                            }
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
                        lesson_teacher.setText(lessonUnit.teacher);
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, getString(R.string.something_went_wrong));
                    }
                }
            });

            TextInputEditText lesson_room = (TextInputEditText) activity.findViewById(R.id.lesson_room);
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

            AutoCompleteTextView lesson_building = (AutoCompleteTextView) activity.findViewById(R.id.lesson_building);
            if (lessonUnit.building != null) lesson_building.setText(lessonUnit.building);
            lesson_building.setThreshold(1);
            lesson_building.setAdapter(ArrayAdapter.createFromResource(getContext(), R.array.buildings, android.R.layout.simple_dropdown_item_1line));
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

            Button lesson_create_button = (Button) activity.findViewById(R.id.lesson_create_button);
            switch (type) {
                case create: lesson_create_button.setText(getString(R.string.create)); break;
                case edit: lesson_create_button.setText(getString(R.string.save)); break;
            }
            lesson_create_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "create_button clicked");
                    if (lessonUnit.title == null || lessonUnit.title.isEmpty()) {
                        Log.v(TAG, "lessonUnit.title required");
                        Static.snackBar(activity, getString(R.string.lesson_title_required));
                        return;
                    }
                    if (lessonUnit.timeStart == null || lessonUnit.timeStart.isEmpty()) {
                        Log.v(TAG, "lessonUnit.timeStart required");
                        Static.snackBar(activity, getString(R.string.lesson_time_start_required));
                        return;
                    }
                    if (lessonUnit.type != null) {
                        if (Objects.equals(lessonUnit.type.toLowerCase(), getString(R.string.lecture).toLowerCase())) lessonUnit.type = "lecture";
                        if (Objects.equals(lessonUnit.type.toLowerCase(), getString(R.string.practice).toLowerCase())) lessonUnit.type = "practice";
                        if (Objects.equals(lessonUnit.type.toLowerCase(), getString(R.string.lab).toLowerCase())) lessonUnit.type = "lab";
                    }
                    switch (type) {
                        case create: {
                            if (ScheduleLessons.createLesson(activity, lessonUnit)) {
                                ScheduleLessonsFragment.reScheduleRequired = true;
                                activity.back();
                            } else {
                                Log.w(TAG, "failed to create lesson");
                                Static.snackBar(activity, getString(R.string.something_went_wrong));
                            }
                            break;
                        }
                        case edit: {
                            if (ScheduleLessons.editLesson(activity, lessonUnit.cache_token, index, hash, lessonUnit)) {
                                ScheduleLessonsFragment.reScheduleRequired = true;
                                activity.back();
                            } else {
                                Log.w(TAG, "failed to edit lesson");
                                Static.snackBar(activity, getString(R.string.something_went_wrong));
                            }
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            Static.error(e);
            Static.snackBar(activity, getString(R.string.something_went_wrong));
            activity.back();
        }
    }
    private LessonUnit retrieveExtras(Bundle extras) throws Exception {
        if (extras == null) {
            throw new NullPointerException("extras cannot be null");
        }
        String header = getStringExtra(extras, "header", true);
        TextView slc_title = (TextView) activity.findViewById(R.id.slc_title);
        TextView slc_desc = (TextView) activity.findViewById(R.id.slc_desc);
        if (slc_title != null) {
            slc_title.setText(header);
        }
        if (slc_desc != null) {
            int week = Static.getWeek(getContext());
            if (week >= 0) {
                slc_desc.setText(week + " " + getString(R.string.school_week));
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
            case "practice": lessonUnit.type = getString(R.string.practice); break;
            case "lecture": lessonUnit.type = getString(R.string.lecture); break;
            case "lab": lessonUnit.type = getString(R.string.lab); break;
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
        String addedStr = Storage.file.perm.get(getContext(), "schedule_lessons#added#" + lessonUnit.cache_token, "");
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
                                case "practice": lessonUnit.type = getString(R.string.practice); break;
                                case "lecture": lessonUnit.type = getString(R.string.lecture); break;
                                case "lab": lessonUnit.type = getString(R.string.lab); break;
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
            void onProgress(int state);
            void onFailure(int state);
            void onSuccess(JSONObject json);
        }
        private static RequestHandle request = null;
        static boolean blocked = false;

        public static void search(final Context context, final String query, final ProgressBar progressBar, final response delegate){
            Log.v(TAG, "search | query=" + query);
            if (request != null) {
                request.cancel(true);
                request = null;
            }
            if (blocked) {
                blocked = false;
                progressBar.setVisibility(View.GONE);
                return;
            }
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
                    progressBar.setVisibility(View.GONE);
                    delegate.onFailure(state);
                }
                @Override
                public void onSuccess(JSONObject json) {
                    Log.v(TAG, "search | success");
                    progressBar.setVisibility(View.GONE);
                    delegate.onSuccess(json);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    request = requestHandle;
                }
            });
            progressBar.setVisibility(View.VISIBLE);
            scheduleLessons.search(query);
        }

    }

}
