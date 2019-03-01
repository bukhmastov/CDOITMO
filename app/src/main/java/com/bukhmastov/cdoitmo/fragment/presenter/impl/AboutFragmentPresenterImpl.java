package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.PikaActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.LogFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.AboutFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Random;

import javax.inject.Inject;

public class AboutFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements AboutFragmentPresenter {

    private static final String TAG = "AboutFragment";
    private static final String MARKET_IDENTITY = "cdoitmo-market-identity";
    private final Random random = new Random();
    private int counterToPika = 0;
    private final int tapsToPika = 5;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public AboutFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onOpenIntentEventFailed(OpenIntentEvent.Failed event) {
        if (!event.getIdentity().equals(MARKET_IDENTITY)) {
            return;
        }
        eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.bukhmastov.cdoitmo"))));
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        thread.runOnUI(() -> {

            TextView appVersion = fragment.container().findViewById(R.id.app_version);
            if (appVersion != null) {
                appVersion.setText(activity.getString(R.string.version) + " " + App.versionName + " (" + App.versionCode + " " + activity.getString(R.string.build) + (App.DEBUG ? ", debug mode" : "") + ")");
            }

            View blockPika = fragment.container().findViewById(R.id.block_pika);
            if (blockPika != null) {
                blockPika.setOnClickListener(v -> {
                    if (counterToPika >= tapsToPika) {
                        if (random.nextInt(200) % 10 == 0) {
                            eventBus.fire(new OpenActivityEvent(PikaActivity.class));
                        }
                    } else {
                        counterToPika++;
                    }
                });
            }

            View openLog = fragment.container().findViewById(R.id.open_log);
            if (openLog != null) {
                openLog.setOnClickListener(v -> thread.runOnUI(() -> {
                    activity.openFragment(ConnectedActivity.TYPE.STACKABLE, LogFragment.class, null);
                }));
            }

            View blockSendMail = fragment.container().findViewById(R.id.block_send_mail);
            if (blockSendMail != null) {
                blockSendMail.setOnClickListener(v -> thread.standalone(() -> {
                    log.v(TAG, "send_mail clicked");
                    firebaseAnalyticsProvider.logBasicEvent(activity, "send mail clicked");
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bukhmastov-alex@ya.ru"});
                    eventBus.fire(new OpenIntentEvent(Intent.createChooser(emailIntent, activity.getString(R.string.send_mail) + "...")));
                }));
            }

            View blockSendVk = fragment.container().findViewById(R.id.block_send_vk);
            if (blockSendVk != null) {
                blockSendVk.setOnClickListener(v -> thread.standalone(() -> {
                    log.v(TAG, "send_vk clicked");
                    firebaseAnalyticsProvider.logBasicEvent(activity, "send vk clicked");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/write9780714"))));
                }));
            }

            View blockRate = fragment.container().findViewById(R.id.block_rate);
            if (blockRate != null) {
                blockRate.setOnClickListener(v -> thread.standalone(() -> {
                    log.v(TAG, "block_rate clicked");
                    firebaseAnalyticsProvider.logBasicEvent(activity, "app rate clicked");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.bukhmastov.cdoitmo"))).withIdentity(MARKET_IDENTITY));
                }));
            }

            View blockGithub = fragment.container().findViewById(R.id.block_github);
            if (blockGithub != null) {
                blockGithub.setOnClickListener(v -> thread.standalone(() -> {
                    log.v(TAG, "block_github clicked");
                    firebaseAnalyticsProvider.logBasicEvent(activity, "view github clicked");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bukhmastov/cdoitmo"))));
                }));
            }

            View blockDonate = fragment.container().findViewById(R.id.block_donate);
            if (blockDonate != null) {
                blockDonate.setOnClickListener(v -> thread.standalone(() -> {
                    log.v(TAG, "block_donate clicked  ┬─┬ ノ( ゜-゜ノ)");
                    firebaseAnalyticsProvider.logBasicEvent(activity, "donate clicked");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://yasobe.ru/na/cdoifmo"))));
                }));
            }

            TextView textDisclaimer = fragment.container().findViewById(R.id.text_disclaimer);
            if (textDisclaimer != null) {
                textDisclaimer.setMovementMethod(LinkMovementMethod.getInstance());
            }
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
