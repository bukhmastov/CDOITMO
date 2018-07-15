package com.bukhmastov.cdoitmo.object.schedule;

import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsImpl;

public interface ScheduleLessons extends Schedule {

    // future: replace with DI factory
    ScheduleLessons instance = new ScheduleLessonsImpl();
    static ScheduleLessons instance() {
        return instance;
    }

    String TYPE = "lessons";

}
