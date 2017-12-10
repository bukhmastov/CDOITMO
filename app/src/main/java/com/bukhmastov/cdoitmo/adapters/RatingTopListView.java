package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.ArrayList;
import java.util.HashMap;

public class RatingTopListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> users;
    private final SparseIntArray colors = new SparseIntArray();

    public RatingTopListView(Activity context, ArrayList<HashMap<String, String>> users) {
        super(context, R.layout.listview_rating_list, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            final HashMap<String, String> user = users.get(position);
            final int rating = Integer.parseInt(user.get("number"));
            final boolean is_me = user.get("is_me").equals("1");
            final int type = getItemViewType(position);
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater == null) throw new NullPointerException("Inflater is null");
                convertView = inflater.inflate(type == 0 ? R.layout.listview_rating_list : R.layout.listview_rating_list_mine, parent, false);
            }
            final ImageView lvrl_crown = convertView.findViewById(R.id.lvrl_crown);
            final TextView lvrl_number = convertView.findViewById(R.id.lvrl_number);
            final TextView lvrl_fio = convertView.findViewById(R.id.lvrl_fio);
            final TextView lvrl_meta = convertView.findViewById(R.id.lvrl_meta);
            final ViewGroup lvrl_delta_container = convertView.findViewById(R.id.lvrl_delta_container);
            final TextView lvrl_delta = convertView.findViewById(R.id.lvrl_delta);
            if (is_me) {
                final View lvrl_its_me_1 = convertView.findViewById(R.id.lvrl_its_me_1);
                final View lvrl_its_me_2 = convertView.findViewById(R.id.lvrl_its_me_2);
                int color;
                if (rating > 0 && rating < 4) {
                    switch (rating) {
                        case 1: color = R.attr.colorGold; break;
                        case 2: color = R.attr.colorSilver; break;
                        default: case 3: color = R.attr.colorBronze; break;
                    }
                    color = resolveColor(color);
                    lvrl_number.setVisibility(View.GONE);
                    lvrl_crown.setVisibility(View.VISIBLE);
                    lvrl_crown.setImageTintList(ColorStateList.valueOf(resolveColor(R.attr.colorRatingHighlightText)));
                } else {
                    color = resolveColor(R.attr.colorRatingHighlightBackground);
                    lvrl_crown.setVisibility(View.GONE);
                    lvrl_number.setVisibility(View.VISIBLE);
                    lvrl_number.setText(user.get("number"));
                    lvrl_number.setTextColor(resolveColor(R.attr.colorRatingHighlightText));
                }
                lvrl_its_me_1.setBackgroundColor(color);
                lvrl_its_me_2.setBackgroundColor(color);
                convertView.setBackgroundColor(getColorWithAlpha(color, 60));
            } else {
                if (rating > 0 && rating < 4) {
                    lvrl_crown.setVisibility(View.VISIBLE);
                    lvrl_number.setVisibility(View.GONE);
                    int color;
                    switch (rating) {
                        case 1: color = R.attr.colorGold; break;
                        case 2: color = R.attr.colorSilver; break;
                        default: case 3: color = R.attr.colorBronze; break;
                    }
                    lvrl_crown.setImageTintList(ColorStateList.valueOf(resolveColor(color)));
                } else {
                    lvrl_crown.setVisibility(View.GONE);
                    lvrl_number.setVisibility(View.VISIBLE);
                    lvrl_number.setTextColor(resolveColor(android.R.attr.textColorPrimary));
                    lvrl_number.setText(user.get("number"));
                }
                convertView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            }
            lvrl_fio.setText(user.get("fio"));
            lvrl_meta.setText(user.get("meta"));
            if (lvrl_delta != null) {
                if (!user.get("change").equals("none")) {
                    lvrl_delta_container.setVisibility(View.VISIBLE);
                    lvrl_delta.setText(user.get("delta"));
                    switch (user.get("change")) {
                        case "up": lvrl_delta.setTextColor(resolveColor(R.attr.colorPositiveTrend)); break;
                        case "down": lvrl_delta.setTextColor(resolveColor(R.attr.colorNegativeTrend)); break;
                    }
                } else {
                    lvrl_delta_container.setVisibility(View.GONE);
                }
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return users.get(position).get("is_me").equals("1") ? 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
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
