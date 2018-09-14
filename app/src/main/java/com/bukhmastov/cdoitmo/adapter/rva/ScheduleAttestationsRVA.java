package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.JsonUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class ScheduleAttestationsRVA extends RVA {

    private static final String TAG = "ScheduleAttestationsRVA";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SUBJECT = 1;
    private static final int TYPE_ATTESTATION_REGULAR = 2;
    private static final int TYPE_ATTESTATION_BOTTOM = 3;
    private static final int TYPE_UPDATE_TIME = 4;
    private static final int TYPE_NO_ATTESTATIONS = 5;

    @Inject
    Thread thread;
    @Inject
    ScheduleAttestations scheduleAttestations;
    @Inject
    Storage storage;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;

    private final ConnectedActivity activity;
    private final JSONObject data;
    private String query = null;

    public ScheduleAttestationsRVA(final ConnectedActivity activity, JSONObject data, int weekday) {
        super();
        AppComponentProvider.getComponent().inject(this);
        this.activity = activity;
        this.data = data;
        try {
            query = data.getString("query");
            addItems(json2dataset(activity, data, weekday));
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_schedule_both_header; break;
            case TYPE_SUBJECT: layout = R.layout.layout_schedule_attestations_subject; break;
            case TYPE_ATTESTATION_REGULAR: layout = R.layout.layout_schedule_attestations_item_regular; break;
            case TYPE_ATTESTATION_BOTTOM: layout = R.layout.layout_schedule_attestations_item_bottom; break;
            case TYPE_UPDATE_TIME: layout = R.layout.layout_schedule_both_update_time; break;
            case TYPE_NO_ATTESTATIONS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: bindHeader(container, item); break;
            case TYPE_SUBJECT: bindSubject(container, item); break;
            case TYPE_ATTESTATION_REGULAR:
            case TYPE_ATTESTATION_BOTTOM: bindAttestation(container, item); break;
            case TYPE_UPDATE_TIME: bindUpdateTime(container, item); break;
            case TYPE_NO_ATTESTATIONS: bindNoAttestations(container, item); break;
        }
    }

    private ArrayList<Item> json2dataset(@NonNull final Context context, @NonNull final JSONObject data, int weekday) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            // check
            if (!ScheduleAttestations.TYPE.equals(data.getString("schedule_type"))) {
                return dataset;
            }
            // header
            dataset.add(getNewItem(TYPE_HEADER, new JSONObject()
                    .put("title", scheduleAttestations.getScheduleHeader(context, data.getString("title"), data.getString("type")))
                    .put("week", scheduleAttestations.getScheduleWeek(context, weekday))
            ));
            // schedule
            final JSONArray schedule = data.getJSONArray("schedule");
            int attestations_count = 0;
            for (int i = 0; i < schedule.length(); i++) {
                final JSONObject subject = schedule.getJSONObject(i);
                final String name = subject.getString("name");
                final int term = subject.getInt("term");
                final JSONArray attestations = subject.getJSONArray("attestations");
                final int length = attestations.length();
                if (length == 0) {
                    continue;
                }
                dataset.add(getNewItem(TYPE_SUBJECT, new JSONObject()
                        .put("name", name)
                        .put("term", term == 1 ? context.getString(R.string.term_autumn) : context.getString(R.string.term_spring))
                ));
                for (int j = 0; j < length; j++) {
                    final JSONObject attestation = attestations.getJSONObject(j);
                    dataset.add(getNewItem(j < length - 1 ? TYPE_ATTESTATION_REGULAR : TYPE_ATTESTATION_BOTTOM, attestation));
                    attestations_count++;
                }
            }
            if (attestations_count == 0) {
                dataset.add(getNewItem(TYPE_NO_ATTESTATIONS, null));
            } else {
                // update time
                dataset.add(new Item(TYPE_UPDATE_TIME, new JSONObject().put("text", context.getString(R.string.update_date) + " " + time.getUpdateTime(context, data.getLong("timestamp")))));
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }

    private void bindHeader(View container, Item item) {
        try {
            final String title = JsonUtils.getString(item.data, "title");
            final String week = JsonUtils.getString(item.data, "week");
            TextView schedule_lessons_header = container.findViewById(R.id.schedule_lessons_header);
            if (title != null && !title.isEmpty()) {
                schedule_lessons_header.setText(title);
            } else {
                ((ViewGroup) schedule_lessons_header.getParent()).removeView(schedule_lessons_header);
            }
            TextView schedule_lessons_week = container.findViewById(R.id.schedule_lessons_week);
            if (week != null && !week.isEmpty()) {
                schedule_lessons_week.setText(week);
            } else {
                ((ViewGroup) schedule_lessons_week.getParent()).removeView(schedule_lessons_week);
            }
            container.findViewById(R.id.schedule_lessons_menu).setOnClickListener(view -> thread.run(() -> {
                final String cache_token = query == null ? null : query.toLowerCase();
                final boolean cached = cache_token != null && !storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cache_token, "").isEmpty();
                thread.runOnUI(() -> {
                    try {
                        final PopupMenu popup = new PopupMenu(activity, view);
                        final Menu menu = popup.getMenu();
                        popup.getMenuInflater().inflate(R.menu.schedule_attestations, menu);
                        menu.findItem(cached ? R.id.add_to_cache : R.id.remove_from_cache).setVisible(false);
                        popup.setOnMenuItemClickListener(item1 -> {
                            log.v(TAG, "menu | popup item | clicked | " + item1.getTitle().toString());
                            switch (item1.getItemId()) {
                                case R.id.add_to_cache:
                                case R.id.remove_from_cache: {
                                    try {
                                        if (cache_token == null) {
                                            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                                        } else {
                                            if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cache_token)) {
                                                if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cache_token)) {
                                                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                                                } else {
                                                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                                                }
                                            } else {
                                                if (data == null) {
                                                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                                                } else {
                                                    if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cache_token, data.toString())) {
                                                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
                                                    } else {
                                                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.exception(e);
                                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                                    }
                                    break;
                                }
                                case R.id.open_settings: {
                                    activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleAttestationsFragment.class, null);
                                    break;
                                }
                            }
                            return false;
                        });
                        popup.show();
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                });
            }));
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindSubject(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.title)).setText(item.data.getString("name"));
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getString("term"));
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindAttestation(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getString("name"));
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getString("week"));
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindUpdateTime(View container, Item item) {
        try {
            final String text = JsonUtils.getString(item.data, "text");
            ((TextView) container.findViewById(R.id.update_time)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoAttestations(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_attestations);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
