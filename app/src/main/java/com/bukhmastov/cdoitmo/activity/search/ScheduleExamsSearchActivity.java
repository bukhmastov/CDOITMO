package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsTabHostFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

public class ScheduleExamsSearchActivity extends SearchActivity {

    private static final String TAG = "SESearchActivity";

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    public ScheduleExamsSearchActivity() {
        super(3, 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAnalyticsProvider.logCurrentScreen(this);
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
        thread.run(() -> {
            log.v(TAG, "onDone | query=", query);
            ScheduleExamsTabHostFragment.setQuery(query);
            ScheduleExamsTabHostFragment.invalidate();
        });
    }
}
