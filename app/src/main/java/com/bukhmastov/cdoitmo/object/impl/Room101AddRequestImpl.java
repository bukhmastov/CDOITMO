package com.bukhmastov.cdoitmo.object.impl;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.Room101Fragment;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.Room101AddRequest;
import com.bukhmastov.cdoitmo.parse.room101.Room101DatePickParse;
import com.bukhmastov.cdoitmo.parse.room101.Room101TimeEndPickParse;
import com.bukhmastov.cdoitmo.parse.room101.Room101TimeStartPickParse;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101AddRequestImpl implements Room101AddRequest {

    private static final String TAG = "Room101AddRequest";
    private Activity activity = null;
    private Callback callback;
    private Pattern timePickerPattern;
    private int CURRENT_STAGE = 0;
    private Client.Request requestHandle = null;
    private JSONObject data = null;
    private String pick_date = null;
    private String pick_time_start = null;
    private String pick_time_end = null;

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private InjectProvider injectProvider = InjectProvider.instance();
    //@Inject
    private Room101Client room101Client = Room101Client.instance();
    //@Inject
    private NotificationMessage notificationMessage = NotificationMessage.instance();
    //@Inject
    private Static staticUtil = Static.instance();
    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Override
    public void start(@NonNull Activity activity, @NonNull Callback callback) {
        this.callback = callback;
        this.activity = activity;
        this.timePickerPattern = Pattern.compile("^(\\d{1,2}:\\d{2})\\s?(\\((" + activity.getString(R.string.room101_available) + ":\\s)?(\\d*)\\))?$");
        proceedStage();
    }

    @Override
    public void back() {
        log.v(TAG, "back");
        switch (CURRENT_STAGE) {
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
        if (CURRENT_STAGE < 0) {
            close(false);
        } else {
            proceedStage();
        }
    }

    @Override
    public void forward() {
        log.v(TAG, "forward");
        switch (CURRENT_STAGE) {
            case STAGE_PICK_DATE_LOAD:
            case STAGE_PICK_TIME_START_LOAD:
            case STAGE_PICK_TIME_END_LOAD:
            case STAGE_PICK_CONFIRMATION_LOAD:
            case STAGE_PICK_CREATE:
                return;
        }
        switch (CURRENT_STAGE) {
            case STAGE_PICK_DATE:
                if (pick_date == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_peek_date));
                    return;
                }
                break;
            case STAGE_PICK_TIME_START:
                if (pick_time_start == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_peek_time_start));
                    return;
                }
                break;
            case STAGE_PICK_TIME_END:
                if (pick_time_end == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_peek_time_end));
                    return;
                }
                break;
            case STAGE_PICK_DONE:
                close(true);
                return;
        }
        CURRENT_STAGE++;
        proceedStage();
    }

    @Override
    public void close(boolean done) {
        log.v(TAG, "close | done=" + (done ? "true" : "false"));
        if (requestHandle != null) requestHandle.cancel();
        if (done) {
            callback.onDone();
        } else {
            callback.onClose();
        }
    }

    private void proceedStage() {
        log.v(TAG, "proceedStage | CURRENT_STAGE=" + CURRENT_STAGE);
        callback.onProgress(CURRENT_STAGE);
        switch (CURRENT_STAGE){
            case STAGE_PICK_DATE_LOAD: loadDatePick(0); break;
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

    private void loadDatePick(final int stage) {
        thread.run(() -> {
            log.v(TAG, "loadDatePick | stage=" + stage);
            if (stage == 0) {
                callback.onDraw(getLoadingLayout(activity.getString(R.string.data_loading)));
                data = null;
                pick_date = null;
                Room101Fragment.execute(activity, room101Client, injectProvider, "newRequest", new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        thread.run(() -> {
                            if (statusCode == 200) {
                                new Room101DatePickParse(response, json -> {
                                    if (json != null) {
                                        data = json;
                                        loadDatePick(1);
                                    } else {
                                        failed();
                                    }
                                }).run();
                            } else {
                                failed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        if (state == Room101Client.FAILED_SERVER_ERROR) {
                            failed(Room101Client.getFailureMessage(activity, statusCode));
                        } else {
                            failed();
                        }
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            } else if (stage == 1) {
                HashMap<String, String> params = new HashMap<>();
                params.put("month", "next");
                params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
                params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
                room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        thread.run(() -> {
                            if (statusCode == 200) {
                                new Room101DatePickParse(response, json -> {
                                    if (json != null) {
                                        if (data == null) {
                                            data = json;
                                        } else {
                                            try {
                                                JSONArray jsonArray = data.getJSONArray("data");
                                                JSONArray jsonArrayNew = json.getJSONArray("data");
                                                for (int i = 0; i < jsonArrayNew.length(); i++) jsonArray.put(jsonArrayNew.getJSONObject(i));
                                                data.put("data", jsonArray);
                                            } catch (Exception e) {
                                                log.exception(e);
                                                data = json;
                                            }
                                        }
                                    }
                                    CURRENT_STAGE++;
                                    proceedStage();
                                }).run();
                            } else {
                                failed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        if (state == Room101Client.FAILED_SERVER_ERROR) {
                            failed(Room101Client.getFailureMessage(activity, statusCode));
                        } else {
                            failed();
                        }
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            } else {
                failed();
            }
        });
    }
    private void loadTimeStartPick() {
        thread.run(() -> {
            log.v(TAG, "loadTimeStartPick");
            callback.onDraw(getLoadingLayout(activity.getString(R.string.data_handling)));
            data = null;
            pick_time_start = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "getWindowBegin");
            params.put("dateRequest", pick_date);
            params.put("timeBegin", "");
            params.put("timeEnd", "");
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.run(() -> {
                        if (statusCode == 200) {
                            new Room101TimeStartPickParse(response, json -> {
                                if (json != null) {
                                    data = json;
                                    CURRENT_STAGE++;
                                    proceedStage();
                                } else {
                                    failed();
                                }
                            }).run();
                        } else {
                            failed();
                        }
                    });
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    if (state == Room101Client.FAILED_SERVER_ERROR) {
                        failed(Room101Client.getFailureMessage(activity, statusCode));
                    } else {
                        failed();
                    }
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        });
    }
    private void loadTimeEndPick() {
        thread.run(() -> {
            log.v(TAG, "loadTimeEndPick");
            callback.onDraw(getLoadingLayout(activity.getString(R.string.data_handling)));
            data = null;
            pick_time_end = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "getWindowEnd");
            params.put("dateRequest", pick_date);
            params.put("timeBegin", pick_time_start);
            params.put("timeEnd", "");
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.run(() -> {
                        if (statusCode == 200) {
                            new Room101TimeEndPickParse(response, json -> {
                                if (json != null) {
                                    data = json;
                                    CURRENT_STAGE++;
                                    proceedStage();
                                } else {
                                    failed();
                                }
                            }).run();
                        } else {
                            failed();
                        }
                    });
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    if (state == Room101Client.FAILED_SERVER_ERROR) {
                        failed(Room101Client.getFailureMessage(activity, statusCode));
                    } else {
                        failed();
                    }
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        });
    }
    private void loadConfirmation() {
        thread.run(() -> {
            log.v(TAG, "loadConfirmation");
            data = null;
            CURRENT_STAGE++;
            proceedStage();
        });
    }
    private void create() {
        thread.run(() -> {
            log.v(TAG, "create");
            callback.onDraw(getLoadingLayout(activity.getString(R.string.add_request)));
            data = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "saveRequest");
            params.put("dateRequest", pick_date);
            params.put("timeBegin", pick_time_start);
            params.put("timeEnd", pick_time_end);
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    try {
                        data = new JSONObject();
                        data.put("done", statusCode == 302);
                        CURRENT_STAGE++;
                        proceedStage();
                    } catch (JSONException e) {
                        log.exception(e);
                        failed();
                    }
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    try {
                        data = new JSONObject();
                        data.put("done", false);
                        data.put("message", state == Room101Client.FAILED_SERVER_ERROR ? Room101Client.getFailureMessage(activity, statusCode) : activity.getString(R.string.request_denied));
                        CURRENT_STAGE++;
                        proceedStage();
                    } catch (JSONException e) {
                        log.exception(e);
                        failed();
                    }
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        });
    }

    private void datePick() {
        thread.run(() -> {
            log.v(TAG, "datePick | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
            try {
                if (data == null) throw new NullPointerException("data cannot be null");
                if (!"date_pick".equals(data.getString("type"))) throw new Exception("Wrong data.type. Expected 'date_pick', got '" + data.getString("type") + "'");
                if (!data.has("data")) throw new Exception("Empty data.data");
                final JSONArray date_pick = data.getJSONArray("data");
                if (date_pick.length() > 0) {
                    callback.onDraw(getChooserLayout(activity.getString(R.string.peek_date), null, date_pick, (buttonView, isChecked) -> {
                        if (isChecked) {
                            try {
                                pick_date = buttonView.getText().toString().trim();
                            } catch (Exception e) {
                                log.exception(e);
                                failed();
                            }
                        }
                    }));
                } else {
                    callback.onDraw(getEmptyLayout(activity.getString(R.string.no_date_to_peek)));
                }
            } catch (Exception e){
                log.exception(e);
                failed();
            }
        });
    }
    private void timeStartPick() {
        thread.run(() -> {
            log.v(TAG, "timeStartPick | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
            try {
                if (data == null) throw new NullPointerException("data cannot be null");
                if (!"time_start_pick".equals(data.getString("type"))) throw new Exception("Wrong data.type. Expected 'time_start_pick', got '" + data.getString("type") + "'");
                if (!data.has("data")) throw new Exception("Empty data.data");
                final JSONArray time_pick = data.getJSONArray("data");
                if (time_pick.length() > 0) {
                    callback.onDraw(getChooserLayout(activity.getString(R.string.peek_time_start), null, time_pick, (buttonView, isChecked) -> {
                        if (isChecked) {
                            try {
                                String value = buttonView.getText().toString().trim();
                                Matcher m = timePickerPattern.matcher(value);
                                if (m.find()) {
                                    value = m.group(1);
                                }
                                pick_time_start = value + ":00";
                            } catch (Exception e) {
                                log.exception(e);
                                failed();
                            }
                        }
                    }));
                } else {
                    callback.onDraw(getEmptyLayout(activity.getString(R.string.no_time_to_peek)));
                }
            } catch (Exception e){
                log.exception(e);
                failed();
            }
        });
    }
    private void timeEndPick() {
        thread.run(() -> {
            log.v(TAG, "timeEndPick | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
            try {
                if (data == null) throw new NullPointerException("data cannot be null");
                if (!"time_end_pick".equals(data.getString("type"))) throw new Exception("Wrong data.type. Expected 'time_end_pick', got '" + data.getString("type") + "'");
                if (!data.has("data")) throw new Exception("Empty data.data");
                final JSONArray time_pick = data.getJSONArray("data");
                if (time_pick.length() > 0) {
                    callback.onDraw(getChooserLayout(activity.getString(R.string.peek_time_end), activity.getString(R.string.peek_time_end_desc), time_pick, (buttonView, isChecked) -> {
                        if (isChecked) {
                            try {
                                String value = buttonView.getText().toString().trim();
                                Matcher m = timePickerPattern.matcher(value);
                                if (m.find()) {
                                    value = m.group(1);
                                }
                                pick_time_end = value + ":00";
                            } catch (Exception e) {
                                log.exception(e);
                                failed();
                            }
                        }
                    }));
                } else {
                    callback.onDraw(getEmptyLayout(activity.getString(R.string.no_time_to_peek)));
                }
            } catch (Exception e){
                log.exception(e);
                failed();
            }
        });
    }
    private void confirmation() {
        thread.run(() -> {
            log.v(TAG, "confirmation | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
            try {
                if (pick_date == null) throw new NullPointerException("pick_date cannot be null");
                if (pick_time_start == null) throw new NullPointerException("pick_time_start cannot be null");
                if (pick_time_end == null) throw new NullPointerException("pick_time_end cannot be null");
                callback.onDraw(getChooserLayout(activity.getString(R.string.attention), activity.getString(R.string.room101_warning), null, null));
            } catch (Exception e){
                log.exception(e);
                failed();
            }
        });
    }
    private void done() {
        thread.run(() -> {
            log.v(TAG, "done | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
            try {
                if (data == null) throw new NullPointerException("data cannot be null");
                if (!data.has("done")) throw new Exception("Empty data.done");
                String message;
                if (data.getBoolean("done")) {
                    message = activity.getString(R.string.request_accepted);
                } else {
                    if (data.has("message")) {
                        message = data.getString("message");
                    } else {
                        message = activity.getString(R.string.request_denied);
                    }
                }
                callback.onDraw(getChooserLayout(message, null, null, null));
                if (data.getBoolean("done")) {
                    firebaseAnalyticsProvider.logEvent(
                            activity,
                            FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_ADDED,
                            firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.ROOM101_REQUEST_DETAILS, pick_date + "#" + pick_time_start + "#" + pick_time_end)
                    );
                }
            } catch (Exception e){
                log.exception(e);
                failed();
            }
        });
    }

    private void failed() {
        failed(activity.getString(R.string.error_occurred));
    }
    private void failed(String message) {
        log.v(TAG, "failed | " + message);
        notificationMessage.snackBar(activity, message);
        close(false);
    }

    private View getLoadingLayout(String text) {
        View view = inflate(R.layout.state_loading_text_compact);
        ((TextView) view.findViewById(R.id.loading_message)).setText(text);
        return view;
    }
    private View getEmptyLayout(String text) throws InflateException {
        View view = inflate(R.layout.state_nothing_to_display_compact);
        ((TextView) view.findViewById(R.id.ntd_text)).setText(text);
        return view;
    }
    private View getChooserLayout(String header, String desc, JSONArray array, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) throws Exception {
        View view = inflate(R.layout.layout_room101_add_request_state);
        if (view == null) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        if (pick_date != null || pick_time_start != null || pick_time_end != null) {
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_date, pick_date != null, activity.getString(R.string.session_date) + ": " + pick_date);
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_time_start, pick_time_start != null, activity.getString(R.string.time_start) + ": " + (pick_time_start == null ? "" : pick_time_start.replaceAll(":00$", "")));
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_time_end, pick_time_end != null, activity.getString(R.string.time_end) + ": " + (pick_time_end == null ? "" : pick_time_end.replaceAll(":00$", "")));
        } else {
            removeView(view, R.id.ars_request_info);
        }
        TextView ars_request_content_header = view.findViewById(R.id.ars_request_content_header);
        if (ars_request_content_header != null) {
            ars_request_content_header.setText(header);
        }
        if (array != null && array.length() > 0) {
            final RadioGroup radioGroup = view.findViewById(R.id.ars_request_chooser);
            final int textColor = Color.resolve(activity, android.R.attr.textColorPrimary);
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject session = array.getJSONObject(i);
                    String text = session.getString("time");
                    if (!session.getString("available").isEmpty()) {
                        text += " (" + activity.getString(R.string.room101_available) + ": " + session.getString("available") + ")";
                    }
                    RadioButton radioButton = new RadioButton(activity);
                    radioButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    radioButton.setText(text);
                    radioButton.setTextColor(textColor);
                    radioButton.setOnCheckedChangeListener(onCheckedChangeListener);
                    radioGroup.addView(radioButton);
                } catch (Exception e) {
                    log.exception(e);
                }
            }
        }
        if (desc != null && !desc.isEmpty()){
            ((TextView) view.findViewById(R.id.ars_request_desc)).setText(desc);
        } else {
            removeView(view, R.id.ars_request_desc);
        }
        if ((array == null || array.length() == 0) && (desc == null || desc.isEmpty())) {
            removeView(view, R.id.ars_request_content);
        }
        return view;
    }
    private void setRequestInfo(final ViewGroup viewGroup, final int layout, final boolean show, final String text) {
        thread.runOnUI(() -> {
            if (show) {
                ((TextView) viewGroup.findViewById(layout)).setText(text);
            } else {
                removeView(viewGroup, layout);
            }
        });
    }
    private void removeView(View view, int layout) {
        staticUtil.removeView(view.findViewById(layout));
    }
    private View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
