package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.util.SparseIntArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.rva.RVADualValue;
import com.bukhmastov.cdoitmo.model.rva.RVALessons;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

public class ScheduleLessonsRVA extends RVA<RVALessons> {

    private static final String TAG = "ScheduleLessonsRVA";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DAY = 1;
    private static final int TYPE_LESSON_TOP = 2;
    private static final int TYPE_LESSON_REGULAR = 3;
    private static final int TYPE_LESSON_BOTTOM = 4;
    private static final int TYPE_LESSON_SINGLE = 5;
    private static final int TYPE_NOTIFICATION = 6;
    private static final int TYPE_UPDATE_TIME = 7;
    private static final int TYPE_NO_LESSONS = 8;
    private static final int TYPE_PICKER_HEADER = 9;
    private static final int TYPE_PICKER_ITEM = 10;
    private static final int TYPE_PICKER_NO_TEACHERS = 11;

    private final Context context;

    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    ScheduleLessonsHelper scheduleLessonsHelper;
    @Inject
    Time time;

    private final String type;
    private final String query;
    private final int parity;
    private final ArrayList<SDay> days = new ArrayList<>();
    private final SparseIntArray daysPositions = new SparseIntArray();
    private final String reducedLessonMode;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1, colorScheduleFlagIwsBG = -1;

    public ScheduleLessonsRVA(Context context, SLessons data, int parity, int weekday, String reducedLessonMode) {
        super();
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        this.type = data.getType();
        this.query = data.getQuery();
        this.parity = parity;
        this.reducedLessonMode = reducedLessonMode;
        addItems(entity2dataset(data, weekday));
    }

