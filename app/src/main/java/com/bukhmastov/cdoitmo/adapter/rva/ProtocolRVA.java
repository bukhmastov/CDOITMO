package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.protocol.PChange;
import com.bukhmastov.cdoitmo.model.protocol.Protocol;
import com.bukhmastov.cdoitmo.model.rva.RVAProtocolChange;
import com.bukhmastov.cdoitmo.model.rva.RVAProtocolGroup;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProtocolRVA extends RVA<RVAProtocolChange> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHANGE = 1;
    private static final int TYPE_CHANGE_SIMPLE = 2;
    private static final int TYPE_SEPARATOR = 3;
    private static final int TYPE_NO_CHANGES = 4;

    private int colorNegativeTrend;
    private int colorPositiveTrend;

    public ProtocolRVA(@NonNull Context context, @NonNull Protocol protocol, boolean advancedMode) {
        super();
        addItems(entity2dataset(protocol, advancedMode));
        try {
            colorPositiveTrend = Color.resolve(context, R.attr.colorPositiveTrend);
        } catch (Exception e) {
            colorPositiveTrend = -1;
        }
        try {
            colorNegativeTrend = Color.resolve(context, R.attr.colorNegativeTrend);
        } catch (Exception e) {
            colorNegativeTrend = -1;
        }
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_protocol_item_advanced_header; break;
            case TYPE_CHANGE: layout = R.layout.layout_protocol_item_advanced; break;
            case TYPE_CHANGE_SIMPLE: layout = R.layout.layout_protocol_item_simple; break;
            case TYPE_SEPARATOR: layout = R.layout.separator_padding; break;
            case TYPE_NO_CHANGES: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: bindHeader(container, item); break;
            case TYPE_CHANGE: bindChange(container, item); break;
            case TYPE_CHANGE_SIMPLE: bindChangeSimple(container, item); break;
            case TYPE_NO_CHANGES: bindNoChanges(container); break;
            case TYPE_SEPARATOR: break;
        }
    }

    private void bindHeader(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getValue());
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindChange(View container, Item<RVAProtocolChange> item) {
        try {
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getDesc());
            ((TextView) container.findViewById(R.id.meta)).setText(item.data.getMeta());
            ((TextView) container.findViewById(R.id.value)).setText(item.data.getValue());
            TextView delta = container.findViewById(R.id.delta);
            if (item.data.isDeltaExists()) {
                delta.setVisibility(View.VISIBLE);
                delta.setText(item.data.getDelta());
                int color = item.data.isDeltaNegative() ? colorNegativeTrend : colorPositiveTrend;
                if (color != -1) {
                    delta.setTextColor(color);
                }
            } else {
                delta.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindChangeSimple(View container, Item<RVAProtocolChange> item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getName());
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getDesc());
            ((TextView) container.findViewById(R.id.meta)).setText(item.data.getMeta());
            ((TextView) container.findViewById(R.id.value)).setText(item.data.getValue());
            TextView delta = container.findViewById(R.id.delta);
            if (item.data.isDeltaExists()) {
                delta.setVisibility(View.VISIBLE);
                delta.setText(item.data.getDelta());
                int color = item.data.isDeltaNegative() ? colorNegativeTrend : colorPositiveTrend;
                if (color != -1) {
                    delta.setTextColor(color);
                }
            } else {
                delta.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoChanges(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_changes_for_period);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull Protocol protocol, boolean advancedMode) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            List<PChange> changes = protocol.getChanges();
            if (CollectionUtils.isEmpty(changes)) {
                dataset.add(new Item(TYPE_NO_CHANGES));
                return dataset;
            }
            if (!advancedMode) {
                // simple mode
                for (PChange change : changes) {
                    if (change == null) {
                        continue;
                    }
                    RVAProtocolChange rvaChange = new RVAProtocolChange();
                    rvaChange.setName(change.getSubject());
                    rvaChange.setDesc(change.getName() + " [" + change.getMin() + "/" + change.getThreshold() + "/" + change.getMax() + "]");
                    rvaChange.setMeta(("..".equals(change.getTeacher()) ? "" : change.getTeacher() + " | ") + change.getDate());
                    rvaChange.setValue(change.getValue());
                    rvaChange.setDelta(change.getCdoitmoDelta());
                    rvaChange.setDeltaExists(change.getCdoitmoDeltaDouble() != null && change.getCdoitmoDeltaDouble() != 0.0);
                    rvaChange.setDeltaNegative(change.getCdoitmoDeltaDouble() != null && change.getCdoitmoDeltaDouble() < 0.0);
                    dataset.add(new Item<>(TYPE_CHANGE_SIMPLE, rvaChange));
                }
            } else {
                // post processing mode
                // Define array of changes by subjects and dates
                SparseArray<RVAProtocolGroup> groups = new SparseArray<>();
                int key = 0;
                for (PChange change : changes) {
                    String subject = change.getSubject();
                    String date = change.getDate();
                    String token = subject + "#" + date;
                    boolean found = false;
                    for (int j = 0; j < groups.size(); j++) {
                        RVAProtocolGroup group = groups.get(groups.keyAt(j));
                        if (token.equals(group.getToken())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        RVAProtocolGroup group = new RVAProtocolGroup();
                        group.setToken(token);
                        group.setSubject(subject);
                        group.setChanges(new ArrayList<>());
                        groups.append(key++, group);
                    }
                    for (int j = 0; j < groups.size(); j++) {
                        RVAProtocolGroup group = groups.get(groups.keyAt(j));
                        if (token.equals(group.getToken())) {
                            group.getChanges().add(change);
                            break;
                        }
                    }
                }
                // Merge identical subjects that ordering one by one
                for (int i = 1; i < groups.size(); i++) {
                    RVAProtocolGroup groupPrevious = groups.get(groups.keyAt(i - 1));
                    RVAProtocolGroup group = groups.get(groups.keyAt(i));
                    if (Objects.equals(group.getSubject(), groupPrevious.getSubject())) {
                        groupPrevious.getChanges().addAll(group.getChanges());
                        group.setChanges(groupPrevious.getChanges());
                        groups.remove(groups.keyAt(i - 1));
                        i--;
                    }
                }
                // Setup dataset
                for (int i = 0; i < groups.size(); i++) {
                    RVAProtocolGroup group = groups.get(groups.keyAt(i));
                    int length = group.getChanges().size();
                    dataset.add(new Item<>(TYPE_HEADER, new RVASingleValue(group.getSubject())));
                    for (int j = 0; j < length; j++) {
                        PChange change = group.getChanges().get(j);
                        RVAProtocolChange rvaChange = new RVAProtocolChange();
                        rvaChange.setDesc(change.getName() + " [" + change.getMin() + "/" + change.getThreshold() + "/" + change.getMax() + "]");
                        rvaChange.setMeta(("..".equals(change.getTeacher()) ? "" : change.getTeacher() + " | ") + change.getDate());
                        rvaChange.setValue(change.getValue());
                        rvaChange.setDelta(change.getCdoitmoDelta());
                        rvaChange.setDeltaExists(change.getCdoitmoDeltaDouble() != null && change.getCdoitmoDeltaDouble() != 0.0);
                        rvaChange.setDeltaNegative(change.getCdoitmoDeltaDouble() != null && change.getCdoitmoDeltaDouble() < 0.0);
                        dataset.add(new Item<>(TYPE_CHANGE, rvaChange));
                        if (j < length - 1) {
                            dataset.add(new Item(TYPE_SEPARATOR));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }
}
