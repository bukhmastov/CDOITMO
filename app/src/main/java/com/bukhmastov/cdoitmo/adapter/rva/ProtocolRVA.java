package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProtocolRVA extends RVA {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHANGE = 1;
    private static final int TYPE_CHANGE_SIMPLE = 2;
    private static final int TYPE_SEPARATOR = 3;
    private static final int TYPE_NO_CHANGES = 4;

    private int colorNegativeTrend;
    private int colorPositiveTrend;

    public ProtocolRVA(@NonNull Context context, @NonNull JSONArray protocol, boolean advancedMode) {
        addItems(json2dataset(context, protocol, advancedMode));
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
            case TYPE_NO_CHANGES: bindNoChanges(container, item); break;
            case TYPE_SEPARATOR: break;
        }
    }

    private ArrayList<Item> json2dataset(@NonNull final Context context, @NonNull final JSONArray protocol, boolean advancedMode) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            if (protocol.length() == 0) {
                dataset.add(getNewItem(TYPE_NO_CHANGES, null));
            } else {
                if (advancedMode) {
                    // Define array of changes by subjects and dates
                    final SparseArray<JSONObject> groups = new SparseArray<>();
                    int key = 0;
                    for (int i = 0; i < protocol.length(); i++) {
                        final JSONObject change = protocol.getJSONObject(i);
                        String subject = change.getString("subject");
                        String date = change.getString("date");
                        String token = subject + "#" + date;
                        boolean found = false;
                        for (int j = 0; j < groups.size(); j++) {
                            JSONObject obj = groups.get(groups.keyAt(j));
                            if (token.equals(obj.getString("token"))) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            groups.append(key++, new JSONObject()
                                    .put("token", token)
                                    .put("subject", subject)
                                    .put("changes", new JSONArray())
                            );
                        }
                        for (int j = 0; j < groups.size(); j++) {
                            JSONObject obj = groups.get(groups.keyAt(j));
                            if (token.equals(obj.getString("token"))) {
                                obj.getJSONArray("changes").put(change);
                                break;
                            }
                        }
                    }
                    // Merge identical subjects that ordering one by one
                    for (int i = 1; i < groups.size(); i++) {
                        JSONObject groupPrevious = groups.get(groups.keyAt(i - 1));
                        JSONObject group = groups.get(groups.keyAt(i));
                        if (group.getString("subject").equals(groupPrevious.getString("subject"))) {
                            JSONArray changesPrevious = groupPrevious.getJSONArray("changes");
                            JSONArray changes = group.getJSONArray("changes");
                            for (int j = 0; j < changes.length(); j++) {
                                changesPrevious.put(changes.getJSONObject(j));
                            }
                            group.put("changes", changesPrevious);
                            groups.remove(groups.keyAt(i - 1));
                            i--;
                        }
                    }
                    // Setup dataset
                    for (int i = 0; i < groups.size(); i++) {
                        final JSONObject group = groups.get(groups.keyAt(i));
                        final String name = group.getString("subject");
                        final JSONArray changes = group.getJSONArray("changes");
                        final int length = changes.length();
                        dataset.add(getNewItem(TYPE_HEADER, new JSONObject().put("name", name)));
                        for (int j = 0; j < length; j++) {
                            final JSONObject change = changes.getJSONObject(j);
                            final JSONObject var = change.getJSONObject("var");
                            dataset.add(getNewItem(TYPE_CHANGE, new JSONObject()
                                    .put("desc", var.getString("name") + " [" + var.getString("min") + "/" + var.getString("threshold") + "/" + var.getString("max") + "]")
                                    .put("meta", ("..".equals(change.getString("sign")) ? "" : change.getString("sign") + " | ") + change.getString("date"))
                                    .put("value", change.getString("value"))
                                    .put("delta", change.getString("cdoitmo_delta"))
                                    .put("delta_exists", change.getDouble("cdoitmo_delta_double") != 0.0)
                                    .put("delta_negative", change.getDouble("cdoitmo_delta_double") < 0.0)
                            ));
                            if (j < length - 1) {
                                dataset.add(getNewItem(TYPE_SEPARATOR, null));
                            }
                        }
                    }
                } else {
                    // Setup dataset
                    for (int i = 0; i < protocol.length(); i++) {
                        final JSONObject change = protocol.getJSONObject(i);
                        final JSONObject var = change.getJSONObject("var");
                        dataset.add(getNewItem(TYPE_CHANGE_SIMPLE, new JSONObject()
                                .put("name", change.getString("subject"))
                                .put("desc", var.getString("name") + " [" + var.getString("min") + "/" + var.getString("threshold") + "/" + var.getString("max") + "]")
                                .put("meta", ("..".equals(change.getString("sign")) ? "" : change.getString("sign") + " | ") + change.getString("date"))
                                .put("value", change.getString("value"))
                                .put("delta", change.getString("cdoitmo_delta"))
                                .put("delta_exists", change.getDouble("cdoitmo_delta_double") != 0.0)
                                .put("delta_negative", change.getDouble("cdoitmo_delta_double") < 0.0)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }

    private void bindHeader(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getString("name"));
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindChange(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getString("desc"));
            ((TextView) container.findViewById(R.id.meta)).setText(item.data.getString("meta"));
            ((TextView) container.findViewById(R.id.value)).setText(item.data.getString("value"));
            TextView delta = container.findViewById(R.id.delta);
            if (item.data.getBoolean("delta_exists")) {
                delta.setVisibility(View.VISIBLE);
                delta.setText(item.data.getString("delta"));
                int color = item.data.getBoolean("delta_negative") ? colorNegativeTrend : colorPositiveTrend;
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
    private void bindChangeSimple(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getString("name"));
            ((TextView) container.findViewById(R.id.desc)).setText(item.data.getString("desc"));
            ((TextView) container.findViewById(R.id.meta)).setText(item.data.getString("meta"));
            ((TextView) container.findViewById(R.id.value)).setText(item.data.getString("value"));
            TextView delta = container.findViewById(R.id.delta);
            if (item.data.getBoolean("delta_exists")) {
                delta.setVisibility(View.VISIBLE);
                delta.setText(item.data.getString("delta"));
                int color = item.data.getBoolean("delta_negative") ? colorNegativeTrend : colorPositiveTrend;
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
    private void bindNoChanges(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_changes_for_period);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
