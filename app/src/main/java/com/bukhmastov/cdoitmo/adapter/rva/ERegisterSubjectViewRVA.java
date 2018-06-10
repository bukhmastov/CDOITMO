package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ERegisterSubjectViewRVA extends RVA {

    private static final int TYPE_ATTESTATION = 0;
    private static final int TYPE_POINT_HIGHLIGHT = 1;
    private static final int TYPE_POINT = 2;
    private static final int TYPE_NO_POINTS = 3;

    private static Pattern patternHighlight = Pattern.compile("^зач[её]т$|^экзамен$|^модуль\\s\\d+$|^промежуточная\\sаттестация$|^защита\\s(кп/кр|кп|кр|курсового\\sпроекта|курсовой\\sработы|курсового\\sпроекта/курсовой\\sработы)$", Pattern.CASE_INSENSITIVE);

    public ERegisterSubjectViewRVA(@NonNull Context context, @NonNull JSONObject subject, int term) {
        addItems(json2dataset(context, subject, term));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_ATTESTATION: layout = R.layout.layout_subject_show_mark; break;
            case TYPE_POINT_HIGHLIGHT: layout = R.layout.layout_subject_show_item_highlight; break;
            case TYPE_POINT: layout = R.layout.layout_subject_show_item; break;
            case TYPE_NO_POINTS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_ATTESTATION: bindAttestation(container, item); break;
            case TYPE_POINT_HIGHLIGHT: bindPoint(container, item, true); break;
            case TYPE_POINT: bindPoint(container, item, false); break;
            case TYPE_NO_POINTS: bindNoPoints(container, item); break;
        }
    }

    @Override
    protected Item getNewItem(int type, JSONObject data) {
        final Item item = new Item(type, data);
        item.extras.put("separator_top", true);
        item.extras.put("separator_bottom", true);
        item.extras.put("margin_top", true);
        return item;
    }

    private ArrayList<Item> json2dataset(@NonNull Context context, @NonNull JSONObject subject, int term) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            final JSONArray attestations = subject.getJSONArray("attestations");
            if (attestations.length() == 0) {
                dataset.add(getNewItem(TYPE_NO_POINTS, null));
            } else {
                for (int i = 0; i < attestations.length(); i++) {
                    final JSONObject attestation = attestations.getJSONObject(i);
                    final double value = attestation.getDouble("value");
                    dataset.add(getNewItem(TYPE_ATTESTATION, new JSONObject()
                            .put("name", attestation.getString("name"))
                            .put("mark", attestation.getString("mark"))
                            .put("value", value < 0.0 ? "" : markConverter(String.valueOf(value)))
                            .put("term", term + " " + context.getString(R.string.semester))
                    ));
                    final JSONArray points = attestation.getJSONArray("points");
                    if (points.length() == 0) {
                        dataset.add(getNewItem(TYPE_NO_POINTS, null));
                    } else {
                        for (int j = 0; j < points.length(); j++) {
                            final JSONObject point = points.getJSONObject(j);
                            final String pName = point.getString("name");
                            final double pValue = point.getDouble("value");
                            final double pLimit = point.getDouble("limit");
                            final double pMax = point.getDouble("max");
                            dataset.add(getNewItem(patternHighlight.matcher(pName).find() ? TYPE_POINT_HIGHLIGHT : TYPE_POINT, new JSONObject()
                                    .put("name", pName)
                                    .put("value", pValue < 0.0 ? "0" : markConverter(String.valueOf(pValue)))
                                    .put("limit", pLimit < 0.0 ? "0" : markConverter(String.valueOf(pLimit)))
                                    .put("max", pMax < 0.0 ? "0" : markConverter(String.valueOf(pMax)))
                            ));
                        }
                    }
                }
            }
            int size = dataset.size();
            for (int i = 0; i < size; i++) {
                Item item = dataset.get(i);
                if (i == 0) {
                    item.extras.put("separator_top", false);
                }
                if (i + 1 < size) {
                    Item itemNext = dataset.get(i + 1);
                    if (item.type == TYPE_ATTESTATION && itemNext.type == TYPE_POINT_HIGHLIGHT) {
                        item.extras.put("separator_bottom", false);
                    }
                    if (item.type == TYPE_POINT_HIGHLIGHT && itemNext.type == TYPE_ATTESTATION) {
                        item.extras.put("separator_bottom", false);
                    }
                    if (item.type == TYPE_POINT && itemNext.type == TYPE_ATTESTATION || itemNext.type == TYPE_POINT_HIGHLIGHT) {
                        item.extras.put("separator_bottom", false);
                    }
                    if (item.type != TYPE_NO_POINTS && itemNext.type == TYPE_NO_POINTS) {
                        item.extras.put("separator_bottom", false);
                    }
                }
                if (i - 1 >= 0) {
                    Item itemPrevious = dataset.get(i - 1);
                    if (item.type != TYPE_NO_POINTS && itemPrevious.type == TYPE_NO_POINTS) {
                        item.extras.put("separator_top", false);
                    }
                }
                if (i + 2 < size && i - 1 >= 0) {
                    Item itemPrevious = dataset.get(i - 1);
                    Item itemNext = dataset.get(i + 1);
                    Item itemAfterNext = dataset.get(i + 2);
                    if (itemPrevious.type == TYPE_ATTESTATION && item.type == TYPE_NO_POINTS && itemNext.type == TYPE_ATTESTATION && itemAfterNext.type == TYPE_NO_POINTS) {
                        item.extras.put("margin_top", false);
                        dataset.remove(i);
                        i--;
                        size--;
                    }
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return dataset;
    }

    private void bindAttestation(View container, Item item) {
        try {
            setTextToTextView(container, R.id.name, item.data.getString("name"));
            setTextToTextView(container, R.id.term, item.data.getString("term"));
            setTextToTextView(container, R.id.mark, item.data.getString("mark"));
            setTextToTextView(container, R.id.value, item.data.getString("value"));
            container.findViewById(R.id.separator_top).setVisibility((boolean) item.extras.get("separator_top") ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.separator_bottom).setVisibility((boolean) item.extras.get("separator_bottom") ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.margin_top).setVisibility((boolean) item.extras.get("margin_top") ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPoint(View container, Item item, boolean hightlight) {
        try {
            String name = item.data.getString("name");
            String value = item.data.getString("value");
            String limit = item.data.getString("limit");
            String max = item.data.getString("max");
            ((TextView) container.findViewById(R.id.name)).setText(name.isEmpty() ? Static.GLITCH : name);
            ((TextView) container.findViewById(R.id.about)).setText("[0 / " + limit + " / " + max + "]");
            ((TextView) container.findViewById(R.id.value)).setText(value);
            if (hightlight) {
                container.findViewById(R.id.separator_top).setVisibility((boolean) item.extras.get("separator_top") ? View.VISIBLE : View.GONE);
            }
            container.findViewById(R.id.separator_bottom).setVisibility((boolean) item.extras.get("separator_bottom") ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNoPoints(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_points);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void setTextToTextView(View container, @IdRes int id, String text) throws Exception {
        TextView tv = container.findViewById(id);
        if (!text.isEmpty()) {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }
    private String markConverter(String value) {
        Matcher m;
        value = value.replace(",", ".").trim();
        m = Pattern.compile("^\\.(.+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        m = Pattern.compile("(.+)\\.0$").matcher(value);
        if (m.find()) value = m.group(1);
        return value;
    }
}
