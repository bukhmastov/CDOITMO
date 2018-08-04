package com.bukhmastov.cdoitmo.view;

import android.content.Context;
import androidx.annotation.LayoutRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;

public class Message {

    private static final String TAG = "Message";
    public interface RemoteMessageCallback {
        void dismiss(Context context, View view);
    }

    public static View getRemoteMessage(final Context context, final int type, final String message, final RemoteMessageCallback callback) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return null;
            }
            int layoutId;
            switch (type) {
                case 0:
                default: {
                    layoutId = R.layout.message_remote_info;
                    break;
                }
                case 1: {
                    layoutId = R.layout.message_remote_warn;
                    break;
                }
            }
            final View layout = inflate(context, layoutId);
            if (layout == null) {
                return null;
            }
            ((TextView) layout.findViewById(R.id.text)).setText(message);
            layout.setOnClickListener(v -> callback.dismiss(context, layout));
            return layout;
        } catch (Exception e) {
            return null;
        }
    }

    private static View inflate(Context context, @LayoutRes int layout) throws InflateException {
        if (context == null) {
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
