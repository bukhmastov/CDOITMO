package com.bukhmastov.cdoitmo.fragment.presenter;

import android.util.SparseArray;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;

public interface ScheduleExamsTabHostFragmentPresenter {

    void onAttach(ConnectedActivity activity);

    void onDetach();

    void onDestroy();

    boolean isActive();

    void setQuery(String query);

    String getQuery();

    boolean isSameQueryRequested();

    void invalidate();

    void invalidate(boolean refresh);

    void invalidate(int exclude, boolean refresh);

    void invalidateOnDemand();

    void storeData(String data);

    String restoreData();

    SparseArray<Scroll> scroll();

    SparseArray<TabProvider> tabs();

    interface TabProvider {
        void onInvalidate(boolean refresh);
    }

    class Scroll {
        public int position = 0;
        public int offset = 0;
    }

    int DEFAULT_TYPE = 0;
    int DEFAULT_INVALID_TYPE = -1;
    int TAB_TOTAL_COUNT = 2;
}
