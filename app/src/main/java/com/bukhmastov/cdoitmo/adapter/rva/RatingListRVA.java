package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.rating.top.RStudent;
import com.bukhmastov.cdoitmo.model.rating.top.RatingTopList;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;

import java.util.ArrayList;

public class RatingListRVA extends RVA<RStudent> {

    private static final int TYPE_ENTRY = 0;
    private static final int TYPE_ENTRY_MINE = 1;
    private static final int TYPE_NO_RATING = 2;

    private final Context context;
    private final boolean isShowBeer;
    private final SparseIntArray colors = new SparseIntArray();

    public RatingListRVA(@NonNull Context context, @NonNull RatingTopList rating, boolean isShowBeer) {
        super();
        this.context = context;
        this.isShowBeer = isShowBeer;
        addItems(entity2dataset(rating));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_ENTRY: layout = R.layout.layout_rating_list_item; break;
            case TYPE_ENTRY_MINE: layout = R.layout.layout_rating_list_item_mine; break;
            case TYPE_NO_RATING: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_ENTRY:
            case TYPE_ENTRY_MINE: bindEntry(container, item); break;
            case TYPE_NO_RATING: bindNoRating(container); break;
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull RatingTopList rating) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            if (CollectionUtils.isEmpty(rating.getStudents())) {
                dataset.add(new Item(TYPE_NO_RATING));
            } else {
                for (RStudent student : rating.getStudents()) {
                    dataset.add(new Item<>(student.isMe() ? TYPE_ENTRY_MINE : TYPE_ENTRY, student));
                }
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
        return dataset;
    }

    private void bindEntry(View container, Item<RStudent> item) {
        try {
            final ImageView crown = container.findViewById(R.id.crown);
            final TextView position = container.findViewById(R.id.position);
            final TextView fio = container.findViewById(R.id.fio);
            final TextView meta = container.findViewById(R.id.meta);
            final ViewGroup delta_container = container.findViewById(R.id.delta_container);
            final TextView delta = container.findViewById(R.id.delta);
            if (item.data.isMe()) {
                final View its_me_1 = container.findViewById(R.id.its_me_1);
                final View its_me_2 = container.findViewById(R.id.its_me_2);
                int color;
                if (item.data.getNumber() > 0 && item.data.getNumber() < 4) {
                    switch (item.data.getNumber()) {
                        case 1: color = R.attr.colorGold; break;
                        case 2: color = R.attr.colorSilver; break;
                        default: case 3: color = R.attr.colorBronze; break;
                    }
                    color = resolveColor(color);
                    position.setVisibility(View.GONE);
                    crown.setVisibility(View.VISIBLE);
                    crown.setImageResource(isShowBeer ? R.drawable.ic_beer : R.drawable.ic_crown);
                    crown.setImageTintList(ColorStateList.valueOf(resolveColor(R.attr.colorRatingHighlightText)));
                } else {
                    color = resolveColor(R.attr.colorRatingHighlightBackground);
                    crown.setVisibility(View.GONE);
                    position.setVisibility(View.VISIBLE);
                    position.setText(String.valueOf(item.data.getNumber()));
                    position.setTextColor(resolveColor(R.attr.colorRatingHighlightText));
                }
                its_me_1.setBackgroundColor(color);
                its_me_2.setBackgroundColor(color);
                container.setBackgroundColor(getColorWithAlpha(color, 60));
            } else {
                if (item.data.getNumber() > 0 && item.data.getNumber() < 4) {
                    int color;
                    switch (item.data.getNumber()) {
                        case 1: color = R.attr.colorGold; break;
                        case 2: color = R.attr.colorSilver; break;
                        default: case 3: color = R.attr.colorBronze; break;
                    }
                    position.setVisibility(View.GONE);
                    crown.setVisibility(View.VISIBLE);
                    crown.setImageResource(isShowBeer ? R.drawable.ic_beer : R.drawable.ic_crown);
                    crown.setImageTintList(ColorStateList.valueOf(resolveColor(color)));
                } else {
                    crown.setVisibility(View.GONE);
                    position.setVisibility(View.VISIBLE);
                    position.setTextColor(resolveColor(android.R.attr.textColorPrimary));
                    position.setText(String.valueOf(item.data.getNumber()));
                }
                container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            }
            fio.setText(item.data.getFio());
            meta.setText(item.data.getGroup() + " — " + item.data.getDepartment());
            if (delta != null) {
                if (!"none".equals(item.data.getChange())) {
                    delta_container.setVisibility(View.VISIBLE);
                    delta.setText(item.data.getDelta());
                    switch (item.data.getChange()) {
                        case "up": delta.setTextColor(resolveColor(R.attr.colorPositiveTrend)); break;
                        case "down": delta.setTextColor(resolveColor(R.attr.colorNegativeTrend)); break;
                    }
                } else {
                    delta_container.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindNoRating(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_rating);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private int resolveColor(int reference) {
        int saved = colors.get(reference, -1);
        if (saved != -1) {
            return saved;
        }
        int color = com.bukhmastov.cdoitmo.util.singleton.Color.resolve(context, reference);
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
