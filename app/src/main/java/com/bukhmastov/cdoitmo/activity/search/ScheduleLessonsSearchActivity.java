package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsTabHostFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

public class ScheduleLessonsSearchActivity extends SearchActivity {

    private static final String TAG = "SLSearchActivity";

    public ScheduleLessonsSearchActivity() {
        super(3, 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(this);
    }

    @Override
    protected String getType() {
        return "lessons";
    }

    @Override
    protected String getHint() {
        return context.getString(R.string.schedule_lessons_search_view_hint);
    }

    @Override
    protected void onDone(final String query) {
        Static.T.runThread(() -> {
            Log.v(TAG, "onDone | query=", query);
            ScheduleLessonsTabHostFragment.setQuery(query);
            ScheduleLessonsTabHostFragment.invalidate();
        });
    }
}
