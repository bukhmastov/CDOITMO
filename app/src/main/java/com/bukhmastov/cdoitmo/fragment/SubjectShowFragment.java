package com.bukhmastov.cdoitmo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.ERegisterSubjectViewRVA;
import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class SubjectShowFragment extends ConnectedFragment {

    private static final String TAG = "SubjectShowFragment";
    private static Pattern patternExamOrCredit = Pattern.compile("^зач[её]т$|^экзамен$", Pattern.CASE_INSENSITIVE);
    private JSONObject data = null;
    private class ShareEntity {
        public String attestation = "";
        public String mark = "";
        public Double value = -1.0;
        public String text = "";
    }

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private NotificationMessage notificationMessage = NotificationMessage.instance();
    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
        try {
            final Bundle extras = getArguments();
            if (extras == null) {
                throw new NullPointerException("extras are null");
            }
            String data = extras.getString("data");
            if (data == null || data.isEmpty()) {
                throw new Exception("Wrong extras provided: " + extras.toString());
            }
            this.data = new JSONObject(data);
        } catch (Exception e) {
            log.exception(e);
            this.data = null;
            activity.back();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (activity != null && activity.toolbar != null) {
                MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                if (action_share != null) action_share.setVisible(false);
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (data == null) return;
        display();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (data == null) return;
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        toggleShare();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_subject_show;
    }

    @Override
    protected int getRootId() {
        return 0;
    }

    private void display() {
        if (data == null) return;
        thread.run(() -> {
            try {
                log.v(TAG, "display");
                final JSONObject subject = data.getJSONObject("subject");
                final int term = data.getInt("term");
                final ERegisterSubjectViewRVA adapter = new ERegisterSubjectViewRVA(activity, subject, term);
                thread.runOnUI(() -> {
                    try {
                        // отображаем заголовок
                        activity.updateToolbar(activity, subject.getString("name"), R.drawable.ic_e_journal);
                        // отображаем список
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView points_list = container.findViewById(R.id.points_list);
                        if (points_list == null) throw new SilentException();
                        points_list.setLayoutManager(layoutManager);
                        points_list.setAdapter(adapter);
                        points_list.setHasFixedSize(true);
                    } catch (SilentException e) {
                        activity.back();
                    } catch (Exception e) {
                        log.exception(e);
                        activity.back();
                    }
                });
            } catch (Exception e) {
                log.exception(e);
                activity.back();
            }
        });
    }
    private void toggleShare() {
        thread.run(() -> {
            try {
                if (data == null || activity.toolbar == null) return;
                final JSONObject subject = data.getJSONObject("subject");
                final String sbj = subject.getString("name");
                final JSONArray attestations = subject.getJSONArray("attestations");
                if (sbj == null || sbj.isEmpty() || attestations == null || attestations.length() == 0) return;
                final ArrayList<ShareEntity> shareEntities = new ArrayList<>();
                for (int i = 0; i < attestations.length(); i++) {
                    final JSONObject attestation = attestations.getJSONObject(i);
                    final String name = attestation.getString("name");
                    final String mark = attestation.getString("mark");
                    final double value = attestation.getDouble("value");
                    if ((mark == null || mark.isEmpty()) && value <= 0.0) continue;
                    ShareEntity shareEntity = new ShareEntity();
                    shareEntity.attestation = name;
                    shareEntity.mark = mark;
                    shareEntity.value = value;
                    shareEntity.text = getShareText(sbj, shareEntity);
                    shareEntities.add(shareEntity);
                }
                if (shareEntities.size() == 0) return;
                thread.runOnUI(() -> {
                    try {
                        final MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                        if (action_share == null) return;
                        action_share.setVisible(true);
                        action_share.setOnMenuItemClickListener(menuItem -> {
                            try {
                                if (shareEntities.size() == 1) {
                                    share(shareEntities.get(0).text);
                                } else {
                                    final ArrayList<String> labels = new ArrayList<>();
                                    for (ShareEntity shareEntity : shareEntities) {
                                        labels.add(shareEntity.attestation != null && !shareEntity.attestation.isEmpty() ? shareEntity.attestation : sbj);
                                    }
                                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.spinner_center);
                                    arrayAdapter.addAll(labels);
                                    new AlertDialog.Builder(activity)
                                            .setAdapter(arrayAdapter, (dialogInterface, position) -> {
                                                try {
                                                    share(shareEntities.get(position).text);
                                                } catch (Exception e) {
                                                    log.exception(e);
                                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                }
                                            })
                                            .setNegativeButton(R.string.do_cancel, null)
                                            .create().show();
                                }
                            } catch (Exception e) {
                                log.exception(e);
                                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                            return false;
                        });
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                });
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    private String getShareText(String sbj, ShareEntity shareEntity) throws Exception {
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
        String attestation = "";
        if (shareEntity.attestation != null && !shareEntity.attestation.isEmpty() && !patternExamOrCredit.matcher(shareEntity.attestation).find()) {
            attestation = " (" + shareEntity.attestation + ")";
        }
        // build text to share based on points and/or mark
        if (!points.isEmpty() && shareEntity.mark != null && !shareEntity.mark.isEmpty()) {
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
                    .replace("%subject%", sbj)
                    .replace("%attestation%", attestation);
        } else if (!points.isEmpty()) {
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
                    .replace("%subject%", sbj)
                    .replace("%attestation%", attestation);
        } else if (shareEntity.mark != null && !shareEntity.mark.isEmpty()) {
            String title = "У меня \"%mark%\" по предмету \"%subject%%attestation%\"!";
            return title
                    .replace("%mark%", shareEntity.mark)
                    .replace("%subject%", sbj)
                    .replace("%attestation%", attestation);
        } else {
            log.w(TAG, "Failed to build text to share (mine subject progress) | subject=" + sbj + " | points=" + shareEntity.value.toString() + " | mark=" + shareEntity.mark);
            return "";
        }
    }
    private void share(final String title) {
        thread.runOnUI(() -> {
            log.v(TAG, "share | " + title);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, title);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share)));
            // track statistics
            firebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.SHARE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "subject")
            );
        });
    }
}
