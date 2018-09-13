package com.bukhmastov.cdoitmo.fragment.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.object.preference.PreferenceHeader;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import android.support.v4.app.Fragment;
import dagger.Lazy;

public class SettingsFragment extends SettingsTemplateHeadersFragment {

    private static final String TAG = "SettingsFragment";
    public static final List<PreferenceHeader> preferenceHeaders;
    static {
        preferenceHeaders = new ArrayList<>();
        preferenceHeaders.add(new PreferenceHeader(R.string.general_settings, R.drawable.ic_settings_applications, SettingsGeneralFragment.class));
        // TODO uncomment, when accounts links will be ready
        //preferenceHeaders.add(new PreferenceHeader(R.string.linked_accounts, R.drawable.ic_account_box, LinkedAccountsFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.cache_and_refresh, R.drawable.ic_save, SettingsCacheFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.notifications, R.drawable.ic_notifications, SettingsNotificationsFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.extended_prefs, R.drawable.ic_tune, SettingsExtendedFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.about, R.drawable.ic_info_outline, AboutFragment.class));
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

    @Inject
    Lazy<StoragePref> storagePref;
    @Inject
    Lazy<Thread> thread;
    @Inject
    Lazy<Theme> theme;
    @Inject
    Lazy<NotificationMessage> notificationMessage;
    @Inject
    Lazy<Static> staticUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        try {
            if (activity.toolbar != null) {
                MenuItem action = activity.toolbar.findItem(R.id.action_restore);
                if (action != null) {
                    action.setVisible(true);
                    action.setOnMenuItemClickListener(item -> {
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return false;
                        }
                        new AlertDialog.Builder(activity)
                                .setIcon(R.drawable.ic_settings_restore_black)
                                .setTitle(R.string.reset_preferences)
                                .setMessage(R.string.reset_preference_message)
                                .setNegativeButton(R.string.do_cancel, null)
                                .setPositiveButton(R.string.proceed, (dialog, which) -> resetPreferences())
                                .create().show();
                        return false;
                    });
                }
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (activity.toolbar != null) {
                MenuItem action = activity.toolbar.findItem(R.id.action_restore);
                if (action != null) {
                    action.setVisible(false);
                }
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    private void resetPreferences() {
        thread.get().run(() -> {
            storagePref.get().reset(activity);
            storagePref.get().applyDebug(activity);
            theme.get().updateAppTheme(activity);
            notificationMessage.get().snackBar(
                    activity,
                    activity.getString(R.string.restart_required),
                    activity.getString(R.string.restart),
                    NotificationMessage.LENGTH_LONG,
                    view -> staticUtil.get().reLaunch(activity)
            );
        });
    }
}