    public int getDayPosition(int weekday) {
        return daysPositions.get(weekday, -1);
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_schedule_both_header; break;
            case TYPE_DAY: layout = R.layout.layout_schedule_lessons_day; break;
            case TYPE_LESSON_TOP: layout = R.layout.layout_schedule_lessons_item_top; break;
            case TYPE_LESSON_REGULAR: layout = R.layout.layout_schedule_lessons_item_regular; break;
            case TYPE_LESSON_BOTTOM: layout = R.layout.layout_schedule_lessons_item_bottom; break;
            case TYPE_LESSON_SINGLE: layout = R.layout.layout_schedule_lessons_item_single; break;
            case TYPE_NOTIFICATION: layout = R.layout.layout_schedule_lessons_notification; break;
            case TYPE_UPDATE_TIME: layout = R.layout.layout_schedule_both_update_time; break;
            case TYPE_NO_LESSONS: layout = R.layout.state_nothing_to_display_compact; break;
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
            case TYPE_DAY: {
                bindDay(container, item);
                break;
            }
            case TYPE_LESSON_TOP:
            case TYPE_LESSON_REGULAR:
            case TYPE_LESSON_BOTTOM:
            case TYPE_LESSON_SINGLE: {
                bindLesson(container, item);
                break;
            }
            case TYPE_NOTIFICATION: {
                bindNotification(container, item);
                break;
            }
            case TYPE_UPDATE_TIME: {
                bindUpdateTime(container, item);
                break;
            }
            case TYPE_NO_LESSONS: {
                bindNoLessons(container);
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
            tryRegisterClickListener(container, R.id.schedule_lessons_menu, null);
            tryRegisterClickListener(container, R.id.schedule_lessons_share, new RVALessons(days));
            tryRegisterClickListener(container, R.id.schedule_lessons_create, new RVALessons(parity));
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindDay(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.day_title)).setText(StringUtils.isNotBlank(item.data.getValue()) ? item.data.getValue() : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindLesson(View container, Item<SLesson> item) {
        try {
            SLesson lesson = item.data;
            int weekday = (int) item.extras.get("weekday");
            boolean isReduced = "reduced".equals(lesson.getCdoitmoType());
            boolean isCompact = isReduced && "compact".equals(reducedLessonMode);
            boolean isSynthetic = "synthetic".equals(lesson.getCdoitmoType());
            float alpha = isReduced ? 0.3F : 1F;
            // reduced and compact
            container.findViewById(R.id.time).setAlpha(alpha);
            container.findViewById(R.id.data).setAlpha(alpha);
            ((TextView) container.findViewById(R.id.lesson_title)).setMaxLines(isCompact ? 1 : 10);
            container.findViewById(R.id.lesson_time_icon).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_time_end).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_desc).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_flags).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_meta).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            // badges
            container.findViewById(R.id.lesson_reduced_icon).setVisibility(isReduced ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.lesson_synthetic_icon).setVisibility(isSynthetic ? View.VISIBLE : View.GONE);
            tryRegisterClickListener(container, R.id.lesson_touch_icon, new RVALessons(lesson, weekday));
            // title and time
            ((TextView) container.findViewById(R.id.lesson_title)).setText(lesson.getSubject());
            ((TextView) container.findViewById(R.id.lesson_time_start)).setText(lesson.getTimeStart());
            ((TextView) container.findViewById(R.id.lesson_time_end)).setText(lesson.getTimeEnd());
            // desc
            TextView lessonDesc = container.findViewById(R.id.lesson_desc);
            String desc = getLessonDesc(lesson, type);
            if (StringUtils.isNotBlank(desc)) {
                lessonDesc.setText(desc);
            } else {
                lessonDesc.setVisibility(View.GONE);
            }
            // flags
            ViewGroup lessonFlags = container.findViewById(R.id.lesson_flags);
            lessonFlags.removeAllViews();
            if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Color.resolve(context, R.attr.colorScheduleFlagTEXT);
            if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Color.resolve(context, R.attr.colorScheduleFlagCommonBG);
            if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Color.resolve(context, R.attr.colorScheduleFlagPracticeBG);
            if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Color.resolve(context, R.attr.colorScheduleFlagLectureBG);
            if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Color.resolve(context, R.attr.colorScheduleFlagLabBG);
            if (colorScheduleFlagIwsBG == -1) colorScheduleFlagIwsBG = Color.resolve(context, R.attr.colorScheduleFlagIwsBG);
            if (StringUtils.isNotBlank(lesson.getType())) {
                switch (lesson.getType()) {
                    case "practice": lessonFlags.addView(getFlag(context.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG)); break;
                    case "lecture": lessonFlags.addView(getFlag(context.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG)); break;
                    case "lab": lessonFlags.addView(getFlag(context.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG)); break;
                    case "iws": lessonFlags.addView(getFlag(context.getString(R.string.iws), colorScheduleFlagTEXT, colorScheduleFlagIwsBG)); break;
                    default: lessonFlags.addView(getFlag(lesson.getType(), colorScheduleFlagTEXT, colorScheduleFlagCommonBG)); break;
                }
            }
            if (parity == 2 && (lesson.getParity() == 0 || lesson.getParity() == 1)) {
                lessonFlags.addView(getFlag(lesson.getParity() == 0 ? context.getString(R.string.tab_even) : context.getString(R.string.tab_odd), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
            }
            // meta
            TextView lessonMeta = container.findViewById(R.id.lesson_meta);
            String meta = getLessonMeta(lesson, type);
            if (StringUtils.isNotBlank(meta)) {
                lessonMeta.setText(meta);
            } else {
                lessonMeta.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNotification(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.lessons_warning)).setText(StringUtils.isNotBlank(item.data.getValue()) ? item.data.getValue() : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindUpdateTime(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.update_time)).setText(StringUtils.isNotBlank(item.data.getValue()) ? item.data.getValue() : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoLessons(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_lessons);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindPickerHeader(View container, Item<RVASingleValue> item) {
        try {
            String query = item.data.getValue();
            String text;
            if (StringUtils.isBlank(query)) {
                text = context.getString(R.string.choose_teacher) + ":";
            } else {
                text = context.getString(R.string.on_search_for) + " \"" + query + "\" " + context.getString(R.string.teachers_found) + ":";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_header)).setText(text);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindPickerItem(View container, Item<STeacher> item) {
        try {
            String teacher = item.data.getPerson();
            if (StringUtils.isNotBlank(item.data.getPost())) {
                teacher += " (" + item.data.getPost() + ")";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_title)).setText(teacher);
            tryRegisterClickListener(container, R.id.teacher_picker_item, new RVALessons(item.data));
        } catch (Exception e) {
            log.exception(e);
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
            log.exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(SLessons schedule, int weekday) {
        final ArrayList<Item> dataset = new ArrayList<>();
        // check
        if (!ScheduleLessons.TYPE.equals(schedule.getScheduleType())) {
            return dataset;
        }
        // teacher picker mode
        if (schedule.getType().equals("teachers")) {
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
        int position = 0;
        // header
        dataset.add(new Item<>(TYPE_HEADER, new RVADualValue(
                scheduleLessons.getScheduleHeader(schedule.getTitle(), schedule.getType()),
                scheduleLessons.getScheduleWeek(weekday)
        )));
        position++;
        // schedule
        int lessonsCount = 0;
        for (SDay day : schedule.getSchedule()) {
            if (day == null || CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            TreeSet<SLesson> lessons = scheduleLessonsHelper.filterAndSortLessons(day.getLessons(), parity, "hidden".equals(reducedLessonMode));
            if (CollectionUtils.isEmpty(lessons)) {
                continue;
            }
            days.add(new SDay(day.getWeekday(), new ArrayList<>(lessons)));
            daysPositions.append(day.getWeekday(), position++);
            dataset.add(new Item<>(TYPE_DAY, new RVASingleValue(getDayTitle(day))));
            int lessonsLength = lessons.size();
            int index = 0;
            for (SLesson lesson : lessons) {
                int type = TYPE_LESSON_REGULAR;
                if (index == 0) {
                    if (lessonsLength - 1 == 0) {
                        type = TYPE_LESSON_SINGLE;
                    } else {
                        type = TYPE_LESSON_TOP;
                    }
                } else if (index == lessonsLength - 1) {
                    type = TYPE_LESSON_BOTTOM;
                }
                Item item = new Item<>(type, lesson);
                item.extras.put("weekday", day.getWeekday());
                dataset.add(item);
                index++;
                position++;
                lessonsCount++;
            }
        }
        if (lessonsCount == 0) {
            dataset.add(new Item(TYPE_NO_LESSONS));
        } else {
            Calendar calendar = time.getCalendar();
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            if (month == Calendar.AUGUST && day > 21 || month == Calendar.SEPTEMBER && day < 21 || month == Calendar.JANUARY && day > 14 || month == Calendar.FEBRUARY && day < 14) {
                dataset.add(new Item<>(TYPE_NOTIFICATION, new RVASingleValue(context.getString(R.string.schedule_lessons_unstable_warning))));
            }
            dataset.add(new Item<>(TYPE_UPDATE_TIME, new RVASingleValue(context.getString(R.string.update_date) + " " + time.getUpdateTime(context, schedule.getTimestamp()))));
        }
        // that's all folks
        return dataset;
    }

    private String getDayTitle(SDay day) {
        switch (day.getWeekday()) {
            case 0: return context.getString(R.string.monday);
            case 1: return context.getString(R.string.tuesday);
            case 2: return context.getString(R.string.wednesday);
            case 3: return context.getString(R.string.thursday);
            case 4: return context.getString(R.string.friday);
            case 5: return context.getString(R.string.saturday);
            case 6: return context.getString(R.string.sunday);
            default:
                /*TODO implement when isu will be ready
                расписание из ису, когда есть расписания на определенный день
                if ("date".equals(day.getType()) && StringUtils.isNotBlank(day.getTitle())) {
                    return day.getTitle();
                }
                */
                return context.getString(R.string.unknown_day);
        }
    }

    private String getLessonDesc(SLesson lesson, String type) {
        switch (type) {
            case "group": return lesson.getTeacherName();
            case "teacher": return lesson.getGroup();
            case "mine":
            case "room": {
                if (StringUtils.isBlank(lesson.getGroup())) {
                    return lesson.getTeacherName();
                }
                String desc = lesson.getGroup();
                if (StringUtils.isNotBlank(lesson.getTeacherName())) {
                    desc += " (" + lesson.getTeacherName() + ")";
                }
                return desc;
            }
        }
        return null;
    }

    private String getLessonMeta(SLesson lesson, String type) {
        switch (type) {
            case "mine":
            case "group":
            case "teacher": {
                if (StringUtils.isBlank(lesson.getRoom())) {
                    return lesson.getBuilding();
                }
                String meta = context.getString(R.string.room_short) + " " + lesson.getRoom();
                if (StringUtils.isNotBlank(lesson.getBuilding())) {
                    meta += " (" + lesson.getBuilding() + ")";
                }
                return meta;
            }
            case "room": {
                return lesson.getBuilding();
            }
        }
        return null;
    }

    private FrameLayout getFlag(String text, int textColor, int backgroundColor) {
        FrameLayout flagContainer = (FrameLayout) inflate(R.layout.layout_schedule_lessons_flag);
        if (flagContainer == null) {
            return null;
        }
        TextView flag_content = flagContainer.findViewById(R.id.flag_content);
        flag_content.setText(text);
        flag_content.setBackgroundColor(backgroundColor);
        flag_content.setTextColor(textColor);
        return flagContainer;
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        if (context == null) {
            log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
