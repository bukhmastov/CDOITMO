package com.bukhmastov.cdoitmo.adapters.rva;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class RatingListRVA extends RecyclerViewAdapter {

    private static final int TYPE_ENTRY = 0;
    private static final int TYPE_ENTRY_MINE = 1;
    private static final int TYPE_NO_RATING = 2;

    private final Context context;
    private final SparseIntArray colors = new SparseIntArray();

    public RatingListRVA(@NonNull Context context, @NonNull JSONArray data) {
        this.context = context;
        addItems(json2dataset(context, data));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_ENTRY: layout = R.layout.layout_rating_list_item; break;
            case TYPE_ENTRY_MINE: layout = R.layout.layout_rating_list_item_mine; break;
            case TYPE_NO_RATING: layout = R.layout.nothing_to_display; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_ENTRY: bindEntry(container, item, false); break;
            case TYPE_ENTRY_MINE: bindEntry(container, item, true); break;
            case TYPE_NO_RATING: bindNoRating(container, item); break;
        }
    }

    private ArrayList<Item> json2dataset(@NonNull final Context context, @NonNull final JSONArray data) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            if (data.length() == 0) {
                dataset.add(getNewItem(TYPE_NO_RATING, null));
            } else {
                for (int i = 0; i < data.length(); i++) {
                    final JSONObject entry = data.getJSONObject(i);
                    dataset.add(getNewItem(entry.getBoolean("mine") ? TYPE_ENTRY_MINE : TYPE_ENTRY, entry));
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return dataset;
    }

    private void bindEntry(View container, Item item, boolean mine) {
        try {
            // fetch
            final int dPosition = item.data.getInt("position");
            final String dFio = item.data.getString("fio");
            final String dMeta = item.data.getString("meta");
            final String dChange = item.data.getString("change");
            final String dDelta = item.data.getString("delta");
            // draw
            final ImageView crown = container.findViewById(R.id.crown);
            final TextView position = container.findViewById(R.id.position);
            final TextView fio = container.findViewById(R.id.fio);
            final TextView meta = container.findViewById(R.id.meta);
            final ViewGroup delta_container = container.findViewById(R.id.delta_container);
            final TextView delta = container.findViewById(R.id.delta);
            if (mine) {
                final View its_me_1 = container.findViewById(R.id.its_me_1);
                final View its_me_2 = container.findViewById(R.id.its_me_2);
                int color;
                if (dPosition > 0 && dPosition < 4) {
                    switch (dPosition) {
                        case 1: color = R.attr.colorGold; break;
                        case 2: color = R.attr.colorSilver; break;
                        default: case 3: color = R.attr.colorBronze; break;
                    }
                    color = resolveColor(color);
                    position.setVisibility(View.GONE);
                    crown.setVisibility(View.VISIBLE);
                    crown.setImageTintList(ColorStateList.valueOf(resolveColor(R.attr.colorRatingHighlightText)));
                } else {
                    color = resolveColor(R.attr.colorRatingHighlightBackground);
                    crown.setVisibility(View.GONE);
                    position.setVisibility(View.VISIBLE);
                    position.setText(String.valueOf(dPosition));
                    position.setTextColor(resolveColor(R.attr.colorRatingHighlightText));
                }
                its_me_1.setBackgroundColor(color);
                its_me_2.setBackgroundColor(color);
                container.setBackgroundColor(getColorWithAlpha(color, 60));
            } else {
                if (dPosition > 0 && dPosition < 4) {
                    crown.setVisibility(View.VISIBLE);
                    position.setVisibility(View.GONE);
                    int color;
                    switch (dPosition) {
                        case 1: color = R.attr.colorGold; break;
                        case 2: color = R.attr.colorSilver; break;
                        default: case 3: color = R.attr.colorBronze; break;
                    }
                    crown.setImageTintList(ColorStateList.valueOf(resolveColor(color)));
                } else {
                    crown.setVisibility(View.GONE);
                    position.setVisibility(View.VISIBLE);
                    position.setTextColor(resolveColor(android.R.attr.textColorPrimary));
                    position.setText(String.valueOf(dPosition));
                }
                container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            }
            fio.setText(dFio);
            meta.setText(dMeta);
            if (delta != null) {
                if (!"none".equals(dChange)) {
                    delta_container.setVisibility(View.VISIBLE);
                    delta.setText(dDelta);
                    switch (dChange) {
                        case "up": delta.setTextColor(resolveColor(R.attr.colorPositiveTrend)); break;
                        case "down": delta.setTextColor(resolveColor(R.attr.colorNegativeTrend)); break;
                    }
                } else {
                    delta_container.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNoRating(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_rating);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private int resolveColor(int reference) throws Exception {
        int saved = colors.get(reference, -1);
        if (saved != -1) {
            return saved;
        }
        int color = Static.resolveColor(context, reference);
        colors.put(reference, color);
        return color;
    }
    private int getColorWithAlpha(int color, int alpha) {
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);
        return Color.argb(alpha, red, green, blue);
    }
}
