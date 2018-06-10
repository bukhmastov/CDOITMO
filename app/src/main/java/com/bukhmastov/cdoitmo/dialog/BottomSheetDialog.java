package com.bukhmastov.cdoitmo.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Log;

public class BottomSheetDialog extends android.support.design.widget.BottomSheetDialog implements View.OnClickListener {

    private static final String TAG = "BottomSheetDialog";
    private final Context context;
    private OnEntryClickListener listener;

    public BottomSheetDialog(@NonNull Context context, @Nullable String header, @NonNull Entry ...entries) {
        super(context);
        this.context = context;
        init(header, entries);
    }

    public BottomSheetDialog(@NonNull Context context, int theme, @Nullable String header, @NonNull Entry ...entries) {
        super(context, theme);
        this.context = context;
        init(header, entries);
    }

    public BottomSheetDialog setListener(@NonNull OnEntryClickListener listener) {
        this.listener = listener;
        return this;
    }

    private void init(@Nullable String header, @NonNull Entry ...entries) {
        ViewGroup layout = (ViewGroup) inflate(R.layout.dialog_bottom_sheet);
        if (layout == null) {
            Log.w(TAG, "init | layout = null");
            return;
        }

        View dialog_header = layout.findViewById(R.id.dialog_header);
        if (header != null) {
            ((TextView) dialog_header).setText(header);
            dialog_header.setVisibility(View.VISIBLE);
        } else {
            dialog_header.setVisibility(View.GONE);
        }

        ViewGroup dialog_entries = layout.findViewById(R.id.dialog_entries);
        for (Entry entry : entries) {
            if (entry == null || entry.title == null) {
                continue;
            }
            ViewGroup layout_entry = (ViewGroup) inflate(R.layout.dialog_bottom_sheet_item);
            TextView text = layout_entry.findViewById(R.id.text);
            text.setText(entry.title);
            text.setTag(entry.tag);
            text.setOnClickListener(this);
            dialog_entries.addView(layout_entry);
        }

        setContentView(layout);
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        if (tag == null) {
            return;
        }
        if (listener != null) {
            listener.onClick(tag);
        }
        dismiss();
    }

    public static class Entry {
        public String title;
        public String tag;
        public Entry(String title, String tag) {
            this.title = title;
            this.tag = tag;
        }
    }

    public interface OnEntryClickListener {
        void onClick(String tag);
    }

    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
