package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragment.presenter.ConnectedFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsCacheFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsERegisterFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsExtendedFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsGeneralFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsNotificationsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsProtocolFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsSystemsFragment;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

public abstract class ConnectedFragment<P extends ConnectedFragmentPresenter> extends ConnectedFragmentBase {

    private static final String TAG = "ConnectedFragment";
    protected ConnectedActivity activity = null;
    protected View container = null;
    protected Bundle extras = null;

    protected abstract P getPresenter();
    protected abstract @LayoutRes int getLayoutId();
    protected abstract @IdRes int getRootId();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            activity = (ConnectedActivity) context;
        } catch (ClassCastException e) {
            log.wtf(TAG, context.toString(), " must implement ConnectedActivity");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        extras = getArguments();
        if (getPresenter() != null) {
            getPresenter().setFragment(this);
        }
        super.onCreate(savedInstanceState);
        if (getPresenter() != null) {
            getPresenter().onCreate(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getPresenter() != null && toolbar() != null) {
            getPresenter().onToolbarSetup(toolbar());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPresenter() != null) {
            getPresenter().onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPresenter() != null) {
            getPresenter().onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getPresenter() != null && toolbar() != null) {
            getPresenter().onToolbarTeardown(toolbar());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getPresenter() != null) {
            getPresenter().onDestroy();
        }
    }

    @Override
    public void onDetach() {
        activity = null;
        super.onDetach();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (getPresenter() != null) {
            getPresenter().onToolbarSetup(menu);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = view;
        onViewCreated();
    }

    public void onViewCreated() {
        if (getPresenter() != null) {
            getPresenter().onViewCreated();
        }
    }

    public boolean isNotAddedToActivity() {
        try {
            if (!thread.assertUI()) {
                return true;
            }
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                return true;
            }
            fragmentManager.executePendingTransactions();
            return !isAdded();
        } catch (Throwable throwable) {
            if (!(throwable instanceof IllegalStateException)) {
                log.exception(throwable);
            }
            return false;
        }
    }

    public @Nullable ConnectedActivity activity() {
        return activity;
    }

    public @Nullable Menu toolbar() {
        if (activity() == null) {
            return null;
        }
        return activity().toolbar;
    }

    public View container() {
        return container;
    }

    public Bundle extras() {
        return extras;
    }

    public void storeData(String data) {
        storeData(data, null);
    }

    public void storeData(String data, String extra) {
        log.v(TAG, "storeData | activity=", activity(), " | data=", (data == null ? "<null>" : "<notnull>"), " | extra=", (extra == null ? "<null>" : "<notnull>"));
        ConnectedActivity.storedFragmentName = this.getClass().getCanonicalName();
        ConnectedActivity.storedFragmentData = data;
        ConnectedActivity.storedFragmentExtra = extra;
    }

    public String restoreData() {
        log.v(TAG, "restoreData | activity=", activity());
        if (ConnectedActivity.storedFragmentName != null && this.getClass().getCanonicalName().equals(ConnectedActivity.storedFragmentName)) {
            return ConnectedActivity.storedFragmentData;
        } else {
            return null;
        }
    }

    public String restoreDataExtra() {
        log.v(TAG, "restoreDataExtra | activity=", activity());
        if (ConnectedActivity.storedFragmentName != null && this.getClass().getCanonicalName().equals(ConnectedActivity.storedFragmentName)) {
            return ConnectedActivity.storedFragmentExtra;
        } else {
            return null;
        }
    }

    public boolean clearData() {
        log.v(TAG, "clearData | activity=", activity());
        if (this.getClass().getCanonicalName().equals(ConnectedActivity.storedFragmentName)) {
            ConnectedActivity.storedFragmentName = null;
            ConnectedActivity.storedFragmentData = null;
            ConnectedActivity.storedFragmentExtra = null;
            return true;
        }
        return false;
    }

    public void close() {
        if (activity() != null && activity().back()) {
            activity().finish();
        }
    }

    public void draw(@LayoutRes int layout) {
        try {
            View view = inflate(layout);
            if (view == null) {
                log.e(TAG, "Failed to draw layout, view is null");
                return;
            }
            draw(view);
        } catch (Exception e){
            log.exception(e);
        }
    }

    public void draw(View view) {
        try {
            if (activity() == null) {
                log.e(TAG, "Failed to draw layout, activity is null");
                return;
            }
            ViewGroup vg = activity().findViewById(getRootId());
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    public View inflate(@LayoutRes int layout) throws InflateException {
        if (activity() == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }

    public static class Data {
        public final Class connectedFragmentClass;
        public final String title;
        public final Integer image;
        public Data(Class connectedFragmentClass, String title, Integer image) {
            this.connectedFragmentClass = connectedFragmentClass;
            this.title = title;
            this.image = image;
        }
    }

    public static @NonNull Data getData(Context context, Class connectedFragment) {
        if (connectedFragment == ERegisterFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
        if (connectedFragment == ProtocolFragment.class) return new Data(connectedFragment, context.getString(R.string.protocol_changes), R.drawable.ic_protocol_changes);
        if (connectedFragment == RatingFragment.class) return new Data(connectedFragment, context.getString(R.string.rating), R.drawable.ic_rating);
        if (connectedFragment == Room101Fragment.class) return new Data(connectedFragment, context.getString(R.string.room101), R.drawable.ic_room101);
        if (connectedFragment == ScheduleExamsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_exams), R.drawable.ic_schedule_exams);
        if (connectedFragment == ScheduleLessonsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_lessons), R.drawable.ic_schedule_lessons);
        if (connectedFragment == ScheduleAttestationsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_attestations), R.drawable.ic_schedule_attestations);
        if (connectedFragment == UniversityFragment.class) return new Data(connectedFragment, context.getString(R.string.university), R.drawable.ic_university);
        if (connectedFragment == ERegisterSubjectFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
        if (connectedFragment == RatingListFragment.class) return new Data(connectedFragment, context.getString(R.string.top_rating), R.drawable.ic_rating);
        if (connectedFragment == ScheduleLessonsModifyFragment.class) return new Data(connectedFragment, context.getString(R.string.lesson_creation), R.drawable.ic_schedule_lessons);
        if (connectedFragment == ScheduleLessonsShareFragment.class) return new Data(connectedFragment, context.getString(R.string.share_changes), R.drawable.ic_share);
        if (connectedFragment == HomeScreenInteractionFragment.class) return new Data(connectedFragment, context.getString(R.string.manage_homescreen_interaction), R.drawable.ic_shortcut);
        if (connectedFragment == SettingsFragment.class) return new Data(connectedFragment, context.getString(R.string.settings), R.drawable.ic_settings);
        if (connectedFragment == SettingsGeneralFragment.class) return new Data(connectedFragment, context.getString(R.string.general_settings), R.drawable.ic_settings_applications);
        if (connectedFragment == SettingsCacheFragment.class) return new Data(connectedFragment, context.getString(R.string.cache_and_refresh), R.drawable.ic_save);
        if (connectedFragment == SettingsNotificationsFragment.class) return new Data(connectedFragment, context.getString(R.string.notifications), R.drawable.ic_notifications);
        if (connectedFragment == SettingsExtendedFragment.class) return new Data(connectedFragment, context.getString(R.string.extended_prefs), R.drawable.ic_tune);
        if (connectedFragment == SettingsERegisterFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
        if (connectedFragment == SettingsProtocolFragment.class) return new Data(connectedFragment, context.getString(R.string.protocol_changes), R.drawable.ic_protocol_changes);
        if (connectedFragment == SettingsScheduleLessonsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_lessons), R.drawable.ic_schedule_lessons);
        if (connectedFragment == SettingsScheduleExamsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_exams), R.drawable.ic_schedule_exams);
        if (connectedFragment == SettingsScheduleAttestationsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_attestations), R.drawable.ic_schedule_attestations);
        if (connectedFragment == SettingsSystemsFragment.class) return new Data(connectedFragment, context.getString(R.string.pref_category_system), R.drawable.ic_package);
        if (connectedFragment == AboutFragment.class) return new Data(connectedFragment, context.getString(R.string.about), R.drawable.ic_info_outline);
        if (connectedFragment == LogFragment.class) return new Data(connectedFragment, context.getString(R.string.log), R.drawable.ic_info_outline);
        if (connectedFragment == LinkedAccountsFragment.class) return new Data(connectedFragment, context.getString(R.string.linked_accounts), R.drawable.ic_account_box);
        if (connectedFragment == LinkAccountFragment.class) return new Data(connectedFragment, context.getString(R.string.link_account), R.drawable.ic_link);
        if (connectedFragment == IsuGroupInfoFragment.class) return new Data(connectedFragment, context.getString(R.string.study_groups), R.drawable.ic_group);
        if (connectedFragment == IsuScholarshipPaidFragment.class) return new Data(connectedFragment, context.getString(R.string.scholarship), R.drawable.ic_money_circle);
        if (connectedFragment == IsuScholarshipAssignedFragment.class) return new Data(connectedFragment, context.getString(R.string.scholarship_assigned), R.drawable.ic_money_circle);
        if (connectedFragment == IsuScholarshipPaidDetailsFragment.class) return new Data(connectedFragment, context.getString(R.string.scholarship_paid), R.drawable.ic_money_circle);
        throw new IllegalStateException("Connected fragment class without declaration at ConnectedFragment#getData, class = " + connectedFragment.getName());
    }
}
