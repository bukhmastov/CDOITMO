package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsTabHostFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

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
        return context.getString(R.string.schedule_exams_search_view_hint);
    }

    @Override
    protected void onDone(final String query) {
        Thread.run(() -> {
            Log.v(TAG, "onDone | query=", query);
            ScheduleExamsTabHostFragment.setQuery(query);
            ScheduleExamsTabHostFragment.invalidate();
        });
    }
}
