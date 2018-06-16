package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AlertDialog;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;

import java.util.ArrayList;
import java.util.List;

public class CacheClearUtil {

    private static final String TAG = "CacheClearUtil";
    private static final String TAG_PREFIX = "cache_clear_";
    private final ConnectedActivity activity;

    private class Entry {
        String title;
        String desc;
        String path;
        @Storage.TYPE String type;
        long bytes;
        Entry(String title, String path, @Storage.TYPE String type) {
            this(title, null, path, type);
        }
        Entry(String title, String desc, String path, @Storage.TYPE String type) {
            this.title = title;
            this.desc = desc;
            this.path = path;
            this.type = type;
            this.bytes = 0L;
        }
    }
    private List<Entry> items = new ArrayList<>();

    public CacheClearUtil(ConnectedActivity activity) {
        this.activity = activity;
        items.clear();
        items.add(new Entry(activity.getString(R.string.cache_mem), activity.getString(R.string.cache_mem_desc), "_mem_", Storage.USER));
        items.add(new Entry(activity.getString(R.string.cache_all), activity.getString(R.string.cache_all_desc), "_all_", Storage.GENERAL));
        items.add(new Entry(activity.getString(R.string.e_journal), "eregister", Storage.USER));
        items.add(new Entry(activity.getString(R.string.protocol_changes), "protocol", Storage.USER));
        items.add(new Entry(activity.getString(R.string.rating), "rating", Storage.USER));
        items.add(new Entry(activity.getString(R.string.schedule_lessons), "schedule_lessons", Storage.GENERAL));
        items.add(new Entry(activity.getString(R.string.schedule_exams), "schedule_exams", Storage.GENERAL));
        items.add(new Entry(activity.getString(R.string.schedule_attestations), "schedule_attestations", Storage.GENERAL));
        items.add(new Entry(activity.getString(R.string.room101), "room101", Storage.USER));
        items.add(new Entry(activity.getString(R.string.university), "university", Storage.GENERAL));
    }

    public void show() {
        Log.v(TAG, "show");
        Static.T.runOnUiThread(() -> {
            final ViewGroup layout = (ViewGroup) inflate(R.layout.dialog_storage_cache);
            if (layout == null) {
                return;
            }
            Static.T.runThread(() -> {
                try {
                    final ViewGroup cache_list = layout.findViewById(R.id.cache_list);
                    for (Entry item : items) {
                        final ViewGroup layout_item = (ViewGroup) inflate(R.layout.dialog_storage_cache_item);
                        if (layout_item == null) {
                            continue;
                        }
                        final ViewGroup cache_item = layout_item.findViewById(R.id.cache_item);
                        final TextView cache_item_title = layout_item.findViewById(R.id.cache_item_title);
                        final TextView cache_item_summary = layout_item.findViewById(R.id.cache_item_summary);
                        final ImageView cache_item_type = layout_item.findViewById(R.id.cache_item_type);
                        final ViewGroup cache_item_size_container = layout_item.findViewById(R.id.cache_item_size_container);
                        final TextView cache_item_size = layout_item.findViewById(R.id.cache_item_size);
                        layout_item.setTag(TAG_PREFIX + item.path);
                        cache_item_title.setText(item.title);
                        if (item.desc == null) {
                            cache_item_summary.setVisibility(View.GONE);
                        } else {
                            cache_item_summary.setText(item.desc);
                        }
                        switch (item.type) {
                            case Storage.GENERAL: cache_item_type.setImageResource(R.drawable.ic_group); break;
                            case Storage.USER: cache_item_type.setImageResource(R.drawable.ic_person); break;
                        }
                        cache_item_size_container.setVisibility(View.INVISIBLE);
                        cache_item_size.setText("...");
                        cache_item.setOnClickListener((v) -> Static.T.runThread(Static.T.BACKGROUND, () -> {
                            if ("_mem_".equals(item.path)) {
                                Storage.cache.reset();
                                ConnectedActivity.clearStore();
                                Static.snackBar(activity, activity.getString(R.string.cache_cleared));
                                return;
                            } else {
                                if (item.bytes <= 0L) {
                                    return;
                                }
                                if ("_all_".equals(item.path)) {
                                    Storage.file.cache.clear(activity);
                                    Storage.file.general.cache.clear(activity);
                                } else {
                                    switch (item.type) {
                                        case Storage.USER: Storage.file.cache.clear(activity, item.path); break;
                                        case Storage.GENERAL: Storage.file.general.cache.clear(activity, item.path); break;
                                    }
                                }
                            }
                            Static.snackBar(activity, activity.getString(R.string.cache_cleared));
                            calculateCacheSize(cache_list);
                        }));
                        cache_list.addView(layout_item);
                    }
                    // show dialog
                    Static.T.runOnUiThread(() -> new AlertDialog.Builder(activity)
                            .setTitle(R.string.cache_clear)
                            .setView(layout)
                            .setNegativeButton(R.string.close, null)
                            .create().show());
                    // calculate size
                    calculateCacheSize(cache_list);
                } catch (Exception e) {
                    Static.error(e);
                }
            });
        });
    }
    private void calculateCacheSize(final ViewGroup cache_list) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            for (final Entry item : items) {
                Static.T.runOnUiThread(() -> {
                    final ViewGroup layout_item = cache_list.findViewWithTag(TAG_PREFIX + item.path);
                    if (layout_item == null) {
                        return;
                    }
                    final ViewGroup cache_item_size_container = layout_item.findViewById(R.id.cache_item_size_container);
                    final TextView cache_item_size = layout_item.findViewById(R.id.cache_item_size);
                    cache_item_size_container.setVisibility(View.VISIBLE);
                    cache_item_size.setText("...");
                });
            }
            for (final Entry item : items) {
                final Long size;
                if ("_mem_".equals(item.path)) {
                    size = -1L;
                } else if ("_all_".equals(item.path)) {
                    size = Storage.file.cache.getDirSize(activity, "") + Storage.file.general.cache.getDirSize(activity, "");
                } else {
                    switch (item.type) {
                        case Storage.USER: size = Storage.file.cache.getDirSize(activity, item.path); break;
                        case Storage.GENERAL: size = Storage.file.general.cache.getDirSize(activity, item.path); break;
                        default: size = -1L; break;
                    }
                }
                Static.T.runOnUiThread(() -> {
                    final ViewGroup layout_item = cache_list.findViewWithTag(TAG_PREFIX + item.path);
                    if (layout_item == null) {
                        return;
                    }
                    final ViewGroup cache_item_size_container = layout_item.findViewById(R.id.cache_item_size_container);
                    final TextView cache_item_size = layout_item.findViewById(R.id.cache_item_size);
                    item.bytes = size;
                    if (size < 0L) {
                        cache_item_size_container.setVisibility(View.INVISIBLE);
                    } else if (size == 0L) {
                        cache_item_size.setText(R.string.empty);
                        cache_item_size_container.setVisibility(View.VISIBLE);
                    } else {
                        cache_item_size.setText(Static.bytes2readable(activity, size));
                        cache_item_size_container.setVisibility(View.VISIBLE);
                    }
                    cache_item_size_container.invalidate();
                });
            }
        });
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            Log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
