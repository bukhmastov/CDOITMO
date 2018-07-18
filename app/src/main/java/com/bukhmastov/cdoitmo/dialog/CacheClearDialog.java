package com.bukhmastov.cdoitmo.dialog;

import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.ArrayList;
import java.util.List;

public class CacheClearDialog extends Dialog {

    private static final String TAG = "CacheClearDialog";
    private static final String TAG_PREFIX = "cache_clear_";
    private final ConnectedActivity activity;

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();
    //@Inject
    private TextUtils textUtils = TextUtils.instance();
    //@Inject
    private NotificationMessage notificationMessage = NotificationMessage.instance();

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
        items.add(new Entry(activity.getString(R.string.university), "university", Storage.GLOBAL));
    }

    public void show() {
        log.v(TAG, "show");
        thread.runOnUI(() -> {
            final ViewGroup layout = (ViewGroup) inflate(R.layout.dialog_storage_cache);
            if (layout == null) {
                return;
            }
            thread.run(() -> {
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
                            case Storage.GLOBAL: cache_item_type.setImageResource(R.drawable.ic_group); break;
                            case Storage.USER: cache_item_type.setImageResource(R.drawable.ic_person); break;
                        }
                        cache_item_size_container.setVisibility(View.INVISIBLE);
                        cache_item_size.setText("...");
                        cache_item.setOnClickListener((v) -> thread.run(thread.BACKGROUND, () -> {
                            if ("_mem_".equals(item.path)) {
                                storage.cacheReset();
                                ConnectedActivity.clearStore();
                                notificationMessage.snackBar(activity, activity.getString(R.string.cache_cleared));
                                return;
                            } else {
                                if (item.bytes <= 0L) {
                                    return;
                                }
                                if ("_all_".equals(item.path)) {
                                    storage.clear(activity, Storage.CACHE, Storage.USER);
                                    storage.clear(activity, Storage.CACHE, Storage.GLOBAL);
                                } else {
                                    switch (item.type) {
                                        case Storage.USER: storage.clear(activity, Storage.CACHE, Storage.USER, item.path); break;
                                        case Storage.GLOBAL: storage.clear(activity, Storage.CACHE, Storage.GLOBAL, item.path); break;
                                    }
                                }
                            }
                            notificationMessage.snackBar(activity, activity.getString(R.string.cache_cleared));
                            calculateCacheSize(cache_list);
                        }));
                        cache_list.addView(layout_item);
                    }
                    // show dialog
                    thread.runOnUI(() -> new AlertDialog.Builder(activity)
                            .setTitle(R.string.cache_clear)
                            .setView(layout)
                            .setNegativeButton(R.string.close, null)
                            .create().show());
                    // calculate size
                    calculateCacheSize(cache_list);
                } catch (Exception e) {
                    log.exception(e);
                }
            });
        });
    }

    private void calculateCacheSize(final ViewGroup cache_list) {
        thread.run(thread.BACKGROUND, () -> {
            for (final Entry item : items) {
                thread.runOnUI(() -> {
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
                    size =  storage.getDirSize(activity, Storage.CACHE, Storage.USER, "") +
                            storage.getDirSize(activity, Storage.CACHE, Storage.GLOBAL, "");
                } else {
                    switch (item.type) {
                        case Storage.USER: size = storage.getDirSize(activity, Storage.CACHE, Storage.USER, item.path); break;
                        case Storage.GLOBAL: size = storage.getDirSize(activity, Storage.CACHE, Storage.GLOBAL, item.path); break;
                        default: size = -1L; break;
                    }
                }
                thread.runOnUI(() -> {
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
                        cache_item_size.setText(textUtils.bytes2readable(activity, storagePref, size));
                        cache_item_size_container.setVisibility(View.VISIBLE);
                    }
                    cache_item_size_container.invalidate();
                });
            }
        });
    }
}
