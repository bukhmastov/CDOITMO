package com.bukhmastov.cdoitmo.builder;

import android.app.Activity;
import android.content.Context;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class Room101ReviewBuilder implements Runnable {

    private static final String TAG = "Room101ReviewBuilder";

    public interface response {
        void state(int state, View layout);
    }
    public interface register {
        void onDenyRequest(int reid, int status);
    }
    private response delegate = null;
    private register register = null;
    private final Activity activity;
    private final JSONArray sessions;
    private final float destiny;

    public static final int STATE_FAILED = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_DONE = 2;

    //@Inject
    private Log log = Log.instance();

    public Room101ReviewBuilder(Activity activity, register register, JSONArray sessions, response delegate) {
        this.activity = activity;
        this.register = register;
        this.delegate = delegate;
        this.sessions = sessions;
        this.destiny = activity.getResources().getDisplayMetrics().density;
    }
    public void run() {
        try {
            log.v(TAG, "started");
            delegate.state(STATE_LOADING, inflate(R.layout.state_loading_compact));
            if (sessions.length() > 0) {
                LinearLayout container = (LinearLayout) inflate(R.layout.layout_room101_review_requests);
                log.v(TAG, "sessions.length() == " + sessions.length());
                LinearLayout review_requests_container = container.findViewById(R.id.review_requests_container);
                for (int i = sessions.length() - 1; i >= 0; i--) {
                    JSONObject request = sessions.getJSONObject(i);
                    RelativeLayout requestLayout = (RelativeLayout) inflate(R.layout.layout_room101_review_requests_item);
                    ((TextView) requestLayout.findViewById(R.id.request_time_start)).setText(request.getString("timeStart"));
                    ((TextView) requestLayout.findViewById(R.id.request_time_end)).setText(request.getString("timeEnd"));
                    ((TextView) requestLayout.findViewById(R.id.request_title)).setText(request.getString("date"));
                    ((TextView) requestLayout.findViewById(R.id.request_desc)).setText(request.getString("status"));
                    final int reid = request.getInt("reid");
                    final String statusText = request.getString("status");
                    if (reid != 0) {
                        requestLayout.findViewById(R.id.request_deny_button).setOnClickListener(v -> {
                            log.v(TAG, "request_deny_button clicked");
                            register.onDenyRequest(reid, "удовлетворена".equals(statusText.toLowerCase()) ? 1 : 0);
                        });
                    } else {
                        ((ViewGroup) requestLayout.findViewById(R.id.request_deny)).removeView(requestLayout.findViewById(R.id.request_deny_button));
                    }
                    review_requests_container.addView(requestLayout);
                    if (i > 0) {
                        View separator = new View(activity);
                        separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                        separator.setBackgroundColor(Color.resolve(activity, R.attr.colorSeparator));
                        review_requests_container.addView(separator);
                    }
                }
                delegate.state(STATE_DONE, container);
            } else {
                log.v(TAG, "sessions.length() == 0");
                View view = inflate(R.layout.state_nothing_to_display_compact);
                ((TextView) view.findViewById(R.id.ntd_text)).setText(activity.getString(R.string.no_requests));
                delegate.state(STATE_DONE, view);
            }
        } catch (Exception e){
            log.exception(e);
            delegate.state(STATE_FAILED, null);
        }
        log.v(TAG, "finished");
    }

    private View inflate(int layout) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
