package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsCacheFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsERegisterFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsExtendedFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsGeneralFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsNotificationsFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsProtocolFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsSystemsFragment;
import com.bukhmastov.cdoitmo.utils.Log;

public abstract class ConnectedFragment extends Fragment {

    private static final String TAG = "ConnectedFragment";
    protected ConnectedActivity activity = null;
    protected Bundle extras = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            activity = (ConnectedActivity) context;
        } catch (ClassCastException e) {
            Log.wtf(TAG, context.toString() + " must implement ConnectedActivity");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extras = getArguments();
    }

    public static Data getData(Context context, Class connectedFragment) {
        if (connectedFragment == ERegisterFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
        if (connectedFragment == ProtocolFragment.class) return new Data(connectedFragment, context.getString(R.string.protocol_changes), R.drawable.ic_protocol_changes);
        if (connectedFragment == RatingFragment.class) return new Data(connectedFragment, context.getString(R.string.rating), R.drawable.ic_rating);
        if (connectedFragment == Room101Fragment.class) return new Data(connectedFragment, context.getString(R.string.room101), R.drawable.ic_room101);
        if (connectedFragment == ScheduleExamsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_exams), R.drawable.ic_schedule_exams);
        if (connectedFragment == ScheduleLessonsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_lessons), R.drawable.ic_schedule_lessons);
        if (connectedFragment == UniversityFragment.class) return new Data(connectedFragment, context.getString(R.string.university), R.drawable.ic_university);
        if (connectedFragment == SubjectShowFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
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
        if (connectedFragment == SettingsSystemsFragment.class) return new Data(connectedFragment, context.getString(R.string.pref_category_system), R.drawable.ic_package);
        if (connectedFragment == AboutFragment.class) return new Data(connectedFragment, context.getString(R.string.about), R.drawable.ic_info_outline);
        if (connectedFragment == LogFragment.class) return new Data(connectedFragment, context.getString(R.string.log), R.drawable.ic_info_outline);
        if (connectedFragment == LinkedAccountsFragment.class) return new Data(connectedFragment, context.getString(R.string.linked_accounts), R.drawable.ic_account_box);
        Log.wtf(TAG, "getData | fragment class (" + connectedFragment.toString() + ") does not supported!");
        return null;
    }

    protected void close() {
        if (activity != null && activity.back()) {
            activity.finish();
        }
    }

    public static class Data {
        public final Class connectedFragmentClass;
        public final String title;
        public final Integer image;
        public Data(Class connectedFragmentClass, String title, Integer image){
            this.connectedFragmentClass = connectedFragmentClass;
            this.title = title;
            this.image = image;
        }
    }
}
