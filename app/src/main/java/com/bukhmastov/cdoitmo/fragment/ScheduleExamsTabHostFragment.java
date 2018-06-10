package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.SparseArray;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

public abstract class ScheduleExamsTabHostFragment extends Fragment {

    private static final String TAG = "SETabHostFragment";
    private static final String FRAGMENT_NAME = "com.bukhmastov.cdoitmo.fragments.ScheduleExamsTabHostFragment";
    public interface TabProvider {
        void onInvalidate(boolean refresh);
    }
    public class Scroll {
        public int position = 0;
        public int offset = 0;
    }
    protected static ConnectedActivity activity = null;
    private static String lastQuery = null;
    private static String query = null;
    protected static final int DEFAULT_TYPE = 0;
    protected static final int DEFAULT_INVALID_TYPE = -1;
    private static final int TAB_TOTAL_COUNT = 2;
    public static final SparseArray<Scroll> scroll = new SparseArray<>();
    public static final SparseArray<TabProvider> tabs = new SparseArray<>();

    protected boolean invalidate = false;
    protected boolean invalidate_refresh = false;
    protected int TYPE = DEFAULT_INVALID_TYPE;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (activity == null) {
            try {
                activity = (ConnectedActivity) getActivity();
            } catch (Exception e) {
                try {
                    activity = (ConnectedActivity) context;
                } catch (Exception e1) {
                    Static.error(e);
                    Static.error(e1);
                    activity = null;
                }
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (!isActive()) {
            activity = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isActive()) {
            scroll.clear();
            tabs.clear();
        }
    }

    protected static boolean isActive() {
        for (int i = 0; i < TAB_TOTAL_COUNT; i++) {
            if (tabs.get(i) != null) return true;
        }
        return false;
    }
    public static void setQuery(String query) {
        ScheduleExamsTabHostFragment.lastQuery = ScheduleExamsTabHostFragment.query;
        ScheduleExamsTabHostFragment.query = query;
        storeData(query);
    }
    public static String getQuery() {
        return ScheduleExamsTabHostFragment.query;
    }
    public static boolean isSameQueryRequested() {
        return lastQuery != null && query != null && lastQuery.equals(query);
    }
    public static void invalidate() {
        invalidate(false);
    }
    public static void invalidate(boolean refresh) {
        invalidate(-1, refresh);
    }
    public static void invalidate(int exclude, boolean refresh) {
        for (int i = 0; i < TAB_TOTAL_COUNT; i++) {
            if (i == exclude) continue;
            TabProvider tabProvider = tabs.get(i);
            if (tabProvider != null) {
                tabProvider.onInvalidate(refresh);
            }
        }
    }
    public static void invalidateOnDemand() {
        if (!isActive() || activity == null) return;
        Static.T.runThread(() -> {
            Log.v(TAG, "invalidateOnDemand");
            Static.snackBar(activity.findViewById(android.R.id.content), activity.getString(R.string.schedule_refresh), activity.getString(R.string.update), v -> {
                setQuery(getQuery());
                invalidate();
            });
        });
    }

    public static void storeData(String data) {
        Log.v(TAG, "storeData | activity=", activity, " | data=", data);
        if (activity != null) {
            activity.storedFragmentName = FRAGMENT_NAME;
            activity.storedFragmentData = data;
            activity.storedFragmentExtra = null;
        }
    }
    public static String restoreData() {
        Log.v(TAG, "restoreData | activity=", activity);
        if (activity != null && activity.storedFragmentName != null && FRAGMENT_NAME.equals(activity.storedFragmentName)) {
            return activity.storedFragmentData;
        } else {
            return null;
        }
    }
}
