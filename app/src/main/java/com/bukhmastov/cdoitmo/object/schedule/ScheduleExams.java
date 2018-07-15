package com.bukhmastov.cdoitmo.object.schedule;

import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleExamsImpl;

public interface ScheduleExams extends Schedule {

    // future: replace with DI factory
    ScheduleExams instance = new ScheduleExamsImpl();
    static ScheduleExams instance() {
        return instance;
    }

    String TYPE = "exams";

}
