package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import androidx.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.rva.RVADualValue;
import com.bukhmastov.cdoitmo.model.rva.RVAExams;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExam;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.DateUtils;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class ScheduleExamsRVA extends RVA<RVAExams> {

    private static final String TAG = "ScheduleExamsRVA";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EXAM = 1;
    private static final int TYPE_UPDATE_TIME = 2;
    private static final int TYPE_NO_EXAMS = 3;
    private static final int TYPE_PICKER_HEADER = 4;
    private static final int TYPE_PICKER_ITEM = 5;
    private static final int TYPE_PICKER_NO_TEACHERS = 6;

    public static Pattern patternBrokenDate = Pattern.compile("^(\\d{1,2})(\\s\\S*)(.*)$", Pattern.CASE_INSENSITIVE);

    @Inject
    Context context;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    StoragePref storagePref;
    @Inject
    Time time;
    @Inject
    DateUtils dateUtils;

    private final String query;
    private final Collection<SSubject> events;
    private final int mode; // 0 - exam, 1 - credit
    private String type;

    public ScheduleExamsRVA(SExams data, Collection<SSubject> events, int mode) {
        super();
        AppComponentProvider.getComponent().inject(this);
        this.query = data.getQuery();
        this.events = events;
        this.mode = mode;
        this.type = data.getType();
        addItems(entity2dataset(data, events));
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
    @SuppressWarnings("unchecked")
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
                bindNoExams(container);
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

    private void bindHeader(View container, Item<RVADualValue> item) {
        try {
            String title = item.data.getFirst();
            String week = item.data.getSecond();
            TextView headerView = container.findViewById(R.id.schedule_lessons_header);
            TextView weekView = container.findViewById(R.id.schedule_lessons_week);
            if (StringUtils.isNotBlank(title)) {
                headerView.setText(title);
            } else {
                ((ViewGroup) headerView.getParent()).removeView(headerView);
            }
            if (StringUtils.isNotBlank(week)) {
                weekView.setText(week);
            } else {
                ((ViewGroup) weekView.getParent()).removeView(weekView);
            }
            View createAction = container.findViewById(R.id.schedule_lessons_create);
            if (createAction != null) {
                createAction.setVisibility(View.GONE);
            }
            tryRegisterClickListener(container, R.id.schedule_lessons_menu, null);
            tryRegisterClickListener(container, R.id.schedule_lessons_share, new RVAExams(new ArrayList<>(events)));
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindExam(View container, Item<SSubject> item) {
        try {
            SSubject subject = item.data;
            final String desc;
            final boolean isTouchIconEnabled;
            switch (type) {
                case "group": {
                    desc = subject.getTeacherName();
                    isTouchIconEnabled = StringUtils.isNotBlank(subject.getTeacherId()) || StringUtils.isNotBlank(subject.getTeacherName());
                    break;
                }
                case "teacher": {
                    desc = subject.getGroup();
                    isTouchIconEnabled = StringUtils.isNotBlank(subject.getGroup());
                    break;
                }
                default: {
                    desc = null;
                    isTouchIconEnabled = false;
                    break;
                }
            }
            // title and description
            ((TextView) container.findViewById(R.id.exam_header)).setText(subject.getSubject().toUpperCase());
            if (StringUtils.isNotBlank(desc)) {
                ((TextView) container.findViewById(R.id.exam_desc)).setText(desc);
                container.findViewById(R.id.exam_desc).setVisibility(View.VISIBLE);
            } else {
                container.findViewById(R.id.exam_desc).setVisibility(View.GONE);
            }
            // badges (actually, only one)
            View examTouchIcon = container.findViewById(R.id.exam_touch_icon);
            examTouchIcon.setVisibility(isTouchIconEnabled ? View.VISIBLE : View.GONE);
            if (isTouchIconEnabled) {
                tryRegisterClickListener(container, R.id.exam_touch_icon, new RVAExams(subject));
            }
            // advice
            boolean isAdviceExists = false;
            if (subject.getAdvice() != null) {
                SExam advice = subject.getAdvice();
                String dateFormatAppend = "";
                if (StringUtils.isNotBlank(advice.getDate())) {
                    String date = advice.getDate();
                    if (StringUtils.isNotBlank(advice.getTime())) {
                        date += " " + advice.getTime();
                        dateFormatAppend = " HH:mm";
                    }
                    String place = "";
                    if (StringUtils.isNotBlank(advice.getRoom())) {
                        place += advice.getRoom();
                    }
                    if (StringUtils.isNotBlank(advice.getBuilding())) {
                        place += " " + advice.getBuilding();
                    }
                    place = place.trim();
                    if (StringUtils.isNotBlank(place)) {
                        place = context.getString(R.string.place) + ": " + place;
                    }
                    ((TextView) container.findViewById(R.id.exam_info_advice_date)).setText(cuteDate(date, dateFormatAppend));
                    TextView placeView = container.findViewById(R.id.exam_info_advice_place);
                    if (!place.isEmpty()) {
                        placeView.setText(place);
                        placeView.setVisibility(View.VISIBLE);
                    } else {
                        placeView.setVisibility(View.GONE);
                    }
                    isAdviceExists = true;
                }
            }
            // exam
            boolean isExamExists = false;
            if (subject.getExam() != null) {
                SExam exam = subject.getExam();
                String dateFormatAppend = "";
                if (StringUtils.isNotBlank(exam.getDate())) {
                    String date = exam.getDate();
                    if (StringUtils.isNotBlank(exam.getTime())) {
                        date += " " + exam.getTime();
                        dateFormatAppend = " HH:mm";
                    }
                    String place = "";
                    if (StringUtils.isNotBlank(exam.getRoom())) {
                        place += exam.getRoom();
                    }
                    if (StringUtils.isNotBlank(exam.getBuilding())) {
                        place += " " + exam.getBuilding();
                    }
                    place = place.trim();
                    if (StringUtils.isNotBlank(place)) {
                        place = context.getString(R.string.place) + ": " + place;
                    }
                    ((TextView) container.findViewById(R.id.exam_info_exam_title)).setText(getTitleResource(subject.getType()));
                    ((TextView) container.findViewById(R.id.exam_info_exam_date)).setText(cuteDate(date, dateFormatAppend));
                    TextView placeView = container.findViewById(R.id.exam_info_exam_place);
                    if (!place.isEmpty()) {
                        placeView.setText(place);
                        placeView.setVisibility(View.VISIBLE);
                    } else {
                        placeView.setVisibility(View.GONE);
                    }
                    isExamExists = true;
                }
            }
            container.findViewById(R.id.exam_info_advice).setVisibility(isAdviceExists ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.exam_info_exam).setVisibility(isExamExists ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.separator_small).setVisibility((isAdviceExists && !isExamExists) || (!isAdviceExists && isExamExists) ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.exam_info).setVisibility(isAdviceExists || isExamExists ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindUpdateTime(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.update_time)).setText(StringUtils.isNotBlank(item.data.getValue()) ? item.data.getValue() : Static.GLITCH);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindNoExams(View container) {
        try {
            String info = "";
            Calendar calendar = time.getCalendar();
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            if ((month >= Calendar.SEPTEMBER && (month <= Calendar.DECEMBER && day < 20)) || (month >= Calendar.FEBRUARY && (month <= Calendar.MAY && day < 20))) {
                info = "\n" + context.getString(R.string.no_exams_info);
            }
            ((TextView) container.findViewById(R.id.ntd_text)).setText((mode == 0 ? context.getText(R.string.no_exams) : context.getText(R.string.no_credits)) + info);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindPickerHeader(View container, Item<RVASingleValue> item) {
        try {
            String query = item.data.getValue();
            String text;
            if (query == null || query.isEmpty()) {
                text = context.getString(R.string.choose_teacher) + ":";
            } else {
                text = context.getString(R.string.on_search_for) + " \"" + query + "\" " + context.getString(R.string.teachers_found) + ":";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_header)).setText(text);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindPickerItem(View container, Item<STeacher> item) {
        try {
            String teacher = item.data.getPerson();
            if (StringUtils.isNotBlank(item.data.getPost())) {
                teacher += " (" + item.data.getPost() + ")";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_title)).setText(teacher);
            tryRegisterClickListener(container, R.id.teacher_picker_item, new RVAExams(item.data));
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindPickerNoTeachers(View container, Item<RVASingleValue> item) {
        try {
            String query = item.data.getValue();
            String text;
            if (query == null || query.isEmpty()) {
                text = context.getString(R.string.no_teachers);
            } else {
                text = context.getString(R.string.on_search_for) + " \"" + query + "\" " + context.getString(R.string.no_teachers).toLowerCase();
            }
            ((TextView) container.findViewById(R.id.ntd_text)).setText(text);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(SExams schedule, Collection<SSubject> events) {
        final ArrayList<Item> dataset = new ArrayList<>();
        // check
        if (!ScheduleExams.TYPE.equals(schedule.getScheduleType())) {
            return dataset;
        }
        // teacher picker mode
        if ("teachers".equals(schedule.getType())) {
            if (schedule.getTeachers() != null && CollectionUtils.isNotEmpty(schedule.getTeachers().getTeachers())) {
                dataset.add(new Item<>(TYPE_PICKER_HEADER, new RVASingleValue(query)));
                for (STeacher teacher : schedule.getTeachers().getTeachers()) {
                    dataset.add(new Item<>(TYPE_PICKER_ITEM, teacher));
                }
            } else {
                dataset.add(new Item<>(TYPE_PICKER_NO_TEACHERS, new RVASingleValue(query)));
            }
            return dataset;
        }
        // regular schedule mode
        // header
        dataset.add(new Item<>(TYPE_HEADER, new RVADualValue(
                scheduleExams.getScheduleHeader(schedule.getTitle(), schedule.getType()),
                scheduleExams.getScheduleWeek(-1)
        )));
        // schedule
        int exams_count = 0;
        for (SSubject subject : events) {
            dataset.add(new Item<>(TYPE_EXAM, subject));
            exams_count++;
        }
        if (exams_count == 0) {
            dataset.add(new Item(TYPE_NO_EXAMS));
        } else {
            // update time
            dataset.add(new Item<>(TYPE_UPDATE_TIME, new RVASingleValue(context.getString(R.string.update_date) + " " + time.getUpdateTime(context, schedule.getTimestamp()))));
        }
        // that's all folks
        return dataset;
    }

    private String cuteDate(String date, String date_format_append) {
        try {
            String date_format = "dd.MM.yyyy" + date_format_append;
            if (isValidFormat(date, date_format)) {
                date = dateUtils.cuteDate(context, date_format, date);
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
                if (isValidFormat(date, date_format)) {
                    date = cuteDateWOYear(date_format, date);
                }
            }
        } catch (Exception ignore) {/* ignore */}
        return date;
    }

    private String cuteDateWOYear(String date_format, String date_string) throws Exception {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, StringUtils.getLocale(context, storagePref));
        Calendar date = time.getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(time.getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(StringUtils.ldgZero(date.get(Calendar.HOUR_OF_DAY)))
                .append(":")
                .append(StringUtils.ldgZero(date.get(Calendar.MINUTE)))
                .toString();
    }

    private boolean isValidFormat(String value, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, StringUtils.getLocale(context, storagePref));
            Date date = sdf.parse(value);
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
            return date != null;
        } catch (Exception e) {
            return false;
        }
    }

    private int getTitleResource(String type) {
        if ("diffcredit".equals(type)) {
            return R.string.diffcredit;
        }
        if ("credit".equals(type)) {
            return R.string.credit;
        }
        return R.string.exam;
    }
}
