package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.AlertDialog;
import android.view.View;

import androidx.annotation.StringRes;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.fragment.LogFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.HelpFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class HelpFragmentPresenterImpl extends ConnectedFragmentPresenterImpl implements HelpFragmentPresenter {

    private static final String TAG = "HelpFragment";

    @Inject
    Thread thread;

    public HelpFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        thread.runOnUI(() -> {

            View helpServers = fragment.container().findViewById(R.id.block_servers);
            if (helpServers != null) {
                helpServers.setOnClickListener(v -> showDialog(R.string.help_error_servers, R.string.help_error_servers_desc));
            }

            View helpNotWorking = fragment.container().findViewById(R.id.block_not_working);
            if (helpNotWorking != null) {
                helpNotWorking.setOnClickListener(v -> showDialog(R.string.help_error_not_working, R.string.help_error_not_working_desc));
            }

            View helpGroupWeek = fragment.container().findViewById(R.id.block_group_week);
            if (helpGroupWeek != null) {
                helpGroupWeek.setOnClickListener(v -> showDialog(R.string.help_error_group_week, R.string.help_error_group_week_desc));
            }

            View helpIsuReAuth = fragment.container().findViewById(R.id.block_isu_reauth);
            if (helpIsuReAuth != null) {
                helpIsuReAuth.setOnClickListener(v -> showDialog(R.string.help_error_isu_reauth, R.string.help_error_isu_reauth_desc));
            }

            View helpReadOnly = fragment.container().findViewById(R.id.block_read_only);
            if (helpReadOnly != null) {
                helpReadOnly.setOnClickListener(v -> showDialog(R.string.help_read_only, R.string.help_read_only_desc));
            }

            View openLog = fragment.container().findViewById(R.id.block_log);
            if (openLog != null) {
                openLog.setOnClickListener(v -> openFragment(LogFragment.class));
            }

            View openAbout = fragment.container().findViewById(R.id.block_about);
            if (openAbout != null) {
                openAbout.setOnClickListener(v -> openFragment(AboutFragment.class));
            }
        });
    }

    private void showDialog(@StringRes int title, @StringRes int message) {
        thread.runOnUI(() -> new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_help)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.close, null)
                .create().show());
    }

    private void openFragment(Class connectedFragmentClass) {
        thread.runOnUI(() -> {
            activity.openFragment(ConnectedActivity.TYPE.STACKABLE, connectedFragmentClass, null);
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
