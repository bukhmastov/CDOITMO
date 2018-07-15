package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.SparseArray;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

public abstract class ScheduleLessonsTabHostFragment extends Fragment {

    private static final String TAG = "SLTabHostFragment";
    private static final String FRAGMENT_NAME = "com.bukhmastov.cdoitmo.fragments.ScheduleLessonsTabHostFragment";
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
    protected static final int DEFAULT_TYPE = 2;
    protected static final int DEFAULT_INVALID_TYPE = -1;
    private static final int TAB_TOTAL_COUNT = 3;
    public static final SparseArray<Scroll> scroll = new SparseArray<>();
    public static final SparseArray<TabProvider> tabs = new SparseArray<>();

    protected boolean invalidate = false;
    protected boolean invalidate_refresh = false;
    protected int TYPE = DEFAULT_INVALID_TYPE;

    //@Inject
    private Log log = Log.instance();

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
                    log.exception(e);
                    log.exception(e1);
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
        ScheduleLessonsTabHostFragment.lastQuery = ScheduleLessonsTabHostFragment.query;
        ScheduleLessonsTabHostFragment.query = query;
        storeData(query);
    }
    public static String getQuery() {
        return ScheduleLessonsTabHostFragment.query;
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
    public static void invalidateOnDemand(Thread thread) {
        if (!isActive() || activity == null) return;
        thread.run(() -> {
            BottomBar.snackBar(activity, activity.findViewById(android.R.id.content), activity.getString(R.string.schedule_refresh), activity.getString(R.string.update), v -> {
                setQuery(getQuery());
                invalidate();
            });
        });
    }

    public static void storeData(String data) {
        if (activity != null) {
            ConnectedActivity.storedFragmentName = FRAGMENT_NAME;
            ConnectedActivity.storedFragmentData = data;
            ConnectedActivity.storedFragmentExtra = null;
        }
    }
    public static String restoreData() {
        if (activity != null && ConnectedActivity.storedFragmentName != null && FRAGMENT_NAME.equals(ConnectedActivity.storedFragmentName)) {
            return ConnectedActivity.storedFragmentData;
        } else {
            return null;
        }
    }
}
