package com.bukhmastov.cdoitmo.object;

import android.content.Context;

import com.bukhmastov.cdoitmo.object.impl.TimeRemainingWidgetImpl;

import org.json.JSONObject;

public interface TimeRemainingWidget {

    // future: replace with DI factory
    TimeRemainingWidget instance = new TimeRemainingWidgetImpl();
    static TimeRemainingWidget instance() {
        return instance;
    }

    void start(Context context, Delegate delegate, JSONObject schedule);

    void stop();

    interface Delegate {
        void onAction(Data json);
        void onCancelled();
    }

    class Data {
        public String current = null;
        public String current_15min = null;
        public String next = null;
        public String day = null;
    }
}
