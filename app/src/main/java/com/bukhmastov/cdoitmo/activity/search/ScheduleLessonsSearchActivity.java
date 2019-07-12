package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class ScheduleLessonsSearchActivity extends SearchActivity {

    private static final String TAG = "SLSearchActivity";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleLessonsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    Account account;

    public ScheduleLessonsSearchActivity() {
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
        return "lessons";
    }

    @Override
    protected String getHint() {
        return context.getString(R.string.schedule_lessons_search_view_hint);
    }

    @Override
    protected void onDone(String query, @ResultType String type) {
        log.v(TAG, "onDone | query=", query, " | type=", type);
        tabHostPresenter.setQuery(query);
        tabHostPresenter.invalidate();
        thread.standalone(() -> logStatistic(query, type));
    }

    private void logStatistic(String query, @ResultType String type) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_TYPE, "schedule_search");
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_QUERY, query);
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_QUERY_IS_SELF, account.getGroups(context).contains(query) ? "1" : "0");
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_EXTRA, type);
        firebaseAnalyticsProvider.logEvent(
                context,
                FirebaseAnalyticsProvider.Event.SCHEDULE_LESSONS,
                bundle
        );
    }
}
