package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ERegisterSubjectRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ATTESTATION = 0;
    private static final int TYPE_POINT_HIGHLIGHT = 1;
    private static final int TYPE_POINT = 2;
    private static final int TYPE_NO_POINTS = 3;

    private static Pattern patternHighlight = Pattern.compile("^зач[её]т$|^экзамен$|^модуль\\s\\d+$|^промежуточная\\sаттестация$|^защита\\s(кп/кр|кп|кр|курсового\\sпроекта|курсовой\\sработы|курсового\\sпроекта/курсовой\\sработы)$", Pattern.CASE_INSENSITIVE);
    private final ArrayList<Item> dataset = new ArrayList<>();
    private class Item {
        public int type;
        public JSONObject data;
        public boolean separator_top = true;
        public boolean separator_bottom = true;
        public boolean margin_top = true;
        public Item (int type, JSONObject data) {
            this.type = type;
            this.data = data;
        }
    }
    private class ViewHolder extends RecyclerView.ViewHolder {
        protected final ViewGroup container;
        ViewHolder(ViewGroup container) {
            super(container);
            this.container = container;
        }
    }

    public ERegisterSubjectRecyclerViewAdapter(Context context, JSONObject subject, int term) {
        addItems(json2dataset(context, subject, term));
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataset.get(position).type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        switch (viewType) {
            case TYPE_ATTESTATION: layout = R.layout.layout_subject_show_mark; break;
            case TYPE_POINT_HIGHLIGHT: layout = R.layout.layout_subject_show_item_highlight; break;
            case TYPE_POINT: layout = R.layout.layout_subject_show_item; break;
            case TYPE_NO_POINTS: layout = R.layout.nothing_to_display; break;
            default: return null;
        }
        return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = dataset.get(position);
        switch (item.type) {
            case TYPE_ATTESTATION: bindAttestation(holder, item); break;
            case TYPE_POINT_HIGHLIGHT: bindPoint(holder, item, true); break;
            case TYPE_POINT: bindPoint(holder, item, false); break;
            case TYPE_NO_POINTS: bindNoPoints(holder, item); break;
        }
    }

    private void bindAttestation(RecyclerView.ViewHolder holder, Item item) {
        try {
            setTextToTextView(holder.itemView, R.id.name, item.data.getString("name"));
            setTextToTextView(holder.itemView, R.id.term, item.data.getString("term"));
            setTextToTextView(holder.itemView, R.id.mark, item.data.getString("mark"));
            setTextToTextView(holder.itemView, R.id.value, item.data.getString("value"));
            holder.itemView.findViewById(R.id.separator_top).setVisibility(item.separator_top ? View.VISIBLE : View.GONE);
            holder.itemView.findViewById(R.id.separator_bottom).setVisibility(item.separator_bottom ? View.VISIBLE : View.GONE);
            holder.itemView.findViewById(R.id.margin_top).setVisibility(item.margin_top ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPoint(RecyclerView.ViewHolder holder, Item item, boolean hightlight) {
        try {
            String name = item.data.getString("name");
            String value = item.data.getString("value");
            String limit = item.data.getString("limit");
            String max = item.data.getString("max");
            ((TextView) holder.itemView.findViewById(R.id.name)).setText(name.isEmpty() ? Static.GLITCH : name);
            ((TextView) holder.itemView.findViewById(R.id.about)).setText("[0 / " + limit + " / " + max + "]");
            ((TextView) holder.itemView.findViewById(R.id.value)).setText(value);
            if (hightlight) {
                holder.itemView.findViewById(R.id.separator_top).setVisibility(item.separator_top ? View.VISIBLE : View.GONE);
            }
            holder.itemView.findViewById(R.id.separator_bottom).setVisibility(item.separator_bottom ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNoPoints(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(R.string.no_points);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void setTextToTextView(View root, @IdRes int id, String text) throws Exception {
        TextView tv = root.findViewById(id);
        if (!text.isEmpty()) {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    public ArrayList<Item> json2dataset(Context context, JSONObject subject, int term) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            final JSONArray attestations = subject.getJSONArray("attestations");
            if (attestations.length() == 0) {
                dataset.add(new Item(TYPE_NO_POINTS, null));
            } else {
                for (int i = 0; i < attestations.length(); i++) {
                    final JSONObject attestation = attestations.getJSONObject(i);
                    final double value = attestation.getDouble("value");
                    dataset.add(new Item(TYPE_ATTESTATION, new JSONObject()
                            .put("name", attestation.getString("name"))
                            .put("mark", attestation.getString("mark"))
                            .put("value", value < 0.0 ? "" : markConverter(String.valueOf(value)))
                            .put("term", term + " " + context.getString(R.string.semester))
                    ));
                    final JSONArray points = attestation.getJSONArray("points");
                    if (points.length() == 0) {
                        dataset.add(new Item(TYPE_NO_POINTS, null));
                    } else {
                        for (int j = 0; j < points.length(); j++) {
                            final JSONObject point = points.getJSONObject(j);
                            final String pName = point.getString("name");
                            final double pValue = point.getDouble("value");
                            final double pLimit = point.getDouble("limit");
                            final double pMax = point.getDouble("max");
                            dataset.add(new Item(patternHighlight.matcher(pName).find() ? TYPE_POINT_HIGHLIGHT : TYPE_POINT, new JSONObject()
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
                    item.separator_top = false;
                }
                if (i + 1 < size) {
                    Item itemNext = dataset.get(i + 1);
                    if (item.type == TYPE_ATTESTATION && itemNext.type == TYPE_POINT_HIGHLIGHT) {
                        item.separator_bottom = false;
                    }
                    if (item.type == TYPE_POINT_HIGHLIGHT && itemNext.type == TYPE_ATTESTATION) {
                        item.separator_bottom = false;
                    }
                    if (item.type == TYPE_POINT && itemNext.type == TYPE_ATTESTATION || itemNext.type == TYPE_POINT_HIGHLIGHT) {
                        item.separator_bottom = false;
                    }
                    if (item.type != TYPE_NO_POINTS && itemNext.type == TYPE_NO_POINTS) {
                        item.separator_bottom = false;
                    }
                }
                if (i - 1 >= 0) {
                    Item itemPrevious = dataset.get(i - 1);
                    if (item.type != TYPE_NO_POINTS && itemPrevious.type == TYPE_NO_POINTS) {
                        item.separator_top = false;
                    }
                }
                if (i + 2 < size && i - 1 >= 0) {
                    Item itemPrevious = dataset.get(i - 1);
                    Item itemNext = dataset.get(i + 1);
                    Item itemAfterNext = dataset.get(i + 2);
                    if (itemPrevious.type == TYPE_ATTESTATION && item.type == TYPE_NO_POINTS && itemNext.type == TYPE_ATTESTATION && itemAfterNext.type == TYPE_NO_POINTS) {
                        itemNext.margin_top = false;
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
    public void addItem(Item item) {
        this.dataset.add(item);
        this.notifyItemInserted(this.dataset.size() - 1);
    }
    public void addItems(ArrayList<Item> dataset) {
        int itemStart = this.dataset.size() - 1;
        this.dataset.addAll(dataset);
        this.notifyItemRangeInserted(itemStart, dataset.size() - 1);
    }
    public void removeItem(int position) {
        this.dataset.remove(position);
        this.notifyItemRemoved(position);
        this.notifyItemRangeChanged(position, this.dataset.size() - 1);
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
