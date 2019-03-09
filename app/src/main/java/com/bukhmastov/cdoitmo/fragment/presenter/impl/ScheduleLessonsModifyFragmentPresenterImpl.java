package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
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
import com.bukhmastov.cdoitmo.adapter.array.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
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
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import androidx.annotation.IdRes;

import static com.bukhmastov.cdoitmo.util.Thread.SLM;

public class ScheduleLessonsModifyFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements ScheduleLessonsModifyFragmentPresenter {

    private static final String TAG = "SLModifyFragment";
    private @TYPE String type;
    private Integer weekdayOriginal;
    private String customDayOriginal;
    private Integer weekday;
    private String customDay;
    private boolean blockTimeStart = false;
    private boolean blockTimeEnd = false;
    private boolean blockNextTeacherSearch = false;
    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Time.DEFAULT_LOCALE);

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
    NotificationMessage notificationMessage;
    @Inject
    Time time;

    public ScheduleLessonsModifyFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        thread.run(SLM, () -> {
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
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            fragment.close();
        });
    }

    private void display() throws Exception {
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
        weekday = getIntegerExtra(fragment.extras(), "weekday", true);
        weekdayOriginal = weekday;
        customDay = getStringExtra(fragment.extras(), "custom_day", true);
        customDayOriginal = customDay;

        switch (StringUtils.defaultIfNull(lesson.getType(), "")) {
            case "practice": lesson.setType(activity.getString(R.string.practice)); break;
            case "lecture": lesson.setType(activity.getString(R.string.lecture)); break;
            case "lab": lesson.setType(activity.getString(R.string.lab)); break;
            case "iws": lesson.setType(activity.getString(R.string.iws)); break;
        }

        thread.runOnUI(SLM, () -> {
            initHeader(time.getWeek(activity), title, lessonType);
            initTextField(R.id.lesson_title, lesson::getSubject, lesson::setSubject);
            initTime(lesson);
            initDay(lesson);
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
        TextView slcTitle = fragment.container().findViewById(R.id.slc_title);
        TextView slcDesc = fragment.container().findViewById(R.id.slc_desc);
        if (slcTitle != null) {
            slcTitle.setText(scheduleLessons.getScheduleHeader(title, lessonType));
        }
        if (slcDesc != null) {
            slcDesc.setText(scheduleLessons.getScheduleWeek(week));
        }
    }

    private void initTextField(@IdRes int resId, Supplier<String> supplier, Consumer<String> consumer) {
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
    }

    private void initAutoCompleteTextField(@IdRes int resId, Supplier<String> supplier, Consumer<String> consumer, ArrayAdapter arrayAdapter) {
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
    }

    private void initTime(SLesson lesson) {
        TextInputEditText lessonTimeStart = fragment.container().findViewById(R.id.lesson_time_start);
        TextInputEditText lessonTimeEnd = fragment.container().findViewById(R.id.lesson_time_end);
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
                    st = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + StringUtils.ldgZero(st_calendar.get(Calendar.MINUTE));
                    if (lessonTimeEnd.getText() == null || lessonTimeEnd.getText().toString().isEmpty()) {
                        Calendar nt_calendar = time.getCalendar();
                        nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                        blockTimeEnd = true;
                        String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + StringUtils.ldgZero(nt_calendar.get(Calendar.MINUTE));
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
                                String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + StringUtils.ldgZero(nt_calendar.get(Calendar.MINUTE));
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
                    et = et_calendar.get(Calendar.HOUR_OF_DAY) + ":" + StringUtils.ldgZero(et_calendar.get(Calendar.MINUTE));
                    if (lessonTimeStart.getText() == null || lessonTimeStart.getText().toString().isEmpty()) {
                        Calendar st_calendar = time.getCalendar();
                        st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                        blockTimeStart = true;
                        String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + StringUtils.ldgZero(st_calendar.get(Calendar.MINUTE));
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
                                String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + StringUtils.ldgZero(st_calendar.get(Calendar.MINUTE));
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
    }

    private void initDay(SLesson lesson) {
        Spinner lessonDateType = fragment.container().findViewById(R.id.lesson_date_type);
        ArrayAdapter<?> lessonDateTypeAdapter = ArrayAdapter.createFromResource(activity, R.array.lesson_date_type_titles, R.layout.spinner_simple);
        String[] typeValues = activity.getResources().getStringArray(R.array.lesson_date_type_values);
        lessonDateTypeAdapter.setDropDownViewResource(R.layout.spinner_center);
        lessonDateType.setAdapter(lessonDateTypeAdapter);
        lessonDateType.setSelection(weekday != null ? 0 : 1);
        lessonDateType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long selectedId) {
                toggleDay(lesson, typeValues[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        toggleDay(lesson, typeValues[weekday != null ? 0 : 1]);
    }

    private void toggleDay(SLesson lesson, String type) {
        if ("regular".equals(type)) {
            initDayRegular(lesson);
        } else {
            initDayDate();
        }
    }

    private void initDayRegular(SLesson lesson) {

        if (weekday == null) {
            weekday = 0;
        }
        customDay = null;

        fragment.container().findViewById(R.id.lesson_date_type_regular).setVisibility(View.VISIBLE);
        fragment.container().findViewById(R.id.lesson_date_type_day).setVisibility(View.GONE);

        Spinner paritySpinner = fragment.container().findViewById(R.id.lesson_week);
        ArrayAdapter<?> parityAdapter = ArrayAdapter.createFromResource(activity, R.array.week_types_titles, R.layout.spinner_simple);
        parityAdapter.setDropDownViewResource(R.layout.spinner_center);
        paritySpinner.setAdapter(parityAdapter);
        paritySpinner.setSelection((lesson.getParity() + 1) % 3);
        paritySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                String[] week_types_values = activity.getResources().getStringArray(R.array.week_types_values);
                lesson.setParity(Integer.parseInt(week_types_values[position]));
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] weekdayValues = activity.getResources().getStringArray(R.array.days_of_week_values);
        Spinner weekdaySpinner = fragment.container().findViewById(R.id.lesson_day_of_week);
        ArrayAdapter<?> weekdayAdapter = ArrayAdapter.createFromResource(activity, R.array.days_of_week_titles, R.layout.spinner_simple);
        weekdayAdapter.setDropDownViewResource(R.layout.spinner_center);
        weekdaySpinner.setAdapter(weekdayAdapter);
        weekdaySpinner.setSelection(weekday < weekdayValues.length ? weekday : 0);
        weekdaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                weekday = Integer.parseInt(weekdayValues[position]);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initDayDate() {

        if (customDay == null) {
            customDay = "";
        }
        weekday = null;

        fragment.container().findViewById(R.id.lesson_date_type_day).setVisibility(View.VISIBLE);
        fragment.container().findViewById(R.id.lesson_date_type_regular).setVisibility(View.GONE);

        TextInputEditText lessonDateDay = fragment.container().findViewById(R.id.lesson_date_day);
        lessonDateDay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                try {
                    Calendar calendar = time.getCalendar();
                    calendar.setTime(dateFormat.parse(value));
                    customDay = time.getScheduleCustomDayRaw(calendar);
                } catch (ParseException ignore) {
                    // wrong format provided
                    customDay = "";
                }
            }
        });
        if (StringUtils.isNotBlank(customDay)) {
            Date date = new Date(time.getScheduleCustomDayTimestamp(customDay));
            String text = dateFormat.format(date);
            lessonDateDay.setText(text);
        }
    }

    private void initTeacher(SLesson lesson) {
        AutoCompleteTextView teacherAutoComplete = fragment.container().findViewById(R.id.lesson_teacher);
        ProgressBar teacherProgressBar = fragment.container().findViewById(R.id.lesson_teacher_bar);
        TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
        TeacherSearch.TeacherSearchCallback teacherSearchCallback = new TeacherSearch.TeacherSearchCallback() {
            @Override
            public void onState(int state) {
                switch (state) {
                    case TeacherSearch.REJECTED_EMPTY:
                    case TeacherSearch.REJECTED:
                    case TeacherSearch.ACCEPTED: break;
                    case TeacherSearch.SEARCHING:
                        thread.runOnUI(SLM, () -> teacherProgressBar.setVisibility(View.VISIBLE));
                        break;
                    case TeacherSearch.NOT_FOUND:
                    case TeacherSearch.FOUND:
                        thread.runOnUI(SLM, () -> teacherProgressBar.setVisibility(View.GONE));
                        break;
                }
            }
            @Override
            public void onSuccess(STeachers teachers) {
                thread.runOnUI(SLM, () -> {
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
                thread.run(SLM, () -> {
                    String teacherName = s.toString().trim();
                    lesson.setTeacherName(teacherName);
                    lesson.setTeacherId("");
                    teacherSearch.search(teacherName, teacherSearchCallback);
                });
            }
        });
        teacherAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            thread.run(SLM, () -> {
                STeacher teacher = teacherPickerAdapter.getItem(position);
                if (teacher == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    return;
                }
                lesson.setTeacherName(teacher.getPerson());
                lesson.setTeacherId(teacher.getPersonId());
                thread.runOnUI(SLM, () -> {
                    blockNextTeacherSearch = true;
                    teacherAutoComplete.setTextKeepState(lesson.getTeacherName());
                    teacherAutoComplete.dismissDropDown();
                });
            });
        });
    }

    private void initAction(SLesson lesson, SLesson lessonOriginal, String query) {
        Button lessonCreateButton = fragment.container().findViewById(R.id.lesson_create_button);
        switch (type) {
            case CREATE: lessonCreateButton.setText(activity.getString(R.string.create)); break;
            case EDIT: lessonCreateButton.setText(activity.getString(R.string.save)); break;
        }
        lessonCreateButton.setOnClickListener(v -> {
            thread.run(SLM, () -> {
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
                if (weekday == null && StringUtils.isBlank(customDay)) {
                    log.v(TAG, "weekday and customDay are null");
                    notificationMessage.snackBar(activity, activity.getString(R.string.lesson_date_required));
                    return;
                }
                if (weekday == null) {
                    lesson.setParity(2);
                }
                if (StringUtils.isNotBlank(lesson.getType())) {
                    if (lesson.getType().toLowerCase().equals(activity.getString(R.string.lecture).toLowerCase())) lesson.setType("lecture");
                    if (lesson.getType().toLowerCase().equals(activity.getString(R.string.practice).toLowerCase())) lesson.setType("practice");
                    if (lesson.getType().toLowerCase().equals(activity.getString(R.string.lab).toLowerCase())) lesson.setType("lab");
                    if (lesson.getType().toLowerCase().equals(activity.getString(R.string.iws).toLowerCase())) lesson.setType("iws");
                }
                switch (type) {
                    case CREATE: {
                        if (scheduleLessonsHelper.createLesson(query, weekday, customDay, lesson, null)) {
                            thread.runOnUI(SLM, () -> {
                                tabHostPresenter.invalidateOnDemand();
                                fragment.close();
                            });
                        } else {
                            log.w(TAG, "failed to create lesson");
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                        break;
                    }
                    case EDIT: {
                        if (scheduleLessonsHelper.deleteLesson(query, weekdayOriginal, customDayOriginal, lessonOriginal, null) &&
                                scheduleLessonsHelper.createLesson(query, weekday, customDay, lesson, null)) {
                            thread.runOnUI(SLM, () -> {
                                tabHostPresenter.invalidateOnDemand();
                                fragment.close();
                            });
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
    }
    
    private String getStringExtra(Bundle extras, String key, boolean strict) throws Exception {
        if (extras.containsKey(key)) {
            return extras.getString(key);
        } else {
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return "";
            }
        }
    }
    
    private Integer getIntegerExtra(Bundle extras, String key, boolean strict) throws Exception {
        if (extras.containsKey(key)) {
            Serializable object = extras.getSerializable(key);
            if (object == null) {
                return null;
            }
            if (object instanceof Integer) {
                return (Integer) object;
            }
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return null;
            }
        } else {
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return null;
            }
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getThreadToken() {
        return SLM;
    }
}
