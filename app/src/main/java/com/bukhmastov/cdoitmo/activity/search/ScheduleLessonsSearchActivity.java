package com.bukhmastov.cdoitmo.activity.search;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.AS;

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
    protected void onDone(final String query) {
        thread.run(AS, () -> {
            log.v(TAG, "onDone | query=", query);
            tabHostPresenter.setQuery(query);
            tabHostPresenter.invalidate();
        });
    }
}
