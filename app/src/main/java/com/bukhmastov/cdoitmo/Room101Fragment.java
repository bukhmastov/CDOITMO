package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class Room101Fragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.register {

    private static final String TAG = "Room101Fragment";
    private boolean interrupted = false;
    private boolean loaded = false;
    static RequestHandle fragmentRequestHandle = null;
    private JSONObject viewRequest = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Room101RestClient.init();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room101, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        relaunch();
    }

    @Override
    public void onPause() {
        super.onPause();
        interrupt();
    }

    @Override
    public void onRefresh() {
        load(true);
    }

    @Override
    public void onDenyRequest(final int reid, final int status) {
        if(MainActivity.OFFLINE_MODE) {
            snackBar(getString(R.string.device_offline_action_refused));
        } else {
            (new AlertDialog.Builder(getContext())
                    .setTitle(R.string.request_deny)
                    .setMessage(getString(R.string.request_deny_1) + "\n" + getString(R.string.request_deny_2))
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            denyRequest(reid, status);
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
            ).show();
        }
    }
    private void denyRequest(final int reid, final int status){
        RequestParams params = new RequestParams();
        switch(status){
            case 1: params.put("getFunc", "snatRequest"); break;
            default: params.put("getFunc", "delRequest"); break;
        }
        params.put("reid", reid);
        params.put("login", Storage.get(getContext(), "login"));
        params.put("password", Storage.get(getContext(), "password"));
        Room101RestClient.post(getContext(), "delRequest.php", params, new Room101RestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(isLaunched()) {
                    draw(R.layout.state_try_again);
                    ((TextView) getActivity().findViewById(R.id.try_again_message)).setText(R.string.wrong_response_from_server);
                    getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            denyRequest(reid, status);
                        }
                    });
                }
            }
            @Override
            public void onProgress(int state) {
                if(isLaunched()) {
                    draw(R.layout.state_loading);
                    TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                    switch (state) {
                        case Room101RestClient.STATE_HANDLING:
                            loading_message.setText(R.string.deny_request);
                            break;
                    }
                }
            }
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                if(statusCode == 302){
                    load(true);
                } else {
                    if(isLaunched()) {
                        draw(R.layout.state_try_again);
                        ((TextView) getActivity().findViewById(R.id.try_again_message)).setText(R.string.wrong_response_from_server);
                        getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                denyRequest(reid, status);
                            }
                        });
                    }
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                fragmentRequestHandle = requestHandle;
            }
        });
    }

    private void addRequest(){
        if(MainActivity.OFFLINE_MODE) {
            snackBar(getString(R.string.device_offline_action_refused));
        } else {
            draw(R.layout.layout_room101_add_request);
            final LinearLayout room101_back = (LinearLayout) getActivity().findViewById(R.id.room101_back);
            final LinearLayout room101_forward = (LinearLayout) getActivity().findViewById(R.id.room101_forward);
            final TextView room101_back_text = (TextView) getActivity().findViewById(R.id.room101_back_text);
            final TextView room101_forward_text = (TextView) getActivity().findViewById(R.id.room101_forward_text);
            final ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.room101_progress_bar);
            final Room101AddRequest room101AddRequest = new Room101AddRequest(getActivity(), new Room101AddRequest.callback() {
                @Override
                public void onProgress(int stage) {
                    try {
                        progressBar.setProgress((stage * 100) / Room101AddRequest.STAGES_COUNT);
                        room101_back.setAlpha(1f);
                        room101_forward.setAlpha(1f);
                        switch (stage) {
                            case Room101AddRequest.STAGE_PICK_DATE_LOAD:
                            case Room101AddRequest.STAGE_PICK_TIME_START_LOAD:
                            case Room101AddRequest.STAGE_PICK_TIME_END_LOAD:
                            case Room101AddRequest.STAGE_PICK_CONFIRMATION_LOAD:
                            case Room101AddRequest.STAGE_PICK_CREATE:
                                room101_back.setAlpha(0.2f);
                                room101_forward.setAlpha(0.2f);
                                if (stage == Room101AddRequest.STAGE_PICK_DATE_LOAD) {
                                    room101_back_text.setText(R.string.do_cancel);
                                    room101_forward_text.setText(R.string.next);
                                }
                                if (stage == Room101AddRequest.STAGE_PICK_TIME_START_LOAD) {
                                    room101_back_text.setText(R.string.back);
                                }
                                break;
                            case Room101AddRequest.STAGE_PICK_DATE:
                                room101_back_text.setText(R.string.do_cancel);
                                room101_forward_text.setText(R.string.next);
                                break;
                            case Room101AddRequest.STAGE_PICK_TIME_START:
                            case Room101AddRequest.STAGE_PICK_TIME_END:
                                room101_back_text.setText(R.string.back);
                                room101_forward_text.setText(R.string.next);
                                break;
                            case Room101AddRequest.STAGE_PICK_CONFIRMATION:
                                room101_back_text.setText(R.string.back);
                                room101_forward_text.setText(R.string.create);
                                break;
                            case Room101AddRequest.STAGE_PICK_DONE:
                                room101_back.setAlpha(0.0f);
                                room101_back_text.setText(R.string.close);
                                room101_forward_text.setText(R.string.close);
                                break;
                        }
                    } catch (Exception e){
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        snackBar(getString(R.string.error_occurred_while_room101_request));
                        load(false);
                    }
                }
                @Override
                public void onDraw(View view) {
                    try {
                        if(isLaunched()) {
                            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.room101_add_request_container));
                            vg.removeAllViews();
                            vg.addView(view);
                        }
                    } catch (Exception e){
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        snackBar(getString(R.string.error_occurred));
                        load(false);
                    }
                }
                @Override
                public void onClose() {
                    load(false);
                }
                @Override
                public void onDone() {
                    load(true);
                }
            });
            getActivity().findViewById(R.id.room101_close_add_request).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    room101AddRequest.close(false);
                }
            });
            getActivity().findViewById(R.id.room101_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    room101AddRequest.back();
                }
            });
            getActivity().findViewById(R.id.room101_forward).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    room101AddRequest.forward();
                }
            });
        }
    }

    private void load(boolean force){
        if(!force || MainActivity.OFFLINE_MODE){
            try {
                final String cache = Cache.get(getContext(), "room101_review");
                if (!Objects.equals(cache, "")) {
                    viewRequest = new JSONObject(cache);
                    display();
                    return;
                }
            } catch (Exception e){
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            }
        }
        if(!MainActivity.OFFLINE_MODE) {
            execute(getContext(), "delRequest", new Room101RestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new Room101ViewRequestParse(new Room101ViewRequestParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                viewRequest = json;
                                if (viewRequest != null) Cache.put(getContext(), "room101_review", viewRequest.toString());
                                display();
                            }
                        }).execute(response);
                    } else {
                        loadFailed();
                    }
                }
                @Override
                public void onProgress(int state) {
                    if(isLaunched()) {
                        draw(R.layout.state_loading);
                        TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                        switch (state) {
                            case Room101RestClient.STATE_HANDLING:
                                loading_message.setText(R.string.loading);
                                break;
                        }
                    }
                }
                @Override
                public void onFailure(int state, int statusCode, Header[] headers) {
                    switch (state) {
                        case Room101RestClient.FAILED_OFFLINE:
                            try {
                                final String cache = Cache.get(getContext(), "room101_review");
                                if (!Objects.equals(cache, "")) {
                                    viewRequest = new JSONObject(cache);
                                    display();
                                    return;
                                }
                            } catch (Exception e) {
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                            }
                            if(isLaunched()) {
                                draw(R.layout.state_offline);
                                getActivity().findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        load(true);
                                    }
                                });
                            }
                            break;
                        case Room101RestClient.FAILED_TRY_AGAIN:
                        case Room101RestClient.FAILED_EXPECTED_REDIRECTION:
                            if(isLaunched()) {
                                draw(R.layout.state_try_again);
                                if (state == Room101RestClient.FAILED_EXPECTED_REDIRECTION) ((TextView) getActivity().findViewById(R.id.try_again_message)).setText(R.string.wrong_response_from_server);
                                getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        load(true);
                                    }
                                });
                            }
                            break;
                        case Room101RestClient.FAILED_AUTH:
                            if(isLaunched()) {
                                draw(R.layout.state_try_again);
                                View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                                ((ViewGroup) try_again_reload.getParent()).removeView(try_again_reload);
                                ((TextView) getActivity().findViewById(R.id.try_again_message)).setText(R.string.room101_auth_failed);
                            }
                            break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    fragmentRequestHandle = requestHandle;
                }
            });
        } else {
            if(isLaunched()) {
                draw(R.layout.state_offline);
                getActivity().findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load(true);
                    }
                });
            }
        }
    }
    private void display(){
        try {
            if(!isLaunched()) return;
            if(viewRequest == null) throw new NullPointerException("viewRequest cannot be null");
            draw(R.layout.layout_room101_review);
            ((TextView) getActivity().findViewById(R.id.room101_limit)).setText(viewRequest.getString("limit"));
            ((TextView) getActivity().findViewById(R.id.room101_last)).setText(viewRequest.getString("left"));
            ((TextView) getActivity().findViewById(R.id.room101_penalty)).setText(viewRequest.getString("penalty"));
            final LinearLayout room101_review_container = (LinearLayout) getActivity().findViewById(R.id.room101_review_container);
            (new Room101ReviewBuilder(getActivity(), this, viewRequest.getJSONArray("sessions"), new Room101ReviewBuilder.response(){
                public void state(final int state, final LinearLayout layout){
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                room101_review_container.removeAllViews();
                                if (state == Room101ReviewBuilder.STATE_DONE || state == Room101ReviewBuilder.STATE_LOADING) {
                                    room101_review_container.addView(layout);
                                } else if (state == Room101ReviewBuilder.STATE_FAILED) {
                                    loadFailed();
                                }
                            }
                        });
                    } catch (NullPointerException e){
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        loadFailed();
                    }
                }
            })).start();
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.room101_review_swipe);
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
            mSwipeRefreshLayout.setColorSchemeColors(typedValue.data);
            getActivity().getTheme().resolveAttribute(R.attr.colorBackgroundRefresh, typedValue, true);
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(typedValue.data);
            mSwipeRefreshLayout.setOnRefreshListener(this);
            // плавающая кнопка
            FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addRequest();
                }
            });
            // показываем снекбар с датой обновления
            long timestamp = viewRequest.getLong("timestamp");
            int shift = (int) ((Calendar.getInstance().getTimeInMillis() - timestamp) / 1000);
            if (shift > 300) {
                String message;
                if (shift < 21600) {
                    if (shift < 3600) {
                        message = shift / 60 + " " + getString(R.string.min_past);
                    } else {
                        message = shift / 3600 + " " + getString(R.string.hour_past);
                    }
                } else {
                    message = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(timestamp));
                }
                snackBar(getString(R.string.update_date) + " " + message);
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            loadFailed();
        }
    }
    private void loadFailed(){
        try {
            if(isLaunched()) {
                draw(R.layout.state_try_again);
                getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load(true);
                    }
                });
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    private void draw(int layoutId){
        try {
            if(isLaunched()) {
                ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_room101));
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    static void execute(final Context context, String scope, final Room101RestClientResponseHandler responseHandler){
        RequestParams params = new RequestParams();
        params.put("getFunc", "isLoginPassword");
        params.put("view", scope);
        params.put("login", Storage.get(context, "login"));
        params.put("password", Storage.get(context, "password"));
        Room101RestClient.post(context, "index.php", params, new Room101RestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if (response.contains("Доступ запрещен") || (response.contains("Неверный") && response.contains("логин/пароль"))) {
                    responseHandler.onFailure(Room101RestClient.FAILED_AUTH, Room101RestClient.STATUS_CODE_EMPTY, null);
                } else {
                    responseHandler.onFailure(Room101RestClient.FAILED_EXPECTED_REDIRECTION, Room101RestClient.STATUS_CODE_EMPTY, null);
                }
            }
            @Override
            public void onProgress(int state) {
                responseHandler.onProgress(state);
            }
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                if(statusCode == 302){
                    boolean found = false;
                    for(Header header : headers){
                        if(Objects.equals(header.getName(), "Location")){
                            String url = header.getValue().trim();
                            if(!url.isEmpty()){
                                found = true;
                                Room101RestClient.get(context, url, null, new Room101RestClientResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, String response) {
                                        responseHandler.onSuccess(statusCode, response);
                                    }
                                    @Override
                                    public void onProgress(int state) {
                                        responseHandler.onProgress(state);
                                    }
                                    @Override
                                    public void onFailure(int state, int statusCode, Header[] headers) {
                                        responseHandler.onFailure(state, statusCode, headers);
                                    }
                                    @Override
                                    public void onNewHandle(RequestHandle requestHandle) {
                                        responseHandler.onNewHandle(requestHandle);
                                    }
                                });
                            }
                            break;
                        }
                    }
                    if(!found){
                        responseHandler.onFailure(Room101RestClient.FAILED_EXPECTED_REDIRECTION, Room101RestClient.STATUS_CODE_EMPTY, null);
                    }
                } else {
                    responseHandler.onFailure(Room101RestClient.FAILED_EXPECTED_REDIRECTION, Room101RestClient.STATUS_CODE_EMPTY, null);
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                responseHandler.onNewHandle(requestHandle);
            }
        });
    }
    private void snackBar(String text){
        if(isLaunched()) {
            Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.room101_review_swipe), text, Snackbar.LENGTH_SHORT);
            getActivity().getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, MainActivity.typedValue, true);
            snackbar.getView().setBackgroundColor(MainActivity.typedValue.data);
            snackbar.show();
        }
    }
    private void interrupt(){
        interrupted = true;
        if(fragmentRequestHandle != null){
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }
    private void relaunch(){
        interrupted = false;
        if(!loaded) {
            loaded = true;
            load(true);
        }
    }
    private boolean isLaunched(){
        return !interrupted;
    }
}

