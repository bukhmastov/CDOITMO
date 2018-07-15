package com.bukhmastov.cdoitmo.object;

import android.content.Context;

import com.bukhmastov.cdoitmo.object.impl.TimeRemainingWidgetImpl;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.singleton.TextUtils;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
