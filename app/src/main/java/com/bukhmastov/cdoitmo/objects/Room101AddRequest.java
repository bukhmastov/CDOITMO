package com.bukhmastov.cdoitmo.objects;

import android.app.Activity;
import android.content.Context;
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
import com.bukhmastov.cdoitmo.fragments.Room101Fragment;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.parse.Room101DatePickParse;
import com.bukhmastov.cdoitmo.parse.Room101TimeEndPickParse;
import com.bukhmastov.cdoitmo.parse.Room101TimeStartPickParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101AddRequest {

    private static final String TAG = "Room101AddRequest";
    public interface callback {
        void onProgress(int stage);
        void onDraw(View view);
        void onClose();
        void onDone();
    }
    private final callback callback;
    public static final int STAGE_PICK_DATE_LOAD = 0;
    public static final int STAGE_PICK_DATE = 1;
    public static final int STAGE_PICK_TIME_START_LOAD = 2;
    public static final int STAGE_PICK_TIME_START = 3;
    public static final int STAGE_PICK_TIME_END_LOAD = 4;
    public static final int STAGE_PICK_TIME_END = 5;
    public static final int STAGE_PICK_CONFIRMATION_LOAD = 6;
    public static final int STAGE_PICK_CONFIRMATION = 7;
    public static final int STAGE_PICK_CREATE = 8;
    public static final int STAGE_PICK_DONE = 9;
    public static final int STAGES_COUNT = 9;
    private int CURRENT_STAGE = 0;
    private Activity activity = null;
    private final Pattern timePickerPattern = Pattern.compile("^(\\d{1,2}:\\d{2})\\s?(\\((Свободных мест:\\s)?(\\d*)\\))?$");
    private Client.Request requestHandle = null;

    private JSONObject data = null;
    private String pick_date = null;
    private String pick_time_start = null;
    private String pick_time_end = null;

    public Room101AddRequest(Activity activity, callback callback) {
        this.callback = callback;
        this.activity = activity;
        proceedStage();
    }

    public void back() {
        Log.v(TAG, "back");
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
    public void forward() {
        Log.v(TAG, "forward");
        switch (CURRENT_STAGE) {
            case STAGE_PICK_DATE_LOAD:
            case STAGE_PICK_TIME_START_LOAD:
            case STAGE_PICK_TIME_END_LOAD:
            case STAGE_PICK_CONFIRMATION_LOAD:
            case STAGE_PICK_CREATE:
                return;
        }
        switch (CURRENT_STAGE) {
            case STAGE_PICK_DATE: if(pick_date == null){ Static.snackBar(activity, activity.getString(R.string.need_to_peek_date)); return; } break;
            case STAGE_PICK_TIME_START: if(pick_time_start == null){ Static.snackBar(activity, activity.getString(R.string.need_to_peek_time_start)); return; } break;
            case STAGE_PICK_TIME_END: if(pick_time_end == null){ Static.snackBar(activity, activity.getString(R.string.need_to_peek_time_end)); return; } break;
            case STAGE_PICK_DONE: close(true); return;
        }
        CURRENT_STAGE++;
        proceedStage();
    }
    public void close(boolean done) {
        Log.v(TAG, "close | done=" + (done ? "true" : "false"));
        if (requestHandle != null) requestHandle.cancel();
        if (done) {
            callback.onDone();
        } else {
            callback.onClose();
        }
    }
    private void proceedStage() {
        Log.v(TAG, "proceedStage | CURRENT_STAGE=" + CURRENT_STAGE);
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadDatePick | stage=" + stage);
                if (stage == 0) {
                    callback.onDraw(getLoadingLayout(activity.getString(R.string.data_loading)));
                    data = null;
                    pick_date = null;
                    Room101Fragment.execute(activity, "newRequest", new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (statusCode == 200) {
                                        new Room101DatePickParse(response, new Room101DatePickParse.response() {
                                            @Override
                                            public void finish(JSONObject json) {
                                                if (json != null) {
                                                    data = json;
                                                    loadDatePick(1);
                                                } else {
                                                    failed();
                                                }
                                            }
                                        }).run();
                                    } else {
                                        failed();
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            failed();
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
                    params.put("login", Storage.file.perm.get(activity, "user#deifmo#login"));
                    params.put("password", Storage.file.perm.get(activity, "user#deifmo#password"));
                    Room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (statusCode == 200) {
                                        new Room101DatePickParse(response, new Room101DatePickParse.response() {
                                            @Override
                                            public void finish(JSONObject json) {
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
                                                            Static.error(e);
                                                            data = json;
                                                        }
                                                    }
                                                }
                                                CURRENT_STAGE++;
                                                proceedStage();
                                            }
                                        }).run();
                                    } else {
                                        failed();
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            failed();
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
            }
        });
    }
    private void loadTimeStartPick() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadTimeStartPick");
                callback.onDraw(getLoadingLayout(activity.getString(R.string.data_handling)));
                data = null;
                pick_time_start = null;
                HashMap<String, String> params = new HashMap<>();
                params.put("getFunc", "getWindowBegin");
                params.put("dateRequest", pick_date);
                params.put("timeBegin", "");
                params.put("timeEnd", "");
                params.put("login", Storage.file.perm.get(activity, "user#deifmo#login"));
                params.put("password", Storage.file.perm.get(activity, "user#deifmo#password"));
                Room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                if (statusCode == 200) {
                                    new Room101TimeStartPickParse(response, new Room101TimeStartPickParse.response() {
                                        @Override
                                        public void finish(JSONObject json) {
                                            if (json != null) {
                                                data = json;
                                                CURRENT_STAGE++;
                                                proceedStage();
                                            } else {
                                                failed();
                                            }
                                        }
                                    }).run();
                                } else {
                                    failed();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        failed();
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            }
        });
    }
    private void loadTimeEndPick() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadTimeEndPick");
                callback.onDraw(getLoadingLayout(activity.getString(R.string.data_handling)));
                data = null;
                pick_time_end = null;
                HashMap<String, String> params = new HashMap<>();
                params.put("getFunc", "getWindowEnd");
                params.put("dateRequest", pick_date);
                params.put("timeBegin", pick_time_start);
                params.put("timeEnd", "");
                params.put("login", Storage.file.perm.get(activity, "user#deifmo#login"));
                params.put("password", Storage.file.perm.get(activity, "user#deifmo#password"));
                Room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                if (statusCode == 200) {
                                    new Room101TimeEndPickParse(response, new Room101TimeEndPickParse.response() {
                                        @Override
                                        public void finish(JSONObject json) {
                                            if (json != null) {
                                                data = json;
                                                CURRENT_STAGE++;
                                                proceedStage();
                                            } else {
                                                failed();
                                            }
                                        }
                                    }).run();
                                } else {
                                    failed();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        failed();
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            }
        });
    }
    private void loadConfirmation() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadConfirmation");
                data = null;
                CURRENT_STAGE++;
                proceedStage();
            }
        });
    }
    private void create() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "create");
                callback.onDraw(getLoadingLayout(activity.getString(R.string.add_request)));
                data = null;
                HashMap<String, String> params = new HashMap<>();
                params.put("getFunc", "saveRequest");
                params.put("dateRequest", pick_date);
                params.put("timeBegin", pick_time_start);
                params.put("timeEnd", pick_time_end);
                params.put("login", Storage.file.perm.get(activity, "user#deifmo#login"));
                params.put("password", Storage.file.perm.get(activity, "user#deifmo#password"));
                Room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Client.Headers headers, String response) {
                        try {
                            data = new JSONObject();
                            data.put("done", statusCode == 302);
                            CURRENT_STAGE++;
                            proceedStage();
                        } catch (JSONException e) {
                            Static.error(e);
                            failed();
                        }
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        try {
                            data = new JSONObject();
                            data.put("done", false);
                            CURRENT_STAGE++;
                            proceedStage();
                        } catch (JSONException e) {
                            Static.error(e);
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
            }
        });
    }

    private void datePick() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "datePick | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
                try {
                    if (data == null) throw new NullPointerException("data cannot be null");
                    if (!Objects.equals(data.getString("type"), "date_pick")) throw new Exception("Wrong data.type. Expected 'date_pick', got '" + data.getString("type") + "'");
                    if (!data.has("data")) throw new Exception("Empty data.data");
                    final JSONArray date_pick = data.getJSONArray("data");
                    if (date_pick.length() > 0) {
                        callback.onDraw(getChooserLayout(activity.getString(R.string.peek_date), null, date_pick, new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    try {
                                        pick_date = buttonView.getText().toString().trim();
                                    } catch (Exception e) {
                                        Static.error(e);
                                        failed();
                                    }
                                }
                            }
                        }));
                    } else {
                        callback.onDraw(getEmptyLayout(activity.getString(R.string.no_date_to_peek)));
                    }
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }
    private void timeStartPick() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "timeStartPick | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
                try {
                    if (data == null) throw new NullPointerException("data cannot be null");
                    if (!Objects.equals(data.getString("type"), "time_start_pick")) throw new Exception("Wrong data.type. Expected 'time_start_pick', got '" + data.getString("type") + "'");
                    if (!data.has("data")) throw new Exception("Empty data.data");
                    final JSONArray time_pick = data.getJSONArray("data");
                    if (time_pick.length() > 0) {
                        callback.onDraw(getChooserLayout(activity.getString(R.string.peek_time_start), null, time_pick, new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    try {
                                        String value = buttonView.getText().toString().trim();
                                        Matcher m = timePickerPattern.matcher(value);
                                        if (m.find()) {
                                            value = m.group(1);
                                        }
                                        pick_time_start = value + ":00";
                                    } catch (Exception e) {
                                        Static.error(e);
                                        failed();
                                    }
                                }
                            }
                        }));
                    } else {
                        callback.onDraw(getEmptyLayout(activity.getString(R.string.no_time_to_peek)));
                    }
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }
    private void timeEndPick() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "timeEndPick | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
                try {
                    if (data == null) throw new NullPointerException("data cannot be null");
                    if (!Objects.equals(data.getString("type"), "time_end_pick")) throw new Exception("Wrong data.type. Expected 'time_end_pick', got '" + data.getString("type") + "'");
                    if (!data.has("data")) throw new Exception("Empty data.data");
                    final JSONArray time_pick = data.getJSONArray("data");
                    if (time_pick.length() > 0) {
                        callback.onDraw(getChooserLayout(activity.getString(R.string.peek_time_end), activity.getString(R.string.peek_time_end_desc), time_pick, new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    try {
                                        String value = buttonView.getText().toString().trim();
                                        Matcher m = timePickerPattern.matcher(value);
                                        if (m.find()) {
                                            value = m.group(1);
                                        }
                                        pick_time_end = value + ":00";
                                    } catch (Exception e) {
                                        Static.error(e);
                                        failed();
                                    }
                                }
                            }
                        }));
                    } else {
                        callback.onDraw(getEmptyLayout(activity.getString(R.string.no_time_to_peek)));
                    }
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }
    private void confirmation() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "confirmation | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
                try {
                    if (pick_date == null) throw new NullPointerException("pick_date cannot be null");
                    if (pick_time_start == null) throw new NullPointerException("pick_time_start cannot be null");
                    if (pick_time_end == null) throw new NullPointerException("pick_time_end cannot be null");
                    callback.onDraw(getChooserLayout(activity.getString(R.string.attention), activity.getString(R.string.room101_warning), null, null));
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }
    private void done() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "done | pick_date=" + pick_date + " | pick_time_start=" + pick_time_start + " | pick_time_end=" + pick_time_end);
                try {
                    if (data == null) throw new NullPointerException("data cannot be null");
                    if (!data.has("done")) throw new Exception("Empty data.done");
                    callback.onDraw(getChooserLayout(data.getBoolean("done") ? activity.getString(R.string.request_accepted) : activity.getString(R.string.request_denied), null, null, null));
                    if (data.getBoolean("done")) {
                        FirebaseAnalyticsProvider.logEvent(
                                activity,
                                FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_ADDED,
                                FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.ROOM101_REQUEST_DETAILS, pick_date + "#" + pick_time_start + "#" + pick_time_end)
                        );
                    }
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }

    private void failed() {
        Log.v(TAG, "failed");
        Static.snackBar(activity, activity.getString(R.string.error_occurred));
        close(false);
    }

    private View getLoadingLayout(String text) {
        View view = inflate(R.layout.state_loading_without_align);
        ((TextView) view.findViewById(R.id.loading_message)).setText(text);
        return view;
    }
    private View getEmptyLayout(String text) throws InflateException {
        View view = inflate(R.layout.nothing_to_display);
        ((TextView) view.findViewById(R.id.ntd_text)).setText(text);
        return view;
    }
    private View getChooserLayout(String header, String desc, JSONArray array, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) throws Exception {
        View view = inflate(R.layout.layout_room101_add_request_state);
        ViewGroup viewGroup = (ViewGroup) view;
        if (pick_date != null || pick_time_start != null || pick_time_end != null) {
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_date, pick_date != null, activity.getString(R.string.session_date) + ": " + pick_date);
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_time_start, pick_time_start != null, activity.getString(R.string.time_start) + ": " + (pick_time_start == null ? "" : pick_time_start.replaceAll(":00$", "")));
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_time_end, pick_time_end != null, activity.getString(R.string.time_end) + ": " + (pick_time_end == null ? "" : pick_time_end.replaceAll(":00$", "")));
        } else {
            removeView(view, R.id.ars_request_info);
        }
        ((TextView) view.findViewById(R.id.ars_request_content_header)).setText(header);
        if (array != null && array.length() > 0) {
            RadioGroup radioGroup = view.findViewById(R.id.ars_request_chooser);
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject session = array.getJSONObject(i);
                    String text = session.getString("time");
                    if (!Objects.equals(session.getString("available"), "")) {
                        text += " (" + "Свободных мест" + ": " + session.getString("available") + ")";
                    }
                    RadioButton radioButton = new RadioButton(activity);
                    radioButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    radioButton.setText(text);
                    radioButton.setOnCheckedChangeListener(onCheckedChangeListener);
                    radioGroup.addView(radioButton);
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        }
        if (desc != null && !Objects.equals(desc, "")){
            ((TextView) view.findViewById(R.id.ars_request_desc)).setText(desc);
        } else {
            removeView(view, R.id.ars_request_desc);
        }
        if ((array == null || array.length() == 0) && (desc == null || Objects.equals(desc, ""))) {
            removeView(view, R.id.ars_request_content);
        }
        return view;
    }
    private void setRequestInfo(final ViewGroup viewGroup, final int layout, final boolean show, final String text) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    ((TextView) viewGroup.findViewById(layout)).setText(text);
                } else {
                    removeView(viewGroup, layout);
                }
            }
        });
    }
    private void removeView(View view, int layout) {
        Static.removeView(view.findViewById(layout));
    }
    private View inflate(int layout) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
