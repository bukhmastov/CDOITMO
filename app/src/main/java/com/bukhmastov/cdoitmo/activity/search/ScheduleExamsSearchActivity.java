package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

public class ScheduleExamsSearchActivity extends SearchActivity {

    private static final String TAG = "SESearchActivity";

    public ScheduleExamsSearchActivity() {
        super(3, 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(this);
    }

    @Override
    protected String getType() {
        return "exams";
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_exams_search_view_hint);
    }

    @Override
    protected void onDone(final String query) {
        Static.T.runThread(() -> {
            Log.v(TAG, "onDone | query=", query);
            ScheduleExamsFragment.setQuery(query);
            ScheduleExamsFragment.invalidate();
        });
    }
}
