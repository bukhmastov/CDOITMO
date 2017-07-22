package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.WebViewActivity;
import com.bukhmastov.cdoitmo.utils.Static;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

public class NewsRecyclerViewAdapter extends UniversityRecyclerViewAdapter {

    public NewsRecyclerViewAdapter(final Context context) {
        super(context, null);
    }
    public NewsRecyclerViewAdapter(final Context context, final ArrayList<Item> dataset) {
        super(context, dataset);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_update_time, parent, false));
            }
            case TYPE_MAIN: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_news_card, parent, false));
            }
            case TYPE_MINOR: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_news_card_compact, parent, false));
            }
            case TYPE_STATE: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_list_item_state, parent, false));
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = dataset.get(position);
        switch (item.type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: {
                bindInfoAboutUpdateTime(holder, item);
                break;
            }
            case TYPE_MAIN: {
                bindMain(holder, item);
                break;
            }
            case TYPE_MINOR: {
                bindMinor(holder, item);
                break;
            }
            case TYPE_STATE: {
                bindState(holder, item);
                break;
            }
        }
    }

    private void bindMain(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String title = getString(item.data, "news_title");
            String img = getString(item.data, "img");
            String img_small = getString(item.data, "img_small");
            String anons = getString(item.data, "anons");
            String category_parent = getString(item.data, "category_parent");
            String category_child = getString(item.data, "category_child");
            String color_hex = getString(item.data, "color_hex");
            String date = getString(item.data, "pub_date");
            final String webview = getString(item.data, "url_webview");
            int count_view = getInt(item.data, "count_view");
            if (title == null || title.trim().isEmpty()) {
                // skip news with empty title
                return;
            }
            if ((img == null || img.trim().isEmpty()) && (img_small != null && !img_small.trim().isEmpty())) {
                img = img_small;
            }
            View titleView = viewHolder.container.findViewById(R.id.title);
            View categoriesView = viewHolder.container.findViewById(R.id.categories);
            View anonsView = viewHolder.container.findViewById(R.id.anons);
            View dateView = viewHolder.container.findViewById(R.id.date);
            View countViewContainerView = viewHolder.container.findViewById(R.id.count_view_container);
            View countView = viewHolder.container.findViewById(R.id.count_view);
            View infoContainerView = viewHolder.container.findViewById(R.id.info_container);
            final View news_image_container = viewHolder.container.findViewById(R.id.news_image_container);
            if (news_image_container != null) {
                if (img != null && !img.trim().isEmpty()) {
                    View news_image = viewHolder.container.findViewById(R.id.news_image);
                    if (news_image != null) {
                        Picasso.with(context)
                                .load(img)
                                .into((ImageView) viewHolder.container.findViewById(R.id.news_image), new Callback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError() {
                                        Static.removeView(news_image_container);
                                    }
                                });
                    }
                } else {
                    Static.removeView(news_image_container);
                }
            }
            if (titleView != null) {
                ((TextView) titleView).setText(Static.escapeString(title));
            }
            if (categoriesView != null) {
                boolean category_parent_exists = category_parent != null && !category_parent.trim().isEmpty();
                boolean category_child_exists = category_child != null && !category_child.trim().isEmpty();
                if (category_parent_exists || category_child_exists) {
                    if (Objects.equals(category_parent, category_child)) {
                        category_child_exists = false;
                    }
                    String category = "";
                    if (category_parent_exists) {
                        category += category_parent;
                        if (category_child_exists) {
                            category += " ► ";
                        }
                    }
                    if (category_child_exists) {
                        category += category_child;
                    }
                    if (!category.isEmpty()) {
                        category = "● " + category;
                        TextView categories = (TextView) categoriesView;
                        categories.setText(category);
                        if (color_hex != null && !color_hex.trim().isEmpty()) {
                            categories.setTextColor(Color.parseColor(color_hex));
                        }
                    } else {
                        Static.removeView(categoriesView);
                    }
                } else {
                    Static.removeView(categoriesView);
                }
            }
            if (anonsView != null) {
                if (anons != null && !anons.trim().isEmpty()) {
                    ((TextView) anonsView).setText(Static.escapeString(anons));
                } else {
                    Static.removeView(anonsView);
                }
            }
            boolean date_exists = date != null && !date.trim().isEmpty();
            boolean count_exists = count_view >= 0;
            if (date_exists || count_exists) {
                if (dateView != null) {
                    if (date_exists) {
                        ((TextView) dateView).setText(Static.cuteDate(context, "yyyy-MM-dd HH:mm:ss", date));
                    } else {
                        Static.removeView(dateView);
                    }
                }
                if (count_exists) {
                    if (countView != null) {
                        ((TextView) countView).setText(String.valueOf(count_view));
                    }
                } else {
                    if (countViewContainerView != null) {
                        Static.removeView(countViewContainerView);
                    }
                }
            } else {
                if (infoContainerView != null) {
                    Static.removeView(infoContainerView);
                }
            }
            if (webview != null && !webview.trim().isEmpty()) {
                viewHolder.container.findViewById(R.id.news_click).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, WebViewActivity.class);
                        Bundle extras = new Bundle();
                        extras.putString("url", webview.trim());
                        extras.putString("title", context.getString(R.string.news));
                        intent.putExtras(extras);
                        context.startActivity(intent);
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindMinor(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String title = getString(item.data, "news_title");
            String img = getString(item.data, "img");
            String img_small = getString(item.data, "img_small");
            String category_parent = getString(item.data, "category_parent");
            String category_child = getString(item.data, "category_child");
            String color_hex = getString(item.data, "color_hex");
            String date = getString(item.data, "pub_date");
            final String webview = getString(item.data, "url_webview");
            int count_view = getInt(item.data, "count_view");
            if (title == null || title.trim().isEmpty()) {
                // skip news with empty title
                return;
            }
            View titleView = viewHolder.container.findViewById(R.id.title);
            View categoriesView = viewHolder.container.findViewById(R.id.categories);
            View dateView = viewHolder.container.findViewById(R.id.date);
            View countViewContainerView = viewHolder.container.findViewById(R.id.count_view_container);
            View countView = viewHolder.container.findViewById(R.id.count_view);
            View infoContainerView = viewHolder.container.findViewById(R.id.info_container);
            if (img_small != null && !img_small.trim().isEmpty()) {
                img = img_small;
            }
            final View news_image_container = viewHolder.container.findViewById(R.id.news_image_container);
            if (news_image_container != null) {
                if (img != null && !img.trim().isEmpty()) {
                    View news_image = viewHolder.container.findViewById(R.id.news_image);
                    if (news_image != null) {
                        Picasso.with(context)
                                .load(img)
                                .into((ImageView) viewHolder.container.findViewById(R.id.news_image), new Callback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError() {
                                        Static.removeView(news_image_container);
                                    }
                                });
                    }
                } else {
                    Static.removeView(news_image_container);
                }
            }
            if (titleView != null) {
                ((TextView) titleView).setText(Static.escapeString(title));
            }
            if (categoriesView != null) {
                boolean category_parent_exists = category_parent != null && !category_parent.trim().isEmpty();
                boolean category_child_exists = category_child != null && !category_child.trim().isEmpty();
                if (category_parent_exists || category_child_exists) {
                    if (Objects.equals(category_parent, category_child)) {
                        category_child_exists = false;
                    }
                    String category = "";
                    if (category_parent_exists) {
                        category += category_parent;
                        if (category_child_exists) {
                            category += " ► ";
                        }
                    }
                    if (category_child_exists) {
                        category += category_child;
                    }
                    if (!category.isEmpty()) {
                        category = "● " + category;
                        TextView categories = (TextView) categoriesView;
                        categories.setText(category);
                        if (color_hex != null && !color_hex.trim().isEmpty()) {
                            categories.setTextColor(Color.parseColor(color_hex));
                        }
                    } else {
                        Static.removeView(categoriesView);
                    }
                } else {
                    Static.removeView(categoriesView);
                }
            }
            boolean date_exists = date != null && !date.trim().isEmpty();
            boolean count_exists = count_view >= 0;
            if (date_exists || count_exists) {
                if (dateView != null) {
                    if (date_exists) {
                        ((TextView) dateView).setText(Static.cuteDate(context, "yyyy-MM-dd HH:mm:ss", date));
                    } else {
                        Static.removeView(dateView);
                    }
                }
                if (count_exists) {
                    if (countView != null) {
                        ((TextView) countView).setText(String.valueOf(count_view));
                    }
                } else {
                    if (countViewContainerView != null) {
                        Static.removeView(countViewContainerView);
                    }
                }
            } else {
                if (infoContainerView != null) {
                    Static.removeView(infoContainerView);
                }
            }
            if (webview != null && !webview.trim().isEmpty()) {
                viewHolder.container.findViewById(R.id.news_click).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, WebViewActivity.class);
                        Bundle extras = new Bundle();
                        extras.putString("url", webview.trim());
                        extras.putString("title", context.getString(R.string.news));
                        intent.putExtras(extras);
                        context.startActivity(intent);
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindState(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            for (int i = viewHolder.container.getChildCount() - 1; i >= 0; i--) {
                View child = viewHolder.container.getChildAt(i);
                if (child.getId() == item.data_state_keep) {
                    child.setVisibility(View.VISIBLE);
                } else {
                    child.setVisibility(View.GONE);
                }
            }
            View.OnClickListener onStateClickListener = onStateClickListeners.containsKey(item.data_state_keep) ? onStateClickListeners.get(item.data_state_keep) : null;
            if (onStateClickListener != null) {
                viewHolder.container.setOnClickListener(onStateClickListener);
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
}
