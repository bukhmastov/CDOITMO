package com.bukhmastov.cdoitmo.object;

import android.content.Context;

import org.json.JSONObject;

import java.util.ArrayList;

public interface DaysRemainingWidget {

    void start(Context context, Delegate delegate, JSONObject schedule);

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
