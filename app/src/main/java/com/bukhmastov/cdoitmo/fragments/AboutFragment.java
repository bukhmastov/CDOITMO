package com.bukhmastov.cdoitmo.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.activities.PikaActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.Random;

public class AboutFragment extends ConnectedFragment {

    private static final String TAG = "AboutFragment";
    private final Random random = new Random();
    private int counterToPika = 0;
    private final int tapsToPika = 5;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView app_version = view.findViewById(R.id.app_version);
        if (app_version != null) {
            app_version.setText(activity.getString(R.string.version) + " " + Static.versionName + " (" + Static.versionCode + " " + activity.getString(R.string.build) + ")");
        }
        // ----------
        View block_pika = view.findViewById(R.id.block_pika);
        if (block_pika != null) {
            block_pika.setOnClickListener(v -> {
                if (counterToPika >= tapsToPika) {
                    if (random.nextInt(200) % 10 == 0) {
                        activity.startActivity(new Intent(activity, PikaActivity.class));
                    }
                } else {
                    counterToPika++;
                }
            });
        }
        // ----------
        View open_log = view.findViewById(R.id.open_log);
        if (open_log != null) {
            open_log.setOnClickListener(v -> activity.openFragment(ConnectedActivity.TYPE.stackable, LogFragment.class, null));
        }
        // ----------
        View block_send_mail = view.findViewById(R.id.block_send_mail);
        if (block_send_mail != null) {
            block_send_mail.setOnClickListener(v -> {
                Log.v(TAG, "send_mail clicked");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "send mail clicked");
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bukhmastov-alex@ya.ru"});
                    activity.startActivity(Intent.createChooser(emailIntent, activity.getString(R.string.send_mail) + "..."));
                } catch (Exception e) {
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
        // ----------
        View block_send_vk = view.findViewById(R.id.block_send_vk);
        if (block_send_vk != null) {
            block_send_vk.setOnClickListener(v -> {
                Log.v(TAG, "send_vk clicked");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "send vk clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/write9780714")));
                } catch (Exception e) {
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
        // ----------
        View block_rate = view.findViewById(R.id.block_rate);
        if (block_rate != null) {
            block_rate.setOnClickListener(v -> {
                Log.v(TAG, "block_rate clicked");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "app rate clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.bukhmastov.cdoitmo")));
                } catch (Exception e) {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.bukhmastov.cdoitmo")));
                    } catch (Exception e2) {
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }
            });
        }
        // ----------
        View block_github = view.findViewById(R.id.block_github);
        if (block_github != null) {
            block_github.setOnClickListener(v -> {
                Log.v(TAG, "block_github clicked");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "view github clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bukhmastov/cdoitmo")));
                } catch (Exception e) {
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
        // ----------
        View block_donate = view.findViewById(R.id.block_donate);
        if (block_donate != null) {
            block_donate.setOnClickListener(v -> {
                Log.v(TAG, "block_donate clicked  ┬─┬ ノ( ゜-゜ノ)");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "donate clicked");
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://yasobe.ru/na/cdoifmo")));
                } catch (Exception e) {
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        }
    }
}
