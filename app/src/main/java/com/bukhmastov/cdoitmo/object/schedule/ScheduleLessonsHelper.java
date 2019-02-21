package com.bukhmastov.cdoitmo.object.schedule;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;

import java.util.Collection;
import java.util.TreeSet;

public interface ScheduleLessonsHelper {

    boolean clearChanges(String query, Callable callback);

    boolean reduceLesson(String query, int weekday, SLesson lesson, Callable callback);

    boolean restoreLesson(String query, int weekday, SLesson lesson, Callable callback);

    boolean createLesson(ConnectedActivity activity, String query, String title, String type, int weekday, SLesson lesson, Callable callback);

    boolean createLesson(String query, int weekday, SLesson lesson, Callable callback);

    boolean deleteLesson(String query, int weekday, SLesson lesson, Callable callback);

    boolean editLesson(ConnectedActivity activity, String query, String title, String type, int weekday, SLesson lesson, Callable callback);

    String getLessonHash(SLesson lesson) throws Exception;

    String getLessonSignature(SLesson lesson) throws Exception;

    TreeSet<SLesson> filterAndSortLessons(Collection<SLesson> lessons, int parity, boolean hideReducedLessons);

    TreeSet<SDay> filterAndSortDays(Collection<SDay> days);

    TreeSet<SLesson> filterAndSortLessonsForWeekday(SLessons schedule, int parity, int weekday, boolean hideReducedLessons);
}
