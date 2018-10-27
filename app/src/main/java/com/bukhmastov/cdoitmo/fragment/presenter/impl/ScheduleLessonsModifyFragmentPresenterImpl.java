package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsModifyFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.function.Supplier;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;
import com.bukhmastov.cdoitmo.object.TeacherSearch;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class ScheduleLessonsModifyFragmentPresenterImpl implements ScheduleLessonsModifyFragmentPresenter {

    private static final String TAG = "SLModifyFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private @TYPE String type;
    private Integer weekday;
    private boolean blockTimeStart = false;
    private boolean blockTimeEnd = false;
    private boolean blockNextTeacherSearch = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    ScheduleLessonsHelper scheduleLessonsHelper;
    @Inject
    TeacherSearch teacherSearch;
    @Inject
    ScheduleLessonsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    Storage storage;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleLessonsModifyFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
    }

    @Override
    public void onViewCreated() {
        thread.run(() -> {
            if (fragment.extras() == null) {
                log.e(TAG, "extras cannot be null");
                fragment.close();
                return;
            }
            type = fragment.extras().getString("action_type");
            if (type == null) {
                log.e(TAG, "type cannot be null");
                fragment.close();
                return;
            }
            switch (type) {
                case CREATE: activity.updateToolbar(activity, activity.getString(R.string.lesson_creation), R.drawable.ic_schedule_lessons); break;
                case EDIT: activity.updateToolbar(activity, activity.getString(R.string.lesson_editing), R.drawable.ic_schedule_lessons); break;
                default:
                    Exception exception = new Exception("got wrong type from arguments bundle: " + type);
                    log.wtf(exception);
                    throw exception;
            }
            display();
        }, throwable -> {
            log.exception(throwable);
            fragment.close();
        });
    }

    private void display() {
        thread.run(() -> {
            Serializable serializable = fragment.extras().getSerializable("lesson");
            if (!(serializable instanceof SLesson)) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                fragment.close();
                return;
            }
            SLesson lessonOriginal = (SLesson) serializable;
            SLesson lesson = new SLesson(lessonOriginal);
            String query = getStringExtra(fragment.extras(), "query", true);
            String lessonType = getStringExtra(fragment.extras(), "type", true);
            String title = getStringExtra(fragment.extras(), "title", true);
            weekday = getIntExtra(fragment.extras(), "weekday", true);

            switch (StringUtils.defaultIfNull(lesson.getType(), "")) {
                case "practice": lesson.setType(activity.getString(R.string.practice)); break;
                case "lecture": lesson.setType(activity.getString(R.string.lecture)); break;
                case "lab": lesson.setType(activity.getString(R.string.lab)); break;
                case "iws": lesson.setType(activity.getString(R.string.iws)); break;
            }

            initHeader(time.getWeek(activity), title, lessonType);
            initTextField(R.id.lesson_title, lesson::getSubject, lesson::setSubject);
            initTime(lesson);
            initWeek(lesson);
            initDayOfWeek();
            initAutoCompleteTextField(R.id.lesson_type, lesson::getType, lesson::setType, ArrayAdapter.createFromResource(activity, R.array.lessons_types, R.layout.spinner_simple_padding));
            initTextField(R.id.lesson_group, lesson::getGroup, lesson::setGroup);
            initTeacher(lesson);
            initTextField(R.id.lesson_room, lesson::getRoom, lesson::setRoom);
            initAutoCompleteTextField(R.id.lesson_building, lesson::getBuilding, lesson::setBuilding, ArrayAdapter.createFromResource(activity, R.array.buildings, R.layout.spinner_simple_padding));
            initAction(lesson, lessonOriginal, query);
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            fragment.close();
        });
    }

    private void initHeader(int week, String title, String lessonType) {
        thread.runOnUI(() -> {
            TextView slcTitle = fragment.container().findViewById(R.id.slc_title);
            TextView slcDesc = fragment.container().findViewById(R.id.slc_desc);
            if (slcTitle != null) {
                slcTitle.setText(scheduleLessons.getScheduleHeader(title, lessonType));
            }
            if (slcDesc != null) {
                slcDesc.setText(scheduleLessons.getScheduleWeek(week));
            }
        });
    }

    private void initTextField(@IdRes int resId, Supplier<String> supplier, Consumer<String> consumer) {
        thread.runOnUI(() -> {
            TextInputEditText editText = fragment.container().findViewById(resId);
            if (StringUtils.isNotBlank(supplier.get())) {
                editText.setText(supplier.get());
            }
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    consumer.accept(s.toString());
                }
            });
        });
    }

    private void initAutoCompleteTextField(@IdRes int resId, Supplier<String> supplier, Consumer<String> consumer, ArrayAdapter arrayAdapter) {
        thread.runOnUI(() -> {
            AutoCompleteTextView autoCompleteTextView = fragment.container().findViewById(resId);
            if (StringUtils.isNotBlank(supplier.get())) {
                autoCompleteTextView.setText(supplier.get());
            }
            autoCompleteTextView.setThreshold(1);
            autoCompleteTextView.setAdapter(arrayAdapter);
            autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    consumer.accept(s.toString());
                }
            });
        });
    }

    private void initTime(SLesson lesson) {
        thread.runOnUI(() -> {
            final TextInputEditText lessonTimeStart = fragment.container().findViewById(R.id.lesson_time_start);
            final TextInputEditText lessonTimeEnd = fragment.container().findViewById(R.id.lesson_time_end);
            if (StringUtils.isNotBlank(lesson.getTimeStart())) {
                lessonTimeStart.setText(lesson.getTimeStart());
            }
            if (StringUtils.isNotBlank(lesson.getTimeEnd())) {
                lessonTimeEnd.setText(lesson.getTimeEnd());
            }
            lessonTimeStart.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (blockTimeStart) {
                        blockTimeStart = false;
                        return;
                    }
                    String st = s.toString().trim();
                    Matcher t = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(st);
                    if (t.find()) {
                        Calendar st_calendar = time.getCalendar();
                        st_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t.group(1)));
                        st_calendar.set(Calendar.MINUTE, Integer.parseInt(t.group(2)));
                        st_calendar.set(Calendar.SECOND, 0);
                        st = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + textUtils.ldgZero(st_calendar.get(Calendar.MINUTE));
                        if (lessonTimeEnd.getText() == null || lessonTimeEnd.getText().toString().isEmpty()) {
                            Calendar nt_calendar = time.getCalendar();
                            nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                            blockTimeEnd = true;
                            String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + textUtils.ldgZero(nt_calendar.get(Calendar.MINUTE));
                            lesson.setTimeEnd(insert);
                            int selection = lessonTimeEnd.getSelectionStart();
                            lessonTimeEnd.setText(insert);
                            try {
                                lessonTimeEnd.setSelection(selection);
                            } catch (Exception ignore) {
                                // ignore
                            }
                        } else {
                            String nt = lessonTimeEnd.getText().toString();
                            Matcher next_time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(nt);
                            if (next_time.find()) {
                                Calendar nt_calendar = time.getCalendar();
                                nt_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(next_time.group(1)));
                                nt_calendar.set(Calendar.MINUTE, Integer.parseInt(next_time.group(2)));
                                nt_calendar.set(Calendar.SECOND, 0);
                                if (nt_calendar.getTimeInMillis() <= st_calendar.getTimeInMillis()) {
                                    nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                                    blockTimeEnd = true;
                                    String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + textUtils.ldgZero(nt_calendar.get(Calendar.MINUTE));
                                    lesson.setTimeEnd(insert);
                                    int selection = lessonTimeEnd.getSelectionStart();
                                    lessonTimeEnd.setText(insert);
                                    try {
                                        lessonTimeEnd.setSelection(selection);
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                }
                            }
                        }
                        if (!s.toString().trim().equals(st)) {
                            blockTimeStart = true;
                            int selection = lessonTimeStart.getSelectionStart();
                            lessonTimeStart.setText(st);
                            try {
                                lessonTimeStart.setSelection(selection);
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                    }
                    lesson.setTimeStart(st);
                }
            });
            lessonTimeEnd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (blockTimeEnd) {
                        blockTimeEnd = false;
                        return;
                    }
                    String et = s.toString().trim();
                    Matcher t = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(et);
                    if (t.find()) {
                        Calendar et_calendar = time.getCalendar();
                        et_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t.group(1)));
                        et_calendar.set(Calendar.MINUTE, Integer.parseInt(t.group(2)));
                        et_calendar.set(Calendar.SECOND, 0);
                        et = et_calendar.get(Calendar.HOUR_OF_DAY) + ":" + textUtils.ldgZero(et_calendar.get(Calendar.MINUTE));
                        if (lessonTimeStart.getText() == null || lessonTimeStart.getText().toString().isEmpty()) {
                            Calendar st_calendar = time.getCalendar();
                            st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                            blockTimeStart = true;
                            String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + textUtils.ldgZero(st_calendar.get(Calendar.MINUTE));
                            lesson.setTimeStart(insert);
                            int selection = lessonTimeStart.getSelectionStart();
                            lessonTimeStart.setText(insert);
                            try {
                                lessonTimeStart.setSelection(selection);
                            } catch (Exception ignore) {
                                // ignore
                            }
                        } else {
                            String st = lessonTimeStart.getText().toString();
                            Matcher previous_time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(st);
                            if (previous_time.find()) {
                                Calendar st_calendar = time.getCalendar();
                                st_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(previous_time.group(1)));
                                st_calendar.set(Calendar.MINUTE, Integer.parseInt(previous_time.group(2)));
                                st_calendar.set(Calendar.SECOND, 0);
                                if (st_calendar.getTimeInMillis() >= et_calendar.getTimeInMillis()) {
                                    st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                                    blockTimeStart = true;
                                    String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + textUtils.ldgZero(st_calendar.get(Calendar.MINUTE));
                                    lesson.setTimeStart(insert);
                                    int selection = lessonTimeStart.getSelectionStart();
                                    lessonTimeStart.setText(insert);
                                    try {
                                        lessonTimeStart.setSelection(selection);
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                }
                            }
                        }
                        if (!s.toString().trim().equals(et)) {
                            blockTimeEnd = true;
                            int selection = lessonTimeEnd.getSelectionStart();
                            lessonTimeEnd.setText(et);
                            try {
                                lessonTimeEnd.setSelection(selection);
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                    }
                    lesson.setTimeEnd(et);
                }
            });
        });
    }

    private void initWeek(SLesson lesson) {
        thread.runOnUI(() -> {
            Spinner lesson_week = fragment.container().findViewById(R.id.lesson_week);
            ArrayAdapter<?> lesson_week_adapter = ArrayAdapter.createFromResource(activity, R.array.week_types_titles, R.layout.spinner_simple);
            lesson_week_adapter.setDropDownViewResource(R.layout.spinner_center);
            lesson_week.setAdapter(lesson_week_adapter);
            lesson_week.setSelection((lesson.getParity() + 1) % 3);
            lesson_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                    String[] week_types_values = activity.getResources().getStringArray(R.array.week_types_values);
                    lesson.setParity(Integer.parseInt(week_types_values[position]));
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void initDayOfWeek() {
        thread.runOnUI(() -> {
            final String[] week_types_values = activity.getResources().getStringArray(R.array.days_of_week_values);
            Spinner lesson_day_of_week = fragment.container().findViewById(R.id.lesson_day_of_week);
            ArrayAdapter<?> lesson_day_of_week_adapter = ArrayAdapter.createFromResource(activity, R.array.days_of_week_titles, R.layout.spinner_simple);
            lesson_day_of_week_adapter.setDropDownViewResource(R.layout.spinner_center);
            lesson_day_of_week.setAdapter(lesson_day_of_week_adapter);
            lesson_day_of_week.setSelection(weekday < week_types_values.length ? weekday : 0);
            lesson_day_of_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                    weekday = Integer.parseInt(week_types_values[position]);
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void initTeacher(SLesson lesson) {
        thread.runOnUI(() -> {
            final AutoCompleteTextView teacherAutoComplete = fragment.container().findViewById(R.id.lesson_teacher);
            final ProgressBar teacherProgressBar = fragment.container().findViewById(R.id.lesson_teacher_bar);
            final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
            final TeacherSearch.TeacherSearchCallback teacherSearchCallback = new TeacherSearch.TeacherSearchCallback() {
                @Override
                public void onState(int state) {
                    switch (state) {
                        case TeacherSearch.REJECTED_EMPTY:
                        case TeacherSearch.REJECTED:
                        case TeacherSearch.ACCEPTED: break;
                        case TeacherSearch.SEARCHING:
                            thread.runOnUI(() -> teacherProgressBar.setVisibility(View.VISIBLE));
                            break;
                        case TeacherSearch.NOT_FOUND:
                        case TeacherSearch.FOUND:
                            thread.runOnUI(() -> teacherProgressBar.setVisibility(View.GONE));
                            break;
                    }
                }
                @Override
                public void onSuccess(STeachers teachers) {
                    thread.runOnUI(() -> {
                        teacherPickerAdapter.clear();
                        teacherAutoComplete.dismissDropDown();
                        if (teachers == null) {
                            return;
                        }
                        teacherPickerAdapter.addAll(teachers.getTeachers());
                        teacherPickerAdapter.addTeachers(teachers.getTeachers());
                        if (teachers.getTeachers().size() > 0) {
                            teacherAutoComplete.showDropDown();
                        }
                    });
                }
            };
            teacherAutoComplete.setThreshold(1);
            teacherAutoComplete.setText(StringUtils.isNotBlank(lesson.getTeacherName()) ? lesson.getTeacherName() : "");
            teacherAutoComplete.setAdapter(teacherPickerAdapter);
            teacherAutoComplete.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (blockNextTeacherSearch) {
                        blockNextTeacherSearch = false;
                        return;
                    }
                    String teacherName = s.toString().trim();
                    lesson.setTeacherName(teacherName);
                    lesson.setTeacherId("");
                    teacherSearch.search(teacherName, teacherSearchCallback);
                }
            });
            teacherAutoComplete.setOnItemClickListener((parent, view, position, id) -> thread.run(() -> {
                STeacher teacher = teacherPickerAdapter.getItem(position);
                if (teacher == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    return;
                }
                lesson.setTeacherName(teacher.getPerson());
                lesson.setTeacherId(teacher.getPersonId());
                blockNextTeacherSearch = true;
                teacherAutoComplete.setTextKeepState(lesson.getTeacherName());
                teacherAutoComplete.dismissDropDown();
            }));
        });
    }

    private void initAction(SLesson lesson, SLesson lessonOriginal, String query) {
        thread.runOnUI(() -> {
            Button lessonCreateButton = fragment.container().findViewById(R.id.lesson_create_button);
            switch (type) {
                case CREATE: lessonCreateButton.setText(activity.getString(R.string.create)); break;
                case EDIT: lessonCreateButton.setText(activity.getString(R.string.save)); break;
            }
            lessonCreateButton.setOnClickListener(v -> {
                thread.run(() -> {
                    log.v(TAG, "create_button clicked");
                    if (StringUtils.isBlank(lesson.getSubject())) {
                        log.v(TAG, "lesson.subject required");
                        notificationMessage.snackBar(activity, activity.getString(R.string.lesson_title_required));
                        return;
                    }
                    if (StringUtils.isBlank(lesson.getTimeStart())) {
                        log.v(TAG, "lesson.timeStart required");
                        notificationMessage.snackBar(activity, activity.getString(R.string.lesson_time_start_required));
                        return;
                    }
                    if (StringUtils.isNotBlank(lesson.getType())) {
                        if (lesson.getType().toLowerCase().equals(activity.getString(R.string.lecture).toLowerCase())) lesson.setType("lecture");
                        if (lesson.getType().toLowerCase().equals(activity.getString(R.string.practice).toLowerCase())) lesson.setType("practice");
                        if (lesson.getType().toLowerCase().equals(activity.getString(R.string.lab).toLowerCase())) lesson.setType("lab");
                        if (lesson.getType().toLowerCase().equals(activity.getString(R.string.iws).toLowerCase())) lesson.setType("iws");
                    }
                    switch (type) {
                        case CREATE: {
                            if (scheduleLessonsHelper.createLesson(query, weekday, lesson, null)) {
                                tabHostPresenter.invalidateOnDemand();
                                fragment.close();
                            } else {
                                log.w(TAG, "failed to create lesson");
                                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                            break;
                        }
                        case EDIT: {
                            if (scheduleLessonsHelper.deleteLesson(query, weekday, lessonOriginal, null) && scheduleLessonsHelper.createLesson(query, weekday, lesson, null)) {
                                tabHostPresenter.invalidateOnDemand();
                                fragment.close();
                            } else {
                                log.w(TAG, "failed to create lesson");
                                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                            break;
                        }
                    }
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    fragment.close();
                });
            });
        });
    }
    
    private String getStringExtra(Bundle extras, String key, boolean strict) throws Exception {
        if (extras.containsKey(key)) {
            String value = extras.getString(key);
            if (value != null) {
                return value;
            } else {
                if (strict) {
                    throw new Exception("Missed extra for key: " + key);
                } else {
                    return "";
                }
            }
        } else {
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return "";
            }
        }
    }
    
    private int getIntExtra(Bundle extras, String key, boolean strict) throws Exception {
        if (extras.containsKey(key)) {
            return extras.getInt(key);
        } else {
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return 0;
            }
        }
    }
}
