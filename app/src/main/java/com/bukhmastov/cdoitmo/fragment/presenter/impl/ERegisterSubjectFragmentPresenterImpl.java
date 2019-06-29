package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.ERegisterSubjectViewRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterSubjectFragmentPresenter;
import com.bukhmastov.cdoitmo.model.eregister.ERMark;
import com.bukhmastov.cdoitmo.model.eregister.ERPoint;
import com.bukhmastov.cdoitmo.model.eregister.ERSubject;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.bukhmastov.cdoitmo.util.Thread.ERS;

public class ERegisterSubjectFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<ERSubject>
        implements ERegisterSubjectFragmentPresenter {

    private static final String TAG = "ERegisterSubjectFragment";
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
    StoragePref storagePref;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ERegisterSubjectFragmentPresenterImpl() {
        super(ERSubject.class);
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.EREGISTER)) {
            return;
        }
        clearData();
    }

    @Override
    public void onStart() {
        super.onStart();
        thread.run(ERS, () -> {
            fragment.setHasOptionsMenu(true);
            Bundle extras = fragment.getArguments();
            if (extras == null) {
                loadFailed();
                return;
            }
            setData((ERSubject) extras.getSerializable("subject"));
            if (getData() == null) {
                loadFailed();
                return;
            }
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem simple = menu.findItem(R.id.action_mode_simple);
            MenuItem advanced = menu.findItem(R.id.action_mode_post_process);
            if (simple != null && advanced != null) {
                switch (storagePref.get(activity, "pref_eregister_mode", "advanced")) {
                    case "simple":
                        advanced.setVisible(true);
                        break;
                    case "advanced":
                        simple.setVisible(true);
                        break;
                }
                simple.setOnMenuItemClickListener(item -> {
                    thread.runOnUI(ERS, () -> {
                        storagePref.put(activity, "pref_eregister_mode", "simple");
                        simple.setVisible(false);
                        advanced.setVisible(true);
                        display();
                    });
                    return false;
                });
                advanced.setOnMenuItemClickListener(item -> {
                    thread.runOnUI(ERS, () -> {
                        storagePref.put(activity, "pref_eregister_mode", "advanced");
                        simple.setVisible(true);
                        advanced.setVisible(false);
                        display();
                    });
                    return false;
                });
            }
            thread.run(ERS, () -> {
                if (share == null) {
                    return;
                }
                ERSubject data = getData();
                if (data == null || StringUtils.isBlank(data.getName()) || CollectionUtils.isEmpty(data.getMarks()) || CollectionUtils.isEmpty(data.getPoints())) {
                    thread.runOnUI(ERS, () -> share.setVisible(false));
                    return;
                }
                thread.runOnUI(ERS, () -> {
                    share.setVisible(true);
                    share.setOnMenuItemClickListener(menuItem -> {
                        share();
                        return true;
                    });
                });
            });
        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    protected void load() {
        display();
    }

    protected void display() {
        thread.run(ERS, () -> {
            log.v(TAG, "display");
            ERSubject data = getData();
            if (data == null) {
                return;
            }
            ERegisterSubjectViewRVA adapter = new ERegisterSubjectViewRVA(activity, data, "advanced".equals(storagePref.get(activity, "pref_eregister_mode", "advanced")));
            thread.runOnUI(ERS, () -> {
                // отображаем заголовок
                activity.updateToolbar(activity, data.getName(), R.drawable.ic_e_journal);
                // отображаем список
                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
                final RecyclerView recyclerView = fragment.container().findViewById(R.id.points_list);
                if (recyclerView == null) {
                    loadFailed();
                    return;
                }
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.setAdapter(adapter);
                recyclerView.setHasFixedSize(true);
            }, throwable -> {
                log.exception(throwable);
                loadFailed();
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(ERS, () -> {
            log.v(TAG, "loadFailed");
            activity.draw(R.layout.state_failed_text);
            TextView message = activity.findViewById(R.id.text);
            if (message != null) {
                message.setText(R.string.error_occurred);
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void share() {
        thread.standalone(() -> {
            ERSubject data = getData();
            if (data == null || StringUtils.isBlank(data.getName()) || CollectionUtils.isEmpty(data.getMarks()) || CollectionUtils.isEmpty(data.getPoints())) {
                return;
            }
            List<ShareEntity> shareEntities = makeShareEntities(data);
            if (CollectionUtils.isEmpty(shareEntities)) {
                return;
            }
            thread.runOnUI(ERS, () -> {
                if (shareEntities.size() == 1) {
                    eventBus.fire(new ShareTextEvent(shareEntities.get(0).text, "txt_eregister_subject"));
                    return;
                }
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                ArrayList<String> labels = new ArrayList<>();
                for (ShareEntity shareEntity : shareEntities) {
                    labels.add(StringUtils.isNotBlank(shareEntity.attestation) ? shareEntity.attestation : data.getName());
                }
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.spinner_center);
                arrayAdapter.addAll(labels);
                new AlertDialog.Builder(activity)
                        .setAdapter(arrayAdapter, (dialogInterface, position) -> {
                            eventBus.fire(new ShareTextEvent(shareEntities.get(position).text, "txt_eregister_subject"));
                        })
                        .setNegativeButton(R.string.do_cancel, null)
                        .create().show();
            }, throwable -> {
                log.exception(throwable);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            });
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private List<ShareEntity> makeShareEntities(ERSubject data) {
        List<ShareEntity> shareEntities = new ArrayList<>();
        if (data == null || StringUtils.isBlank(data.getName()) || CollectionUtils.isEmpty(data.getMarks()) || CollectionUtils.isEmpty(data.getPoints())) {
            return shareEntities;
        }
        for (ERMark mark : data.getMarks()) {
            Double value = -1.0;
            for (ERPoint point : data.getPoints()) {
                if (point == null || point.getValue() == null || point.getValue() < value) {
                    continue;
                }
                if (Objects.equals(mark.getWorkType(), point.getName())) {
                    value = point.getValue();
                    continue;
                }
                if (point.getMax() != null && point.getMax() == 100.0) {
                    value = point.getValue();
                    continue;
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
            shareEntity.text = getShareText(data.getName(), shareEntity);
            shareEntities.add(shareEntity);
        }
        if (shareEntities.size() == 0) {
            Double value = -1.0;
            for (ERPoint point : data.getPoints()) {
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
                shareEntity.text = getShareText(data.getName(), shareEntity);
                shareEntities.add(shareEntity);
            }
        }
        return shareEntities;
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

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCacheType() {
        return null;
    }

    @Override
    protected String getCachePath() {
        return null;
    }

    @Override
    protected String getThreadToken() {
        return ERS;
    }
}
