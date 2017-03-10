package com.bukhmastov.cdoitmo.builders;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class Room101ReviewBuilder extends Thread {

    public interface response {
        void state(int state, View layout);
    }
    public interface register {
        void onDenyRequest(int reid, int status);
    }
    private response delegate = null;
    private register register = null;
    private Activity activity;
    private JSONArray sessions;
    private float destiny;

    public static final int STATE_FAILED = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_DONE = 2;

    public Room101ReviewBuilder(Activity activity, register register, JSONArray sessions, response delegate){
        this.activity = activity;
        this.register = register;
        this.delegate = delegate;
        this.sessions = sessions;
        this.destiny = activity.getResources().getDisplayMetrics().density;
    }
    public void run(){
        try {
            delegate.state(STATE_LOADING, inflate(R.layout.state_loading_compact));
            LinearLayout container = (LinearLayout) inflate(R.layout.layout_room101_review_requests);
            if (sessions.length() > 0) {
                LinearLayout review_requests_container = (LinearLayout) container.findViewById(R.id.review_requests_container);
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
                        requestLayout.findViewById(R.id.request_deny_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int status = 0;
                                if (Objects.equals(statusText, "удовлетворена")) status = 1;
                                register.onDenyRequest(reid, status);
                            }
                        });
                    } else {
                        ((ViewGroup) requestLayout.findViewById(R.id.request_deny)).removeView(requestLayout.findViewById(R.id.request_deny_button));
                    }
                    review_requests_container.addView(requestLayout);
                    if (i > 0) {
                        View separator = new View(activity);
                        separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                        separator.setBackgroundColor(Static.colorSeparator);
                        review_requests_container.addView(separator);
                    }
                }
            } else {
                container.addView(inflate(R.layout.layout_room101_review_without_requests));
            }
            delegate.state(STATE_DONE, container);
        } catch (Exception e){
            Static.error(e);
            delegate.state(STATE_FAILED, null);
        }
    }

    private View inflate(int layout) throws Exception {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }

}