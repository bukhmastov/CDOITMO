package com.bukhmastov.cdoitmo.adapters.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.RatingFragment;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class RatingRVA extends RecyclerViewAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COMMON = 1;
    private static final int TYPE_COMMON_EMPTY = 2;
    private static final int TYPE_OWN = 3;
    private static final int TYPE_OWN_EMPTY = 4;
    private static final int TYPE_FAILED = 5;
    private static final int TYPE_OFFLINE = 6;

    private String commonSelectedFaculty = "";
    private String commonSelectedCourse = "";

    public RatingRVA(@NonNull Context context, @NonNull ArrayMap<RatingFragment.TYPE, RatingFragment.Info> data) {
        this.commonSelectedFaculty = Storage.file.cache.get(context, "rating#choose#faculty");
        this.commonSelectedCourse = Storage.file.cache.get(context, "rating#choose#course");
        addItems(map2dataset(context, data));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_rating_header; break;
            case TYPE_COMMON: layout = R.layout.layout_rating_common; break;
            case TYPE_OWN: layout = R.layout.layout_rating_own; break;
            case TYPE_COMMON_EMPTY:
            case TYPE_OWN_EMPTY: layout = R.layout.nothing_to_display; break;
            case TYPE_FAILED: layout = R.layout.state_failed_compact; break;
            case TYPE_OFFLINE: layout = R.layout.state_offline_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: bindHeader(container, item); break;
            case TYPE_COMMON: bindCommon(container, item); break;
            case TYPE_OWN: bindOwn(container, item); break;
            case TYPE_COMMON_EMPTY:
            case TYPE_OWN_EMPTY: bindEmpty(container, item); break;
            case TYPE_FAILED: bindFailed(container, item); break;
            case TYPE_OFFLINE: break;
        }
    }

    private ArrayList<Item> map2dataset(@NonNull final Context context, @NonNull final ArrayMap<RatingFragment.TYPE, RatingFragment.Info> data) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            final RatingFragment.Info common = data.get(RatingFragment.TYPE.common);
            final RatingFragment.Info own    = data.get(RatingFragment.TYPE.own);
            // setup common part
            JSONArray faculties = new JSONArray();
            dataset.add(getNewItem(TYPE_HEADER, new JSONObject().put("title", context.getString(R.string.detailed_rating))));
            if (common.status == RatingFragment.STATUS.loaded && common.data != null) {
                try {
                    faculties = common.data.getJSONObject("rating").getJSONArray("faculties");
                    if (faculties.length() == 0) {
                        dataset.add(getNewItem(TYPE_COMMON_EMPTY, null));
                    } else {
                        dataset.add(getNewItem(TYPE_COMMON, new JSONObject().put("faculties", faculties)));
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            } else {
                dataset.add(getNewItem(
                        common.status == RatingFragment.STATUS.offline ? TYPE_OFFLINE : TYPE_FAILED,
                        new JSONObject().put("text", common.status == RatingFragment.STATUS.server_error ? DeIfmoClient.getFailureMessage(context, -1) : "")
                ));
            }
            // setup own mode
            if (!Static.UNAUTHORIZED_MODE) {
                dataset.add(getNewItem(TYPE_HEADER, new JSONObject().put("title", context.getString(R.string.your_rating))));
                if (own.status == RatingFragment.STATUS.loaded && own.data != null) {
                    try {
                        final JSONArray courses = own.data.getJSONObject("rating").getJSONArray("courses");
                        final int max_course = own.data.getJSONObject("rating").getInt("max_course");
                        if (courses.length() == 0) {
                            dataset.add(getNewItem(TYPE_OWN_EMPTY, null));
                        } else {
                            for (int i = 0; i < courses.length(); i++) {
                                final JSONObject course = courses.getJSONObject(i);
                                final String f = course.getString("faculty");
                                final String p = course.getString("position");
                                final int c = course.getInt("course");
                                JSONObject extras = null;
                                for (int j = 0; j < faculties.length(); j++) {
                                    JSONObject faculty = faculties.getJSONObject(j);
                                    if (faculty.getString("name").contains(f)) {
                                        int course_delta = (max_course - c);
                                        Calendar now = Static.getCalendar();
                                        int year = now.get(Calendar.YEAR) - course_delta;
                                        int month = now.get(Calendar.MONTH);
                                        String years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
                                        extras = new JSONObject()
                                                .put("faculty", faculty.getString("depId"))
                                                .put("course", String.valueOf(c))
                                                .put("years", years);
                                        break;
                                    }
                                }
                                dataset.add(getNewItem(TYPE_OWN, new JSONObject()
                                        .put("title", f + " — " + String.valueOf(c) + " " + context.getString(R.string.course))
                                        .put("position", p)
                                        .put("extras", extras)
                                ));
                            }
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                } else {
                    dataset.add(getNewItem(
                            own.status == RatingFragment.STATUS.offline ? TYPE_OFFLINE : TYPE_FAILED,
                            new JSONObject().put("text", own.status == RatingFragment.STATUS.server_error ? DeIfmoClient.getFailureMessage(context, -1) : "")
                    ));
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return dataset;
    }

    private void bindHeader(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.title)).setText(item.data.getString("title"));
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindCommon(View container, Item item) {
        Static.T.runThread(() -> {
            try {
                final Context context = container.getContext();
                final JSONArray faculties = item.data.getJSONArray("faculties");
                final ArrayList<String> facultiesAdapterArr = new ArrayList<>();
                final ArrayList<String> coursesAdapterArr = new ArrayList<>();
                final ArrayList<Integer> selected = new ArrayList<>();
                final JSONObject extras = new JSONObject()
                        .put("faculty", commonSelectedFaculty)
                        .put("course", commonSelectedCourse);
                selected.add(0, 0);
                selected.add(1, 0);
                for (int i = 0; i < faculties.length(); i++) {
                    final JSONObject faculty = faculties.getJSONObject(i);
                    facultiesAdapterArr.add(faculty.getString("name"));
                    if (commonSelectedFaculty.equals(faculty.getString("depId"))) {
                        selected.add(0, i);
                    }
                }
                for (int i = 1; i <= 4; i++) {
                    coursesAdapterArr.add(i + " " + context.getString(R.string.course));
                    if (commonSelectedCourse.equals(String.valueOf(i))) {
                        selected.add(1, i - 1);
                    }
                }
                Static.T.runOnUiThread(() -> {
                    try {
                        // faculty spinner
                        final Spinner faculty_spinner = container.findViewById(R.id.faculty);
                        if (faculty_spinner != null) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_rating_layout, facultiesAdapterArr);
                            adapter.setDropDownViewResource(R.layout.spinner_layout_normal_case);
                            faculty_spinner.setAdapter(adapter);
                            faculty_spinner.setSelection(selected.get(0));
                            faculty_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                                    Static.T.runThread(() -> {
                                        try {
                                            commonSelectedFaculty = faculties.getJSONObject(position).getString("depId");
                                            Storage.file.cache.put(context, "rating#choose#faculty", commonSelectedFaculty);
                                        } catch (Exception e) {
                                            Static.error(e);
                                        }
                                    });
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // course spinner
                        final Spinner course_spinner = container.findViewById(R.id.course);
                        if (course_spinner != null) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_rating_layout, coursesAdapterArr);
                            adapter.setDropDownViewResource(R.layout.spinner_layout_normal_case);
                            course_spinner.setAdapter(adapter);
                            course_spinner.setSelection(selected.get(1));
                            course_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                                    Static.T.runThread(() -> {
                                        try {
                                            commonSelectedCourse = String.valueOf(position + 1);
                                            Storage.file.cache.put(context, "rating#choose#course", commonSelectedCourse);
                                        } catch (Exception e) {
                                            Static.error(e);
                                        }
                                    });
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // apply button
                        if (onElementClickListeners.containsKey(R.id.common_apply)) {
                            container.findViewById(R.id.common_apply).setOnClickListener(v -> Static.T.runOnUiThread(() -> onElementClickListeners.get(R.id.common_apply).onClick(v, getMap("data", extras))));
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                });
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    private void bindOwn(View container, Item item) {
        try {
            final String title = item.data.getString("title");
            final String position = item.data.getString("position");
            final JSONObject extras = item.data.has("extras") && !item.data.isNull("extras") ? item.data.getJSONObject("extras") : null;
            ((TextView) container.findViewById(R.id.title)).setText(title);
            ((TextView) container.findViewById(R.id.position)).setText(position);
            if (onElementClickListeners.containsKey(R.id.own_apply)) {
                container.findViewById(R.id.own_apply).setOnClickListener(v -> Static.T.runOnUiThread(() -> onElementClickListeners.get(R.id.own_apply).onClick(v, getMap("data", extras))));
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindEmpty(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_data);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindFailed(View container, Item item) {
        try {
            final String text = item.data.getString("text");
            if (text != null && !text.isEmpty()) {
                ((TextView) container.findViewById(R.id.state_failed_compact_message)).setText(text);
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
}
