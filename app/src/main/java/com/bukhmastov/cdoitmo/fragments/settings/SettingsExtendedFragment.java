package com.bukhmastov.cdoitmo.fragments.settings;

import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceHeader;

import java.util.ArrayList;
import java.util.List;

public class SettingsExtendedFragment extends SettingsTemplateHeadersFragment {

    private static final String TAG = "SettingsFragment";
    public static List<PreferenceHeader> preferenceHeaders;
    static {
        preferenceHeaders = new ArrayList<>();
        preferenceHeaders.add(new PreferenceHeader(R.string.e_journal, R.drawable.ic_e_journal, SettingsERegisterFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.protocol_changes, R.drawable.ic_protocol_changes, SettingsProtocolFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.schedule_lessons, R.drawable.ic_schedule_lessons, SettingsScheduleLessonsFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.schedule_exams, R.drawable.ic_schedule_exams, SettingsScheduleExamsFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.pref_category_system, R.drawable.ic_package, SettingsSystemsFragment.class));
    }

    @Override
    protected List<PreferenceHeader> getPreferenceHeaders() {
        return preferenceHeaders;
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    @Override
    protected Fragment getSelf() {
        return this;
    }
}
