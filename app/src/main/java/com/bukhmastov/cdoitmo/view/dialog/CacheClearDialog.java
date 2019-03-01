package com.bukhmastov.cdoitmo.view.dialog;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class CacheClearDialog extends Dialog {

    private static final String TAG = "CacheClearDialog";
    private static final String TAG_PREFIX = "cache_clear_";
    private final ConnectedActivity activity;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    NotificationMessage notificationMessage;

    private class Entry {
        String title;
        String desc;
        String path;
        @Storage.Type String type;
        long bytes;
        Entry(String title, String path, @Storage.Type String type) {
            this(title, null, path, type);
        }
        Entry(String title, String desc, String path, @Storage.Type String type) {
            this.title = title;
            this.desc = desc;
            this.path = path;
            this.type = type;
            this.bytes = 0L;
        }
    }
    private List<Entry> items = new ArrayList<>();

    public CacheClearDialog(ConnectedActivity activity) {
        super(activity);
        AppComponentProvider.getComponent().inject(this);
        this.activity = activity;
        items.clear();
        items.add(new Entry(activity.getString(R.string.cache_mem), activity.getString(R.string.cache_mem_desc), "_mem_", Storage.USER));
        items.add(new Entry(activity.getString(R.string.cache_all), activity.getString(R.string.cache_all_desc), "_all_", Storage.GLOBAL));
        items.add(new Entry(activity.getString(R.string.e_journal), "eregister", Storage.USER));
        items.add(new Entry(activity.getString(R.string.protocol_changes), "protocol", Storage.USER));
        items.add(new Entry(activity.getString(R.string.rating), "rating", Storage.USER));
        items.add(new Entry(activity.getString(R.string.schedule_lessons), "schedule_lessons", Storage.GLOBAL));
        items.add(new Entry(activity.getString(R.string.schedule_exams), "schedule_exams", Storage.GLOBAL));
        items.add(new Entry(activity.getString(R.string.schedule_attestations), "schedule_attestations", Storage.GLOBAL));
        items.add(new Entry(activity.getString(R.string.room101), "room101", Storage.USER));
        items.add(new Entry(activity.getString(R.string.study_groups), "group", Storage.USER));
        items.add(new Entry(activity.getString(R.string.scholarship), "scholarship", Storage.USER));
        items.add(new Entry(activity.getString(R.string.university), "university", Storage.GLOBAL));
    }

    public void show() {
        log.v(TAG, "show");
        thread.runOnUI(() -> {
            ViewGroup layout = (ViewGroup) inflate(R.layout.dialog_storage_cache);
            if (layout == null) {
                return;
            }
            ViewGroup cacheList = layout.findViewById(R.id.cache_list);
            for (Entry item : items) {
                ViewGroup layoutItem = (ViewGroup) inflate(R.layout.dialog_storage_cache_item);
                if (layoutItem == null) {
                    continue;
                }
                ViewGroup cacheItem = layoutItem.findViewById(R.id.cache_item);
                TextView cacheItemTitle = layoutItem.findViewById(R.id.cache_item_title);
                TextView cacheItemSummary = layoutItem.findViewById(R.id.cache_item_summary);
                ImageView cacheItemType = layoutItem.findViewById(R.id.cache_item_type);
                ViewGroup cacheItemSizeContainer = layoutItem.findViewById(R.id.cache_item_size_container);
                TextView cacheItemSize = layoutItem.findViewById(R.id.cache_item_size);
                layoutItem.setTag(TAG_PREFIX + item.path);
                cacheItemTitle.setText(item.title);
                if (item.desc == null) {
                    cacheItemSummary.setVisibility(View.GONE);
                } else {
                    cacheItemSummary.setText(item.desc);
                }
                switch (item.type) {
                    case Storage.GLOBAL: cacheItemType.setImageResource(R.drawable.ic_group); break;
                    case Storage.USER: cacheItemType.setImageResource(R.drawable.ic_person); break;
                }
                cacheItemSizeContainer.setVisibility(View.INVISIBLE);
                cacheItemSize.setText("...");
                cacheItem.setOnClickListener((v) -> thread.standalone(() -> {
                    if ("_mem_".equals(item.path)) {
                        storage.cacheReset();
                        ConnectedActivity.clearStore();
                        eventBus.fire(new ClearCacheEvent());
                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_cleared));
                        return;
                    }
                    if (item.bytes <= 0L) {
                        return;
                    }
                    if ("_all_".equals(item.path)) {
                        storage.clear(activity, Storage.CACHE, Storage.USER);
                        storage.clear(activity, Storage.CACHE, Storage.GLOBAL);
                        eventBus.fire(new ClearCacheEvent());
                    } else {
                        storage.clear(activity, Storage.CACHE, item.type, item.path);
                        eventBus.fire(new ClearCacheEvent(item.path));
                    }
                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_cleared));
                    calculateCacheSize(cacheList);
                }));
                cacheList.addView(layoutItem);
            }
            // show dialog
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            thread.runOnUI(() -> new AlertDialog.Builder(activity)
                    .setTitle(R.string.cache_clear)
                    .setView(layout)
                    .setNegativeButton(R.string.close, null)
                    .create().show());
            // calculate size
            thread.standalone(() -> {
                calculateCacheSize(cacheList);
            });
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void calculateCacheSize(ViewGroup cacheList) {
        thread.runOnUI(() -> {
            for (Entry item : items) {
                ViewGroup layoutItem = cacheList.findViewWithTag(TAG_PREFIX + item.path);
                if (layoutItem == null) {
                    return;
                }
                ViewGroup cacheItemSizeContainer = layoutItem.findViewById(R.id.cache_item_size_container);
                TextView cacheItemSize = layoutItem.findViewById(R.id.cache_item_size);
                cacheItemSizeContainer.setVisibility(View.VISIBLE);
                cacheItemSize.setText("...");
            }
        });
        for (Entry item : items) {
            if ("_mem_".equals(item.path)) {
                item.bytes = -1L;
                return;
            }
            if ("_all_".equals(item.path)) {
                item.bytes = storage.getDirSize(activity, Storage.CACHE, Storage.USER, "") +
                        storage.getDirSize(activity, Storage.CACHE, Storage.GLOBAL, "");
                return;
            }
            switch (item.type) {
                case Storage.USER: item.bytes = storage.getDirSize(activity, Storage.CACHE, Storage.USER, item.path); break;
                case Storage.GLOBAL: item.bytes = storage.getDirSize(activity, Storage.CACHE, Storage.GLOBAL, item.path); break;
                default: item.bytes = -1L; break;
            }
        }
        thread.runOnUI(() -> {
            for (Entry item : items) {
                ViewGroup layoutItem = cacheList.findViewWithTag(TAG_PREFIX + item.path);
                if (layoutItem == null) {
                    return;
                }
                ViewGroup cacheItemSizeContainer = layoutItem.findViewById(R.id.cache_item_size_container);
                TextView cacheItemSize = layoutItem.findViewById(R.id.cache_item_size);
                if (item.bytes < 0L) {
                    cacheItemSizeContainer.setVisibility(View.INVISIBLE);
                } else if (item.bytes == 0L) {
                    cacheItemSize.setText(R.string.empty);
                    cacheItemSizeContainer.setVisibility(View.VISIBLE);
                } else {
                    cacheItemSize.setText(StringUtils.bytes2readable(activity, storagePref, item.bytes));
                    cacheItemSizeContainer.setVisibility(View.VISIBLE);
                }
                cacheItemSizeContainer.invalidate();
            }
        });
    }
}
