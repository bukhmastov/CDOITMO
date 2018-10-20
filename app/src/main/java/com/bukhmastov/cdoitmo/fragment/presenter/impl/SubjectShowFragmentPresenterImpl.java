package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ERegisterSubjectViewRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.SubjectShowFragmentPresenter;
import com.bukhmastov.cdoitmo.model.eregister.ERMark;
import com.bukhmastov.cdoitmo.model.eregister.ERPoint;
import com.bukhmastov.cdoitmo.model.eregister.ERSubject;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Objects;

import javax.inject.Inject;

public class SubjectShowFragmentPresenterImpl implements SubjectShowFragmentPresenter {

    private static final String TAG = "SubjectShowFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private ERSubject subject = null;
    private class ShareEntity {
        public String attestation = "";
        public String mark = "";
        public Double value = -1.0;
        public String text = "";
    }

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public SubjectShowFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            fragment.setHasOptionsMenu(true);
            Bundle extras = fragment.getArguments();
            if (extras == null) {
                back();
                return;
            }
            subject = (ERSubject) extras.getSerializable("subject");
            if (subject == null) {
                back();
            }
        }, throwable -> {
            log.exception(throwable);
            back();
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment destroyed");
            if (activity != null && activity.toolbar != null) {
                MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                if (action_share != null) {
                    action_share.setVisible(false);
                }
            }
        });
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            if (subject == null) {
                return;
            }
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        });
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
    }

    @Override
    public void onViewCreated() {
        if (subject == null) {
            return;
        }
        display();
    }

    @Override
    public void toggleShare() {
        thread.run(() -> {
            if (subject == null || activity.toolbar == null) {
                return;
            }
            if (StringUtils.isBlank(subject.getName()) || CollectionUtils.isEmpty(subject.getMarks()) || CollectionUtils.isEmpty(subject.getPoints())) {
                return;
            }
            ArrayList<ShareEntity> shareEntities = new ArrayList<>();
            for (ERMark mark : subject.getMarks()) {
                Double value = -1.0;
                for (ERPoint point : subject.getPoints()) {
                    if (point == null) {
                        continue;
                    }
                    if (Objects.equals(mark.getWorkType(), point.getName())) {
                        value = point.getValue();
                        break;
                    }
                }
                if (value == null) {
                    value = 0.0;
                }
                if (value < 0.0) {
                    continue;
                }
                ShareEntity shareEntity = new ShareEntity();
                shareEntity.attestation = mark.getWorkType();
                shareEntity.mark = mark.getMark();
                shareEntity.value = value;
                shareEntity.text = getShareText(subject.getName(), shareEntity);
                shareEntities.add(shareEntity);
            }
            if (shareEntities.size() == 0) {
                Double value = -1.0;
                for (ERPoint point : subject.getPoints()) {
                    if (point == null) {
                        continue;
                    }
                    if (point.getMax() != null && point.getMax() == 100.0) {
                        value = point.getValue();
                        break;
                    }
                }
                if (value >= 0.0) {
                    ShareEntity shareEntity = new ShareEntity();
                    shareEntity.attestation = null;
                    shareEntity.mark = null;
                    shareEntity.value = value;
                    shareEntity.text = getShareText(subject.getName(), shareEntity);
                    shareEntities.add(shareEntity);
                }
            }
            if (shareEntities.size() == 0) {
                return;
            }
            thread.runOnUI(() -> {
                if (activity == null || activity.toolbar == null) {
                    return;
                }
                final MenuItem share = activity.toolbar.findItem(R.id.action_share);
                if (share == null) {
                    return;
                }
                share.setVisible(true);
                share.setOnMenuItemClickListener(menuItem -> {
                    thread.runOnUI(() -> {
                        if (shareEntities.size() == 1) {
                            eventBus.fire(new ShareTextEvent(shareEntities.get(0).text, "subject"));
                            return;
                        }
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        final ArrayList<String> labels = new ArrayList<>();
                        for (ShareEntity shareEntity : shareEntities) {
                            labels.add(StringUtils.isNotBlank(shareEntity.attestation) ? shareEntity.attestation : subject.getName());
                        }
                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.spinner_center);
                        arrayAdapter.addAll(labels);
                        new AlertDialog.Builder(activity)
                                .setAdapter(arrayAdapter, (dialogInterface, position) -> eventBus.fire(new ShareTextEvent(shareEntities.get(position).text, "subject")))
                                .setNegativeButton(R.string.do_cancel, null)
                                .create().show();
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    return false;
                });
            }, throwable -> {
                log.exception(throwable);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            });
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void display() {
        if (subject == null) {
            return;
        }
        thread.run(() -> {
            log.v(TAG, "display");
            ERegisterSubjectViewRVA adapter = new ERegisterSubjectViewRVA(activity, subject, true /* TODO eregister simple mode */);
            thread.runOnUI(() -> {
                // отображаем заголовок
                activity.updateToolbar(activity, subject.getName(), R.drawable.ic_e_journal);
                // отображаем список
                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                final RecyclerView recyclerView = fragment.container().findViewById(R.id.points_list);
                if (recyclerView == null) {
                    activity.back();
                    return;
                }
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.setAdapter(adapter);
                recyclerView.setHasFixedSize(true);
            }, throwable -> {
                log.exception(throwable);
                activity.back();
            });
        }, throwable -> {
            log.exception(throwable);
            activity.back();
        });
    }

    private void back() {
        subject = null;
        activity.back();
    }
    
    private String getShareText(String subjectName, ShareEntity shareEntity) {
        // prettify points value
        String points = "";
        int iValue = shareEntity.value.intValue();
        if (shareEntity.value != -1.0) {
            if (shareEntity.value == Double.parseDouble(iValue + ".0")) {
                points = String.valueOf(iValue);
            } else {
                points = String.valueOf(shareEntity.value);
            }
        }
        // fetch attestation name
        String attestation = StringUtils.isBlank(shareEntity.attestation) ? "" : " (" + shareEntity.attestation + ")";
        // build text to share based on points and/or mark
        if (StringUtils.isNotBlank(points) && StringUtils.isNotBlank(shareEntity.mark)) {
            String title = "У меня %points% балл%suffix% и оценка \"%mark%\" по предмету \"%subject%%attestation%\"!";
            String suffix = "ов";
            if (!(iValue % 100 >= 10 && iValue % 100 < 20)) {
                switch (iValue % 10) {
                    case 1: suffix = ""; break;
                    case 2: case 3: case 4: suffix = "а"; break;
                }
            }
            return title
                    .replace("%points%", points)
                    .replace("%suffix%", suffix)
                    .replace("%mark%", shareEntity.mark)
                    .replace("%subject%", subjectName)
                    .replace("%attestation%", attestation);
        } else if (StringUtils.isNotBlank(points)) {
            String title = "У меня %points% балл%suffix% по предмету \"%subject%%attestation%\"!";
            String suffix = "ов";
            if (!(iValue % 100 >= 10 && iValue % 100 < 20)) {
                switch (iValue % 10) {
                    case 1: suffix = ""; break;
                    case 2: case 3: case 4: suffix = "а"; break;
                }
            }
            return title
                    .replace("%points%", points)
                    .replace("%suffix%", suffix)
                    .replace("%subject%", subjectName)
                    .replace("%attestation%", attestation);
        } else if (StringUtils.isNotBlank(shareEntity.mark)) {
            String title = "У меня \"%mark%\" по предмету \"%subject%%attestation%\"!";
            return title
                    .replace("%mark%", shareEntity.mark)
                    .replace("%subject%", subjectName)
                    .replace("%attestation%", attestation);
        } else {
            log.w(TAG, "Failed to build text to share (mine subject progress) | subject=" + subjectName + " | points=" + shareEntity.value.toString() + " | mark=" + shareEntity.mark);
            return "";
        }
    }
}
