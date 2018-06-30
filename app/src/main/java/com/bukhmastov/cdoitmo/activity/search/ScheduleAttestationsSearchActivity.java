package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

public class ScheduleAttestationsSearchActivity extends SearchActivity {

    private static final String TAG = "SASearchActivity";

    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    public ScheduleAttestationsSearchActivity() {
        super(3, 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAnalyticsProvider.logCurrentScreen(this);
    }

    @Override
    protected String getType() {
        return "attestations";
    }

    @Override
    protected String getHint() {
        return context.getString(R.string.schedule_attestations_search_view_hint);
    }

    @Override
    protected void onDone(final String query) {
        Thread.run(() -> {
            Log.v(TAG, "onDone | query=", query);
            ScheduleAttestationsFragment.setQuery(query);
            ScheduleAttestationsFragment.invalidate();
        });
    }
}
