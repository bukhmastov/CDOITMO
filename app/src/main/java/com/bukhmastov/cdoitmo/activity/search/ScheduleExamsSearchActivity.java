package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.AS;

public class ScheduleExamsSearchActivity extends SearchActivity {

    private static final String TAG = "SESearchActivity";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleExamsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleExamsSearchActivity() {
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
        return "exams";
    }

    @Override
    protected String getHint() {
        return context.getString(R.string.schedule_exams_search_view_hint);
    }

    @Override
    protected void onDone(String query, @ResultType String type) {
        log.v(TAG, "onDone | query=", query, " | type=", type);
        tabHostPresenter.setQuery(query);
        tabHostPresenter.invalidate();
    }
}
