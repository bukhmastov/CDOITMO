package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.util.ArrayMap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.RatingFragmentPresenter;
import com.bukhmastov.cdoitmo.model.rating.pickerall.RFaculty;
import com.bukhmastov.cdoitmo.model.rating.pickerall.RatingPickerAll;
import com.bukhmastov.cdoitmo.model.rating.pickerown.RCourse;
import com.bukhmastov.cdoitmo.model.rating.pickerown.RatingPickerOwn;
import com.bukhmastov.cdoitmo.model.rva.RVARating;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class RatingRVA extends RVA<RVARating> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COMMON = 1;
    private static final int TYPE_COMMON_EMPTY = 2;
    private static final int TYPE_OWN = 3;
    private static final int TYPE_OWN_EMPTY = 4;
    private static final int TYPE_FAILED = 5;
    private static final int TYPE_OFFLINE = 6;

    private String commonSelectedFaculty;
    private String commonSelectedCourse;

    @Inject
    Storage storage;
    @Inject
    Time time;

    public RatingRVA(@NonNull Context context, @NonNull ArrayMap<String, RatingFragmentPresenter.Info> data) {
        super();
        AppComponentProvider.getComponent().inject(this);
        this.commonSelectedFaculty = storage.get(context, Storage.CACHE, Storage.USER, "rating#choose#faculty");
        this.commonSelectedCourse = storage.get(context, Storage.CACHE, Storage.USER, "rating#choose#course");
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
            case TYPE_OWN_EMPTY: layout = R.layout.state_nothing_to_display_compact; break;
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
            case TYPE_OWN_EMPTY: bindEmpty(container); break;
            case TYPE_FAILED: bindFailed(container, item); break;
            case TYPE_OFFLINE: break;
        }
    }

    private ArrayList<Item> map2dataset(@NonNull Context context, @NonNull ArrayMap<String, RatingFragmentPresenter.Info> data) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            RatingFragmentPresenter.Info common = data.get(RatingFragmentPresenter.COMMON);
            RatingFragmentPresenter.Info own    = data.get(RatingFragmentPresenter.OWN);
            // setup common part
            List<RFaculty> faculties = new ArrayList<>();
            dataset.add(new Item<>(TYPE_HEADER, new RVASingleValue(context.getString(R.string.detailed_rating))));
            if (RatingFragmentPresenter.LOADED.equals(common.status) && common.data != null) {
                try {
                    RatingPickerAll ratingPickerAll = (RatingPickerAll) common.data;
                    faculties = ratingPickerAll.getFaculties();
                    if (CollectionUtils.isEmpty(faculties)) {
                        dataset.add(new Item(TYPE_COMMON_EMPTY));
                    } else {
                        dataset.add(new Item<>(TYPE_COMMON, ratingPickerAll));
                    }
                } catch (Exception e) {
                    log.exception(e);
                }
            } else {
                dataset.add(new Item<>(
                        common.status.equals(RatingFragmentPresenter.OFFLINE) ? TYPE_OFFLINE : TYPE_FAILED,
                        new RVASingleValue(common.status.equals(RatingFragmentPresenter.SERVER_ERROR) ? DeIfmoClient.getFailureMessage(context, -1) : "")
                ));
            }
            // setup own mode
            if (App.UNAUTHORIZED_MODE) {
                return dataset;
            }
            dataset.add(new Item<>(TYPE_HEADER, new RVASingleValue(context.getString(R.string.your_rating))));
            if (own.status.equals(RatingFragmentPresenter.LOADED) && own.data != null) {
                try {
                    RatingPickerOwn ratingPickerOwn = (RatingPickerOwn) own.data;
                    List<RCourse> courses = ratingPickerOwn.getCourses();
                    if (CollectionUtils.isEmpty(courses)) {
                        dataset.add(new Item(TYPE_OWN_EMPTY));
                    } else {
                        for (RCourse course : courses) {
                            if (course == null) {
                                continue;
                            }
                            RVARating rvaRating = new RVARating();
                            rvaRating.setTitle(course.getFaculty() + " — " + String.valueOf(course.getCourse()) + " " + context.getString(R.string.course));
                            rvaRating.setPosition(course.getPosition());
                            for (RFaculty faculty : faculties) {
                                if (StringUtils.isNotBlank(faculty.getName()) && faculty.getName().contains(course.getFaculty())) {
                                //if (Objects.equals(faculty.getName(), course.getFaculty())) {
                                    Calendar now = time.getCalendar();
                                    int courseDelta = (ratingPickerOwn.getMaxCourse() - course.getCourse());
                                    int year = now.get(Calendar.YEAR) - courseDelta;
                                    int month = now.get(Calendar.MONTH);
                                    String years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
                                    rvaRating.setDesc(faculty.getDepId());
                                    rvaRating.setMeta(String.valueOf(course.getCourse()));
                                    rvaRating.setExtra(years);
                                    break;
                                }
                            }
                            dataset.add(new Item<>(TYPE_OWN, rvaRating));
                        }
                    }
                } catch (Exception e) {
                    log.exception(e);
                }
            } else {
                dataset.add(new Item<>(
                        own.status.equals(RatingFragmentPresenter.OFFLINE) ? TYPE_OFFLINE : TYPE_FAILED,
                        new RVASingleValue(own.status.equals(RatingFragmentPresenter.SERVER_ERROR) ? DeIfmoClient.getFailureMessage(context, -1) : "")
                ));
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }

    private void bindHeader(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.title)).setText(item.data.getValue());
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindCommon(View container, Item<RatingPickerAll> item) {
        try {
            if (item.data == null || CollectionUtils.isEmpty(item.data.getFaculties())) {
                return;
            }
            Context context = container.getContext();
            ArrayList<String> facultiesAdapterArr = new ArrayList<>();
            ArrayList<String> coursesAdapterArr = new ArrayList<>();
            ArrayList<Integer> selected = new ArrayList<>();
            selected.add(0, 0);
            selected.add(1, 0);
            for (int i = 0; i < item.data.getFaculties().size(); i++) {
                RFaculty faculty = item.data.getFaculties().get(i);
                facultiesAdapterArr.add(faculty.getName());
                if (commonSelectedFaculty.equals(faculty.getDepId())) {
                    selected.add(0, i);
                }
            }
            for (int i = 1; i <= 4; i++) {
                coursesAdapterArr.add(i + " " + context.getString(R.string.course));
                if (commonSelectedCourse.equals(String.valueOf(i))) {
                    selected.add(1, i - 1);
                }
            }
            // faculty spinner
            Spinner spinner;
            spinner = container.findViewById(R.id.faculty);
            if (spinner != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_rating, facultiesAdapterArr);
                adapter.setDropDownViewResource(R.layout.spinner_center_normal_case);
                spinner.setAdapter(adapter);
                spinner.setSelection(selected.get(0));
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long selectedId) {
                        commonSelectedFaculty = item.data.getFaculties().get(position).getDepId();
                        if (onElementClickListeners.containsKey(R.id.faculty)) {
                            RVARating rvaRating = new RVARating();
                            rvaRating.setExtra(commonSelectedFaculty);
                            onElementClickListeners.get(R.id.faculty).onClick(view, rvaRating);
                        }
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                commonSelectedFaculty = item.data.getFaculties().get(spinner.getSelectedItemPosition()).getDepId();
            }
            // course spinner
            spinner = container.findViewById(R.id.course);
            if (spinner != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_rating, coursesAdapterArr);
                adapter.setDropDownViewResource(R.layout.spinner_center_normal_case);
                spinner.setAdapter(adapter);
                spinner.setSelection(selected.get(1));
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long selectedId) {
                        commonSelectedCourse = String.valueOf(position + 1);
                        if (onElementClickListeners.containsKey(R.id.course)) {
                            RVARating rvaRating = new RVARating();
                            rvaRating.setExtra(commonSelectedCourse);
                            onElementClickListeners.get(R.id.course).onClick(view, rvaRating);
                        }
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                commonSelectedCourse = String.valueOf(spinner.getSelectedItemPosition() + 1);
            }
            // apply button
            if (onElementClickListeners.containsKey(R.id.common_apply)) {
                container.findViewById(R.id.common_apply).setOnClickListener(v -> {
                    OnElementClickListener<RVARating> listener = onElementClickListeners.get(R.id.common_apply);
                    if (listener != null) {
                        RVARating rvaRating = new RVARating();
                        rvaRating.setTitle(commonSelectedFaculty);
                        rvaRating.setDesc(commonSelectedCourse);
                        listener.onClick(v, rvaRating);
                    }
                });
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindOwn(View container, Item<RVARating> item) {
        try {
            ((TextView) container.findViewById(R.id.title)).setText(item.data.getTitle());
            ((TextView) container.findViewById(R.id.position)).setText(item.data.getPosition());
            tryRegisterClickListener(container, R.id.own_apply, item.data);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindEmpty(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_data);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindFailed(View container, Item<RVASingleValue> item) {
        try {
            if (StringUtils.isNotBlank(item.data.getValue())) {
                ((TextView) container.findViewById(R.id.state_failed_compact_message)).setText(item.data.getValue());
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