class Room101RestClient {

    private static final String TAG = "Room101RestClient";
    private static final String BASE_URL = "http://de.ifmo.ru/m/";
    private static final String USER_AGENT = "Android Application";
    private static AsyncHttpClient httpclient = new AsyncHttpClient();
    private static boolean initialized = false;
    private static String PHPSESSID = "";

    static final int STATE_HANDLING = 0;

    static final int FAILED_OFFLINE = 0;
    static final int FAILED_TRY_AGAIN = 1;
    static final int FAILED_AUTH = 2;
    static final int FAILED_EXPECTED_REDIRECTION = 3;

    static final int STATUS_CODE_EMPTY = -1;

    static void init(){
        if(!initialized){
            httpclient.setLoggingLevel(Log.WARN);
            httpclient.addHeader("User-Agent", USER_AGENT);
            httpclient.addHeader("Cookie", "PHPSESSID=" + PHPSESSID + "; autoexit=true;");
            initialized = true;
        }
    }

    static void get(Context context, final String url, final RequestParams params, final Room101RestClientResponseHandler responseHandler){
        if(isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie();
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
                    try {
                        String data = "";
                        if (responseBody != null) data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        responseHandler.onSuccess(statusCode, data);
                    } catch (UnsupportedEncodingException e) {
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }
    static void post(Context context, final String url, final RequestParams params, final Room101RestClientResponseHandler responseHandler){
        if(isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie();
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
                    try {
                        String data = "";
                        if (responseBody != null) data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        responseHandler.onSuccess(statusCode, data);
                    } catch (UnsupportedEncodingException e) {
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
    private static void renewCookie(){
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("Cookie", "PHPSESSID=" + PHPSESSID + "; autoexit=true;");
    }
    private static void analyseCookie(Header[] headers){
        if (headers == null) return;
        for (Header header : headers) {
            if (Objects.equals(header.getName(), "Cookie") || Objects.equals(header.getName(), "Set-Cookie")) {
                String[] pairs = header.getValue().trim().split(";");
                for (String pair : pairs) {
                    String[] cookie = pair.split("=");
                    if (Objects.equals(cookie[0], "PHPSESSID") && !Objects.equals(cookie[1], "") && cookie[1] != null) PHPSESSID = cookie[1];
                }
            }
        }
    }
    static boolean isOnline(Context context) {
        if(context != null) {
            NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        } else {
            return true;
        }
    }
}
interface Room101RestClientResponseHandler {
    void onSuccess(int statusCode, String response);
    void onProgress(int state);
    void onFailure(int state, int statusCode, Header[] headers);
    void onNewHandle(RequestHandle requestHandle);
}

class Room101ViewRequestParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    Room101ViewRequestParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            JSONObject response = new JSONObject();
            TagNode multi_table = root.getElementsByAttValue("class", "multi_table", true, false)[0];
            TagNode[] d_table = multi_table.getElementsByName("table", true);
            TagNode[] tds = d_table[0].getAllElements(false)[0].getAllElements(false)[1].getAllElements(false);
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
            response.put("date", tds[0].getText().toString().trim());
            response.put("limit", tds[1].getText().toString().trim());
            response.put("left", tds[2].getText().toString().trim());
            response.put("penalty", tds[3].getText().toString().trim());
            TagNode[] trs = d_table[1].getAllElements(false)[0].getAllElements(false);
            JSONArray sessions = new JSONArray();
            for(int i = 1; i < trs.length; i++){
                tds = trs[i].getAllElements(false);
                JSONObject session = new JSONObject();
                session.put("number", tds[0].getText().toString().trim());
                session.put("date", tds[1].getText().toString().trim());
                session.put("time", tds[2].getText().toString().trim());
                TagNode[] inputs = tds[3].getElementsByName("input", false);
                if(inputs.length > 0){
                    session.put("status", inputs[0].getAttributeByName("value").trim());
                    Matcher m = Pattern.compile(".*document\\.fn\\.reid\\.value=(\\d+).*").matcher(inputs[0].getAttributeByName("onclick").trim());
                    if(m.find()){
                        int reid = 0;
                        try {
                            reid = Integer.parseInt(m.group(1));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        session.put("reid", reid);
                    } else {
                        session.put("reid", 0);
                    }
                } else {
                    session.put("status", tds[3].getText().toString().trim());
                    session.put("reid", 0);
                }
                session.put("requested", tds[4].getText().toString().trim());
                sessions.put(session);
            }
            response.put("sessions", sessions);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class Room101ReviewBuilder extends Thread {

    interface response {
        void state(int state, LinearLayout layout);
    }
    interface register {
        void onDenyRequest(int reid, int status);
    }
    private response delegate = null;
    private register register = null;
    private Activity activity;
    private JSONArray sessions;
    private float destiny;

    static final int STATE_FAILED = 0;
    static final int STATE_LOADING = 1;
    static final int STATE_DONE = 2;

    Room101ReviewBuilder(Activity activity, register register, JSONArray sessions, response delegate){
        this.activity = activity;
        this.register = register;
        this.delegate = delegate;
        this.sessions = sessions;
        this.destiny = activity.getResources().getDisplayMetrics().density;
    }
    public void run(){
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        try {
            delegate.state(STATE_LOADING, getLoadingScreen());
            if(sessions.length() > 0){
                for(int i = 0; i < sessions.length(); i++){
                    JSONObject request = sessions.getJSONObject(i);
                    LinearLayout requestLayout = new LinearLayout(activity);
                    requestLayout.setOrientation(LinearLayout.HORIZONTAL);
                    requestLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    FrameLayout numberLayout = new FrameLayout(activity);
                    numberLayout.setLayoutParams(new LinearLayout.LayoutParams((int) (40 * destiny), (int) (40 * destiny)));
                    TextView numberText = new TextView(activity);
                    numberText.setText(request.getString("number") + ")");
                    numberText.setGravity(Gravity.CENTER);
                    numberText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    numberText.setTextColor(MainActivity.textColorPrimary);
                    numberLayout.addView(numberText);
                    requestLayout.addView(numberLayout);
                    LinearLayout contentLayout = new LinearLayout(activity);
                    contentLayout.setOrientation(LinearLayout.HORIZONTAL);
                    contentLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (40 * destiny)));
                    TextView contentText = new TextView(activity);
                    contentText.setText(request.getString("date") + "    " + request.getString("time") + "    " + request.getString("status"));
                    contentText.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    lp.weight = 200;
                    contentText.setLayoutParams(lp);
                    contentText.setTextColor(MainActivity.textColorPrimary);
                    contentLayout.addView(contentText);
                    FrameLayout denyLayout = new FrameLayout(activity);
                    lp = new LinearLayout.LayoutParams((int) (40 * destiny), (int) (40 * destiny));
                    lp.weight = 1;
                    denyLayout.setLayoutParams(lp);
                    final int reid = request.getInt("reid");
                    final String statusText = request.getString("status");
                    if (reid != 0){
                        ImageView denyImg = new ImageView(activity);
                        denyImg.setLayoutParams(new LinearLayout.LayoutParams((int) (40 * destiny), (int) (40 * destiny)));
                        denyImg.setPadding((int) (8 * destiny), (int) (8 * destiny), (int) (8 * destiny), (int) (8 * destiny));
                        denyImg.setImageDrawable(activity.getDrawable(R.drawable.ic_close));
                        denyImg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int status = 0;
                                if(Objects.equals(statusText, "удовлетворена")) status = 1;
                                register.onDenyRequest(reid, status);
                            }
                        });
                        denyLayout.addView(denyImg);
                    }
                    contentLayout.addView(denyLayout);
                    requestLayout.addView(contentLayout);
                    container.addView(requestLayout);
                    View separator = new View(activity);
                    separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                    separator.setBackgroundColor(MainActivity.colorSeparator);
                    container.addView(separator);
                }
            } else {
                container.addView(getEmptyScreen());
            }
            delegate.state(STATE_DONE, container);
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            delegate.state(STATE_FAILED, container);
        }
    }

    private LinearLayout getLoadingScreen() throws Exception {
        LinearLayout loadingLayout = new LinearLayout(activity);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        loadingLayout.setPadding((int) (16 * destiny), (int) (16 * destiny), (int) (16 * destiny), (int) (16 * destiny));
        ProgressBar progressBar = new ProgressBar(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(lp);
        loadingLayout.addView(progressBar);
        return loadingLayout;
    }
    private LinearLayout getEmptyScreen() throws Exception {
        LinearLayout emptyLayout = new LinearLayout(activity);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView textView = new TextView(activity);
        textView.setText(R.string.no_requests);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setTextColor(MainActivity.textColorPrimary);
        textView.setGravity(Gravity.CENTER);
        textView.setHeight((int) (60 * destiny));
        emptyLayout.addView(textView);
        return emptyLayout;
    }
}

class Room101AddRequest {

    private static final String TAG = "Room101AddRequest";
    interface callback {
        void onProgress(int stage);
        void onDraw(View view);
        void onClose();
        void onDone();
    }
    private callback callback;
    static final int STAGE_PICK_DATE_LOAD = 0;
    static final int STAGE_PICK_DATE = 1;
    static final int STAGE_PICK_TIME_START_LOAD = 2;
    static final int STAGE_PICK_TIME_START = 3;
    static final int STAGE_PICK_TIME_END_LOAD = 4;
    static final int STAGE_PICK_TIME_END = 5;
    static final int STAGE_PICK_CONFIRMATION_LOAD = 6;
    static final int STAGE_PICK_CONFIRMATION = 7;
    static final int STAGE_PICK_CREATE = 8;
    static final int STAGE_PICK_DONE = 9;
    static final int STAGES_COUNT = 9;
    private int CURRENT_STAGE = 0;
    private Activity context = null;

    private RequestHandle ARequestHandle = null;

    private JSONObject data = null;
    private String pick_date = null;
    private String pick_time_start = null;
    private String pick_time_end = null;

    Room101AddRequest(Activity context, callback callback){
        this.callback = callback;
        this.context = context;
        proceedStage();
    }

    void back(){
        switch (CURRENT_STAGE){
            case STAGE_PICK_DATE_LOAD:
            case STAGE_PICK_TIME_START_LOAD:
            case STAGE_PICK_TIME_END_LOAD:
            case STAGE_PICK_CONFIRMATION_LOAD:
            case STAGE_PICK_CREATE:
            case STAGE_PICK_DONE:
                return;
        }
        CURRENT_STAGE -= 3;
        data = null;
        if(CURRENT_STAGE < 0){
            close(false);
        } else {
            proceedStage();
        }
    }
    void forward(){
        switch (CURRENT_STAGE){
            case STAGE_PICK_DATE_LOAD:
            case STAGE_PICK_TIME_START_LOAD:
            case STAGE_PICK_TIME_END_LOAD:
            case STAGE_PICK_CONFIRMATION_LOAD:
            case STAGE_PICK_CREATE:
                return;
        }
        switch (CURRENT_STAGE){
            case STAGE_PICK_DATE: if(pick_date == null){ snackBar(context.getString(R.string.need_to_peek_date)); return; } break;
            case STAGE_PICK_TIME_START: if(pick_time_start == null){ snackBar(context.getString(R.string.need_to_peek_time_start)); return; } break;
            case STAGE_PICK_TIME_END: if(pick_time_end == null){ snackBar(context.getString(R.string.need_to_peek_time_end)); return; } break;
            case STAGE_PICK_DONE: close(true); return;
        }
        CURRENT_STAGE++;
        proceedStage();
    }
    void close(boolean done){
        if(ARequestHandle != null) ARequestHandle.cancel(true);
        if(done){
            callback.onDone();
        } else {
            callback.onClose();
        }
    }
    private void proceedStage(){
        callback.onProgress(CURRENT_STAGE);
        switch (CURRENT_STAGE){
            case STAGE_PICK_DATE_LOAD: loadDatePick(); break;
            case STAGE_PICK_DATE: datePick(); break;
            case STAGE_PICK_TIME_START_LOAD: loadTimeStartPick(); break;
            case STAGE_PICK_TIME_START: timeStartPick(); break;
            case STAGE_PICK_TIME_END_LOAD: loadTimeEndPick(); break;
            case STAGE_PICK_TIME_END: timeEndPick(); break;
            case STAGE_PICK_CONFIRMATION_LOAD: loadConfirmation(); break;
            case STAGE_PICK_CONFIRMATION: confirmation(); break;
            case STAGE_PICK_CREATE: create(); break;
            case STAGE_PICK_DONE: done(); break;
        }
    }

    private void loadDatePick(){
        callback.onDraw(getLoadingLayout(context.getString(R.string.data_loading)));
        data = null;
        pick_date = null;
        Room101Fragment.execute(context, "newRequest", new Room101RestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new DatePickParse(new DatePickParse.response() {
                        @Override
                        public void finish(JSONObject json) {
                            if(json != null){
                                data = json;
                                CURRENT_STAGE++;
                                proceedStage();
                            } else {
                                failed();
                            }
                        }
                    }).execute(response);
                } else {
                    failed();
                }
            }
            @Override
            public void onProgress(int state) {}
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                failed();
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                ARequestHandle = requestHandle;
            }
        });
    }
    private void loadTimeStartPick(){
        callback.onDraw(getLoadingLayout(context.getString(R.string.data_handling)));
        data = null;
        pick_time_start = null;
        RequestParams params = new RequestParams();
        params.put("getFunc", "getWindowBegin");
        params.put("dateRequest", pick_date);
        params.put("timeBegin", "");
        params.put("timeEnd", "");
        params.put("login", Storage.get(context, "login"));
        params.put("password", Storage.get(context, "password"));
        Room101RestClient.post(context, "newRequest.php", params, new Room101RestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new TimeStartPickParse(new TimeStartPickParse.response() {
                        @Override
                        public void finish(JSONObject json) {
                            if(json != null){
                                data = json;
                                CURRENT_STAGE++;
                                proceedStage();
                            } else {
                                failed();
                            }
                        }
                    }).execute(response);
                } else {
                    failed();
                }
            }
            @Override
            public void onProgress(int state) {}
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                failed();
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                ARequestHandle = requestHandle;
            }
        });
    }
    private void loadTimeEndPick(){
        callback.onDraw(getLoadingLayout(context.getString(R.string.data_handling)));
        data = null;
        pick_time_end = null;
        RequestParams params = new RequestParams();
        params.put("getFunc", "getWindowEnd");
        params.put("dateRequest", pick_date);
        params.put("timeBegin", pick_time_start);
        params.put("timeEnd", "");
        params.put("login", Storage.get(context, "login"));
        params.put("password", Storage.get(context, "password"));
        Room101RestClient.post(context, "newRequest.php", params, new Room101RestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new TimeEndPickParse(new TimeEndPickParse.response() {
                        @Override
                        public void finish(JSONObject json) {
                            if(json != null){
                                data = json;
                                CURRENT_STAGE++;
                                proceedStage();
                            } else {
                                failed();
                            }
                        }
                    }).execute(response);
                } else {
                    failed();
                }
            }
            @Override
            public void onProgress(int state) {}
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                failed();
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                ARequestHandle = requestHandle;
            }
        });
    }
    private void loadConfirmation(){
        data = null;
        CURRENT_STAGE++;
        proceedStage();
    }
    private void create(){
        callback.onDraw(getLoadingLayout(context.getString(R.string.add_request)));
        data = null;
        RequestParams params = new RequestParams();
        params.put("getFunc", "saveRequest");
        params.put("dateRequest", pick_date);
        params.put("timeBegin", pick_time_start);
        params.put("timeEnd", pick_time_end);
        params.put("login", Storage.get(context, "login"));
        params.put("password", Storage.get(context, "password"));
        Room101RestClient.post(context, "newRequest.php", params, new Room101RestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                try {
                    data = new JSONObject();
                    data.put("done", false);
                    CURRENT_STAGE++;
                    proceedStage();
                } catch (JSONException e) {
                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                    failed();
                }
            }
            @Override
            public void onProgress(int state) {}
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                if(statusCode == 302){
                    try {
                        data = new JSONObject();
                        data.put("done", true);
                        CURRENT_STAGE++;
                        proceedStage();
                    } catch (JSONException e) {
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        failed();
                    }
                } else {
                    failed();
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                ARequestHandle = requestHandle;
            }
        });
    }

    private void datePick(){
        try {
            if (data == null) throw new NullPointerException("data cannot be null");
            if (!Objects.equals(data.getString("type"), "date_pick")) throw new Exception("Wrong data.type. Expected 'date_pick', got '" + data.getString("type") + "'");
            if (!data.has("data")) throw new Exception("Empty data.data");
            final JSONArray date_pick = data.getJSONArray("data");
            if(date_pick.length() > 0){
                callback.onDraw(getChooserLayout(context.getString(R.string.peek_date), "", date_pick, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            try {
                                pick_date = buttonView.getText().toString().trim();
                            } catch (Exception e) {
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                                failed();
                            }
                        }
                    }
                }));
            } else {
                callback.onDraw(getEmptyLayout(context.getString(R.string.no_date_to_peek)));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
    }
    private void timeStartPick(){
        try {
            if (data == null) throw new NullPointerException("data cannot be null");
            if (!Objects.equals(data.getString("type"), "time_start_pick")) throw new Exception("Wrong data.type. Expected 'time_start_pick', got '" + data.getString("type") + "'");
            if (!data.has("data")) throw new Exception("Empty data.data");
            final JSONArray time_pick = data.getJSONArray("data");
            if(time_pick.length() > 0){
                callback.onDraw(getChooserLayout(context.getString(R.string.peek_time_start), "", time_pick, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            try {
                                pick_time_start = buttonView.getText().toString().trim() + ":00";
                            } catch (Exception e) {
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                                failed();
                            }
                        }
                    }
                }));
            } else {
                callback.onDraw(getEmptyLayout(context.getString(R.string.no_time_to_peek)));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
    }
    private void timeEndPick(){
        try {
            if (data == null) throw new NullPointerException("data cannot be null");
            if (!Objects.equals(data.getString("type"), "time_end_pick")) throw new Exception("Wrong data.type. Expected 'time_end_pick', got '" + data.getString("type") + "'");
            if (!data.has("data")) throw new Exception("Empty data.data");
            final JSONArray time_pick = data.getJSONArray("data");
            if(time_pick.length() > 0){
                callback.onDraw(getChooserLayout(context.getString(R.string.peek_time_end), context.getString(R.string.peek_time_end_desc), time_pick, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            try {
                                pick_time_end = buttonView.getText().toString().trim() + ":00";
                            } catch (Exception e) {
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                                failed();
                            }
                        }
                    }
                }));
            } else {
                callback.onDraw(getEmptyLayout(context.getString(R.string.no_time_to_peek)));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
    }
    private void confirmation(){
        try {
            if(pick_date == null) throw new NullPointerException("pick_date cannot be null");
            if(pick_time_start == null) throw new NullPointerException("pick_time_start cannot be null");
            if(pick_time_end == null) throw new NullPointerException("pick_time_end cannot be null");
            callback.onDraw(getChooserLayout(context.getString(R.string.attention) + "!", context.getString(R.string.room101_warning), null, null));
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
    }
    private void done(){
        try {
            if (data == null) throw new NullPointerException("data cannot be null");
            if (!data.has("done")) throw new Exception("Empty data.done");
            callback.onDraw(getChooserLayout(data.getBoolean("done") ? context.getString(R.string.request_accepted) : context.getString(R.string.request_denied), "", null, null));
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
    }

    private void failed(){
        snackBar(context.getString(R.string.error_occurred));
        close(false);
    }
    private void snackBar(String text){
        try {
            Snackbar snackbar = Snackbar.make(context.findViewById(R.id.container_room101), text, Snackbar.LENGTH_SHORT);
            context.getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, MainActivity.typedValue, true);
            snackbar.getView().setBackgroundColor(MainActivity.typedValue.data);
            snackbar.show();
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    private LinearLayout getLoadingLayout(String text){
        LinearLayout loading = new LinearLayout(context);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        loading.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        progressBar.setPadding(0, (int) (16 * MainActivity.destiny), 0, (int) (10 * MainActivity.destiny));
        loading.addView(progressBar);
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setTextColor(MainActivity.textColorPrimary);
        loading.addView(textView);
        return loading;
    }
    private LinearLayout getEmptyLayout(String text){
        LinearLayout empty = new LinearLayout(context);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        empty.setGravity(Gravity.CENTER);
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setTextColor(MainActivity.textColorPrimary);
        textView.setPadding(0, (int) (24 * MainActivity.destiny), 0, (int) (24 * MainActivity.destiny));
        empty.addView(textView);
        return empty;
    }
    private LinearLayout getChooserLayout(String header, String desc, JSONArray array, CompoundButton.OnCheckedChangeListener onCheckedChangeListener){
        LinearLayout chooser = new LinearLayout(context);
        chooser.setOrientation(LinearLayout.VERTICAL);
        chooser.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        chooser.setPadding((int) (16 * MainActivity.destiny), (int) (8 * MainActivity.destiny), (int) (16 * MainActivity.destiny), (int) (8 * MainActivity.destiny));
        try {
            if(pick_date != null || pick_time_start != null || pick_time_end != null){
                LinearLayout requestLayout = new LinearLayout(context);
                requestLayout.setOrientation(LinearLayout.VERTICAL);
                requestLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                requestLayout.setPadding(0, (int) (8 * MainActivity.destiny), 0, (int) (8 * MainActivity.destiny));
                TextView textView = new TextView(context);
                textView.setText(context.getString(R.string.request) + ":");
                textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                textView.setTextColor(MainActivity.textColorPrimary);
                requestLayout.addView(textView);
                if(pick_date != null){
                    textView = new TextView(context);
                    textView.setText(context.getString(R.string.session_date) + ":" + " " + pick_date);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(MainActivity.textColorSecondary);
                    requestLayout.addView(textView);
                }
                if(pick_time_start != null){
                    textView = new TextView(context);
                    textView.setText(context.getString(R.string.time_start) + ":" + " " + pick_time_start.replaceAll(":00$", ""));
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(MainActivity.textColorSecondary);
                    requestLayout.addView(textView);
                }
                if(pick_time_end != null){
                    textView = new TextView(context);
                    textView.setText(context.getString(R.string.time_end) + ":" + " " + pick_time_end.replaceAll(":00$", ""));
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(MainActivity.textColorSecondary);
                    requestLayout.addView(textView);
                }
                chooser.addView(requestLayout);
            }
            TextView textView = new TextView(context);
            textView.setText(header);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(MainActivity.textColorPrimary);
            textView.setPadding(0, (int) (8 * MainActivity.destiny), 0, (int) (8 * MainActivity.destiny));
            chooser.addView(textView);
            if (array != null && array.length() > 0) {
                RadioGroup radioGroup = new RadioGroup(context);
                radioGroup.setOrientation(RadioGroup.VERTICAL);
                radioGroup.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                for (int i = 0; i < array.length(); i++) {
                    RadioButton radioButton = new RadioButton(context);
                    radioButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    radioButton.setText(array.getString(i));
                    radioButton.setOnCheckedChangeListener(onCheckedChangeListener);
                    radioGroup.addView(radioButton);
                }
                chooser.addView(radioGroup);
            }
            if(desc != null && !Objects.equals(desc, "")){
                TextView descView = new TextView(context);
                descView.setText(desc);
                descView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                descView.setTextColor(MainActivity.textColorSecondary);
                descView.setPadding(0, (int) (8 * MainActivity.destiny), 0, (int) (8 * MainActivity.destiny));
                chooser.addView(descView);
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
        return chooser;
    }
}

class DatePickParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    DatePickParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "date_pick");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] tables = root.getElementsByAttValue("class", "d_table2 calendar_1", true, false);
            if(tables == null || tables.length == 0){
                response.put("data", new JSONArray());
                return response;
            }
            TagNode table = tables[0];
            TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            JSONArray dates = new JSONArray();
            for(TagNode tr : trs){
                counter++;
                if(counter == 1 || counter == 2) continue;
                TagNode[] tds = tr.getElementsByName("td", false);
                for(TagNode td : tds){
                    if(!td.hasChildren()) continue;
                    TagNode[] inputs = td.getElementsByName("input", false);
                    if(inputs == null || inputs.length == 0) continue;
                    TagNode input = inputs[0];
                    if(!input.hasAttribute("disabled")){
                        String onclick = input.getAttributeByName("onclick");
                        if(onclick != null && !Objects.equals(onclick, "")) {
                            Matcher m = Pattern.compile(".*dateRequest\\.value='(.*)';.*").matcher(onclick);
                            if (m.find()) {
                                dates.put(m.group(1));
                            }
                        }
                    }
                }
            }
            response.put("data", dates);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class TimeStartPickParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    TimeStartPickParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "time_start_pick");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] tables = root.getElementsByAttValue("class", "d_table min_lmargin_table", true, false);
            if(tables == null || tables.length == 0){
                response.put("data", new JSONArray());
                return response;
            }
            TagNode table = tables[0];
            TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            JSONArray times = new JSONArray();
            for(TagNode tr : trs){
                counter++;
                if(counter == 1) continue;
                TagNode td = tr.getElementsByName("td", false)[0];
                TagNode[] inputs = td.getElementsByName("input", false);
                if(inputs == null || inputs.length == 0) continue;
                TagNode input = inputs[0];
                if(!input.hasAttribute("disabled")){
                    String value = input.getAttributeByName("value");
                    if(value != null) times.put(value);
                }
            }
            response.put("data", times);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class TimeEndPickParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    TimeEndPickParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "time_end_pick");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] tables = root.getElementsByAttValue("class", "d_table min_lmargin_table", true, false);
            if(tables == null || tables.length == 0){
                response.put("data", new JSONArray());
                return response;
            }
            TagNode table = tables[0];
            TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            JSONArray times = new JSONArray();
            for(TagNode tr : trs){
                counter++;
                if(counter == 1) continue;
                TagNode td = tr.getElementsByName("td", false)[1];
                TagNode[] inputs = td.getElementsByName("input", false);
                if(inputs == null || inputs.length == 0) continue;
                TagNode input = inputs[0];
                if(!input.hasAttribute("disabled")){
                    String value = input.getAttributeByName("value");
                    if(value != null) times.put(value);
                }
            }
            response.put("data", times);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}