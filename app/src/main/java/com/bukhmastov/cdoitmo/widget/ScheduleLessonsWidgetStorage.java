package com.bukhmastov.cdoitmo.widget;

import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.widget.schedule.lessons.WSLSettings;

public interface ScheduleLessonsWidgetStorage  {

    WSLSettings getSettings(int appWidgetId) throws Exception;

    SLessons getCache(int appWidgetId) throws Exception;

    SLessons getConverted(int appWidgetId) throws Exception;

    void save(int appWidgetId, String type, SLessons cache) throws Exception;

    void save(int appWidgetId, WSLSettings settings) throws Exception;

    void delete(int appWidgetId);
}
