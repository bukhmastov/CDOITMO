package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;

public interface TimeRemainingWidget {

    void start(SLessons schedule, Delegate delegate);

    void stop();

    interface Delegate {
        void onAction(Data json);
        void onCancelled();
    }

    class Data {
        public String current = null;
        public String current15min = null;
        public String next = null;
        public String day = null;
    }
}
