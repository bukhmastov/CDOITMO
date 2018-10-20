package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;

import java.util.ArrayList;

public interface DaysRemainingWidget {

    void start(SExams schedule, Delegate delegate);

    void stop();

    interface Delegate {
        void onAction(ArrayList<Data> data);
        void onCancelled();
    }

    class Data {
        public String subject = null;
        public String desc = null;
        public DaysRemainingWidget.Time time = null;
    }

    class Time {
        public String day = null;
        public String hour = null;
        public String min = null;
        public String sec = null;
    }
}
