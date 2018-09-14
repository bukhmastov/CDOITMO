package com.bukhmastov.cdoitmo.adapter.rva.university;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.WebViewActivity;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.util.singleton.JsonUtils;

import java.util.ArrayList;

public class UniversityEventsRVA extends UniversityRVA {

    public UniversityEventsRVA(final Context context) {
        this(context, null);
    }
    public UniversityEventsRVA(final Context context, final ArrayList<Item> dataset) {
        super(context, dataset);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_update_time, parent, false));
            }
            case TYPE_MINOR: {
                return new UniversityEventsRVA.ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_news_card_compact, parent, false));
            }
            case TYPE_STATE: {
                return new UniversityEventsRVA.ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_list_item_state, parent, false));
            }
            default:
            case TYPE_NO_DATA: {
                return new UniversityEventsRVA.ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.state_nothing_to_display_compact, parent, false));
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = dataset.get(position);
        switch (item.type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: {
                bindInfoAboutUpdateTime(holder, item);
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
            case TYPE_NO_DATA: {
                bindNoData(holder, item);
                break;
            }
        }
    }

    private void bindMinor(RecyclerView.ViewHolder holder, Item item) {
        try {
            UniversityEventsRVA.ViewHolder viewHolder = (UniversityEventsRVA.ViewHolder) holder;
            String title = JsonUtils.getString(item.data, "name");
            String type = JsonUtils.getString(item.data, "type_name");
            String color_hex = "#DF1843";
            String date_begin = JsonUtils.getString(item.data, "date_begin");
            String date_end = JsonUtils.getString(item.data, "date_end");
            final String webview = JsonUtils.getString(item.data, "url_webview");
            if (title == null || title.trim().isEmpty()) {
                // skip event with empty title
                return;
            }
            View titleView = viewHolder.container.findViewById(R.id.title);
            View categoriesView = viewHolder.container.findViewById(R.id.categories);
            View dateView = viewHolder.container.findViewById(R.id.date);
            View newsImageContainerView = viewHolder.container.findViewById(R.id.news_image_container);
            View countViewContainerView = viewHolder.container.findViewById(R.id.count_view_container);
            if (titleView != null) {
                ((TextView) titleView).setText(textUtils.escapeString(title));
            }
            if (categoriesView != null) {
                if (type != null && !type.trim().isEmpty()) {
                    TextView categories = (TextView) categoriesView;
                    categories.setText("● " + type);
                    categories.setTextColor(Color.parseColor(color_hex));
                } else {
                    staticUtil.removeView(categoriesView);
                }
            }
            if (dateView != null) {
                boolean date_begin_exists = date_begin != null && !date_begin.trim().isEmpty();
                boolean date_end_exists = date_end != null && !date_end.trim().isEmpty();
                if (date_begin_exists || date_end_exists) {
                    String date = null;
                    if (date_begin_exists && date_end_exists) {
                        date = textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", date_begin, date_end);
                    } else if (date_begin_exists) {
                        date = textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", date_begin);
                    } else if (date_end_exists) {
                        date = textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", date_end);
                    }
                    ((TextView) dateView).setText(date);
                } else {
                    staticUtil.removeView(dateView);
                }
            }
            if (newsImageContainerView != null) {
                staticUtil.removeView(newsImageContainerView);
            }
            if (countViewContainerView != null) {
                staticUtil.removeView(countViewContainerView);
            }
            if (webview != null && !webview.trim().isEmpty()) {
                viewHolder.container.findViewById(R.id.news_click).setOnClickListener(v -> {
                    Bundle extras = new Bundle();
                    extras.putString("url", webview.trim());
                    extras.putString("title", context.getString(R.string.events));
                    eventBus.fire(new OpenActivityEvent(WebViewActivity.class, extras));
                });
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindState(RecyclerView.ViewHolder holder, Item item) {
        try {
            UniversityEventsRVA.ViewHolder viewHolder = (UniversityEventsRVA.ViewHolder) holder;
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
            log.exception(e);
        }
    }
    private void bindNoData(RecyclerView.ViewHolder holder, Item item) {
        try {
            UniversityEventsRVA.ViewHolder viewHolder = (UniversityEventsRVA.ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(R.string.no_events);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
