package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleAttestationsFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.AS;

public class ScheduleAttestationsSearchActivity extends SearchActivity {

    private static final String TAG = "SASearchActivity";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleAttestationsFragmentPresenter scheduleAttestationsFragmentPresenter;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleAttestationsSearchActivity() {
        super(3, 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
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
        thread.run(AS, () -> {
            log.v(TAG, "onDone | query=", query);
            scheduleAttestationsFragmentPresenter.setQuery(query);
            scheduleAttestationsFragmentPresenter.invalidate();
        });
    }
}
