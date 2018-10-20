package com.bukhmastov.cdoitmo.builder;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;
import com.bukhmastov.cdoitmo.model.room101.requests.RSession;
import com.bukhmastov.cdoitmo.model.room101.requests.Room101Requests;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import javax.inject.Inject;

public class Room101ReviewBuilder implements ThrowingRunnable {

    private static final String TAG = "Room101ReviewBuilder";

    public interface Response {
        void state(int state, View layout);
    }
    public interface Request {
        void onDenyRequest(int reid, int status);
    }
    private final Context context;
    private final Response delegate;
    private final Request request;
    private final Room101Requests requests;
    private final float destiny;

    public static final int STATE_LOADING = 0;
    public static final int STATE_DONE = 1;

    @Inject
    Log log;

    public Room101ReviewBuilder(Context context, Request request, Room101Requests requests, Response delegate) {
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        this.request = request;
        this.delegate = delegate;
        this.requests = requests;
        this.destiny = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public void run() {
        delegate.state(STATE_LOADING, inflate(R.layout.state_loading_compact));
        if (requests == null || CollectionUtils.isEmpty(requests.getSessions())) {
            log.v(TAG, "sessions.length() == 0");
            View view = inflate(R.layout.state_nothing_to_display_compact);
            if (view != null) {
                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_requests);
            }
            delegate.state(STATE_DONE, view);
            return;
        }
        LinearLayout container = (LinearLayout) inflate(R.layout.layout_room101_review_requests);
        if (container == null) {
            delegate.state(STATE_DONE, null);
            return;
        }
        log.v(TAG, "sessions.length() == " + requests.getSessions().size());
        LinearLayout requestsContainer = container.findViewById(R.id.review_requests_container);
        for (int i = requests.getSessions().size() - 1; i >= 0; i--) {
            RSession request = requests.getSessions().get(i);
            RelativeLayout requestLayout = (RelativeLayout) inflate(R.layout.layout_room101_review_requests_item);
            if (requestLayout == null) {
                delegate.state(STATE_DONE, null);
                return;
            }
            ((TextView) requestLayout.findViewById(R.id.request_time_start)).setText(request.getTimeStart());
            ((TextView) requestLayout.findViewById(R.id.request_time_end)).setText(request.getTimeEnd());
            ((TextView) requestLayout.findViewById(R.id.request_title)).setText(request.getDate());
            ((TextView) requestLayout.findViewById(R.id.request_desc)).setText(request.getStatus());
            if (request.getReid() != 0 && request.getStatus() != null) {
                requestLayout.findViewById(R.id.request_deny_button).setOnClickListener(v -> {
                    log.v(TAG, "request_deny_button clicked");
                    this.request.onDenyRequest(request.getReid(), "удовлетворена".equals(request.getStatus().toLowerCase()) ? 1 : 0);
                });
            } else {
                ((ViewGroup) requestLayout.findViewById(R.id.request_deny)).removeView(requestLayout.findViewById(R.id.request_deny_button));
            }
            requestsContainer.addView(requestLayout);
            if (i > 0) {
                View separator = new View(context);
                separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                separator.setBackgroundColor(Color.resolve(context, R.attr.colorSeparator));
                requestsContainer.addView(separator);
            }
        }
        delegate.state(STATE_DONE, container);
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        if (context == null) {
            log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
