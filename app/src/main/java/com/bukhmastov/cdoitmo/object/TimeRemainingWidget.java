package com.bukhmastov.cdoitmo.object;

import android.content.Context;

import org.json.JSONObject;

public interface TimeRemainingWidget {

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
