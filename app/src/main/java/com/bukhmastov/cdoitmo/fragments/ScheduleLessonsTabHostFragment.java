package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

public abstract class ScheduleLessonsTabHostFragment extends Fragment {

    private static final String TAG = "SLFragmentTabHostFragment";
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
    private static final int TYPE_TOTAL_COUNT = 3;
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
        for (int i = 0; i < TYPE_TOTAL_COUNT; i++) {
            if (tabs.get(i) != null) return true;
        }
        return false;
    }
    public static void setQuery(String query) {
        ScheduleLessonsTabHostFragment.lastQuery = ScheduleLessonsTabHostFragment.query;
        ScheduleLessonsTabHostFragment.query = query;
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
        for (int i = 0; i < TYPE_TOTAL_COUNT; i++) {
            if (i == exclude) continue;
            TabProvider tabProvider = tabs.get(i);
            if (tabProvider != null) {
                tabProvider.onInvalidate(refresh);
            }
        }
    }
    public static void invalidateOnDemand() {
        if (!isActive() || activity == null) return;
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "invalidateOnDemand");
                Static.snackBar(activity.findViewById(android.R.id.content), activity.getString(R.string.schedule_refresh), activity.getString(R.string.update), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setQuery(getQuery());
                        invalidate();
                    }
                });
            }
        });
    }
}
