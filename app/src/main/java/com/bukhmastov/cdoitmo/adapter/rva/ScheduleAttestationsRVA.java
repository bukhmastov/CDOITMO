package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.rva.RVAAttestations;
import com.bukhmastov.cdoitmo.model.rva.RVADualValue;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestation;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestations;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SSubject;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

public class ScheduleAttestationsRVA extends RVA<RVAAttestations> {

    private static final String TAG = "ScheduleAttestationsRVA";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SUBJECT = 1;
    private static final int TYPE_ATTESTATION_REGULAR = 2;
    private static final int TYPE_ATTESTATION_BOTTOM = 3;
    private static final int TYPE_UPDATE_TIME = 4;
    private static final int TYPE_NO_ATTESTATIONS = 5;

    @Inject
    Context context;
    @Inject
    ScheduleAttestations scheduleAttestations;
    @Inject
    Time time;

    private final ArrayList<SSubject> subjects = new ArrayList<>();

    public ScheduleAttestationsRVA(SAttestations data, int weekday) {
        super();
        AppComponentProvider.getComponent().inject(this);
        addItems(entity2dataset(data, weekday));
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
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: bindHeader(container, item); break;
            case TYPE_SUBJECT: bindSubject(container, item); break;
            case TYPE_ATTESTATION_REGULAR:
            case TYPE_ATTESTATION_BOTTOM: bindAttestation(container, item); break;
            case TYPE_UPDATE_TIME: bindUpdateTime(container, item); break;
            case TYPE_NO_ATTESTATIONS: bindNoAttestations(container); break;
        }
    }

    private void bindHeader(View container, Item<RVADualValue> item) {
        try {
            String title = item.data.getFirst();
            String week = item.data.getSecond();
            TextView headerView = container.findViewById(R.id.schedule_lessons_header);
            TextView weekView = container.findViewById(R.id.schedule_lessons_week);
            if (StringUtils.isNotBlank(title)) {
                headerView.setText(title);
            } else {
                ((ViewGroup) headerView.getParent()).removeView(headerView);
            }
            if (StringUtils.isNotBlank(week)) {
                weekView.setText(week);
            } else {
                ((ViewGroup) weekView.getParent()).removeView(weekView);
            }
            View createAction = container.findViewById(R.id.schedule_lessons_create);
            if (createAction != null) {
                createAction.setVisibility(View.GONE);
            }
            tryRegisterClickListener(container, R.id.schedule_lessons_menu, null);
            tryRegisterClickListener(container, R.id.schedule_lessons_share, new RVAAttestations(subjects));
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindSubject(View container, Item<RVADualValue> item) {
        try {
            ((TextView) container.findViewById(R.id.title)).setText(item.data.getFirst());
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getSecond());
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindAttestation(View container, Item<SAttestation> item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getName());
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getWeek());
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindUpdateTime(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.update_time)).setText(StringUtils.isNotBlank(item.data.getValue()) ? item.data.getValue() : Static.GLITCH);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindNoAttestations(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_attestations);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull SAttestations schedule, int weekday) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            // check
            if (!ScheduleAttestations.TYPE.equals(schedule.getScheduleType())) {
                return dataset;
            }
            // header
            dataset.add(new Item<>(TYPE_HEADER, new RVADualValue(
                    scheduleAttestations.getScheduleHeader(schedule.getTitle(), schedule.getType()),
                    scheduleAttestations.getScheduleWeek(weekday)
            )));
            // schedule
            int attestations_count = 0;
            for (SSubject subject : schedule.getSchedule()) {
                if (subject == null || CollectionUtils.isEmpty(subject.getAttestations())) {
                    continue;
                }
                subjects.add(subject);
                dataset.add(new Item<>(TYPE_SUBJECT, new RVADualValue(
                        subject.getName(),
                        subject.getTerm() == 1 ? context.getString(R.string.term_autumn) : context.getString(R.string.term_spring)
                )));
                int size = subject.getAttestations().size();
                for (int i = 0; i < size; i++) {
                    SAttestation attestation = subject.getAttestations().get(i);
                    dataset.add(new Item<>(i < size - 1 ? TYPE_ATTESTATION_REGULAR : TYPE_ATTESTATION_BOTTOM, attestation));
                    attestations_count++;
                }
            }
            if (attestations_count == 0) {
                dataset.add(new Item(TYPE_NO_ATTESTATIONS));
            } else {
                // update time
                dataset.add(new Item<>(TYPE_UPDATE_TIME, new RVASingleValue(context.getString(R.string.update_date) + " " + time.getUpdateTime(context, schedule.getTimestamp()))));
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
        return dataset;
    }
}
