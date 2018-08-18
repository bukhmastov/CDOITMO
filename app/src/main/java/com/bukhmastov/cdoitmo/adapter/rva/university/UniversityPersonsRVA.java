package com.bukhmastov.cdoitmo.adapter.rva.university;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UniversityPersonsRVA extends UniversityRVA {

    public UniversityPersonsRVA(final Context context) {
        this(context, null);
    }
    public UniversityPersonsRVA(final Context context, final ArrayList<Item> dataset) {
        super(context, dataset);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_update_time, parent, false));
            }
            case TYPE_MAIN: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_persons_list_item, parent, false));
            }
            case TYPE_STATE: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_list_item_state, parent, false));
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
            case TYPE_MAIN: {
                bindMain(holder, item);
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

    private void bindMain(RecyclerView.ViewHolder holder, final Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            final String name = (getStringIfExists(item.data, "title_l") + " " + getStringIfExists(item.data, "title_f") + " " + getStringIfExists(item.data, "title_m")).trim();
            final String degree = item.data.getString("degree").trim();
            final String image = item.data.getString("image");
            View nameView = viewHolder.container.findViewById(R.id.name);
            View postView = viewHolder.container.findViewById(R.id.post);
            View avatarView = viewHolder.container.findViewById(R.id.avatar);
            if (nameView != null) {
                ((TextView) nameView).setText(name);
            }
            if (postView != null) {
                if (!degree.isEmpty()) {
                        ((TextView) postView).setText(degree.substring(0, 1).toUpperCase() + degree.substring(1));
                } else {
                    staticUtil.removeView(viewHolder.container.findViewById(R.id.post));
                }
            }
            if (avatarView != null) {
                Picasso.with(context)
                        .load(image)
                        .error(R.drawable.ic_sentiment_very_satisfied)
                        .transform(new CircularTransformation())
                        .into((ImageView) avatarView);
            }
            viewHolder.container.setOnClickListener(v -> {
                try {
                    Bundle extras = new Bundle();
                    extras.putInt("pid", item.data.getInt("persons_id"));
                    extras.putString("person", item.data.toString());
                    eventBus.fire(new OpenActivityEvent(UniversityPersonCardActivity.class, extras));
                } catch (Exception e) {
                    log.exception(e);
                }
            });
        } catch (Exception e) {
            log.exception(e);
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
            log.exception(e);
        }
    }
    private void bindNoData(RecyclerView.ViewHolder holder, Item item) {
        try {
            UniversityEventsRVA.ViewHolder viewHolder = (UniversityEventsRVA.ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(R.string.no_persons);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private String getStringIfExists(JSONObject jsonObject, String key) throws JSONException {
        return jsonObject.has(key) ? jsonObject.getString(key) : "";
    }
}
