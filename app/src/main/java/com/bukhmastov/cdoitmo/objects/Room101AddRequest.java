package com.bukhmastov.cdoitmo.objects;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.Room101Fragment;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.parse.Room101DatePickParse;
import com.bukhmastov.cdoitmo.parse.Room101TimeEndPickParse;
import com.bukhmastov.cdoitmo.parse.Room101TimeStartPickParse;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class Room101AddRequest {

    private static final String TAG = "Room101AddRequest";
    public interface callback {
        void onProgress(int stage);
        void onDraw(View view);
        void onClose();
        void onDone();
    }
    private callback callback;
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
    private Activity context = null;

    private RequestHandle ARequestHandle = null;

    private JSONObject data = null;
    private String pick_date = null;
    private String pick_time_start = null;
    private String pick_time_end = null;

    public Room101AddRequest(Activity context, callback callback){
        this.callback = callback;
        this.context = context;
        proceedStage();
    }

    public void back(){
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
    public void forward(){
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
    public void close(boolean done){
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

    private void loadDatePick(int stage){
        if (stage == 0) {
            callback.onDraw(getLoadingLayout(context.getString(R.string.data_loading)));
            data = null;
            pick_date = null;
            Room101Fragment.execute(context, "newRequest", new Room101ClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new Room101DatePickParse(new Room101DatePickParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (json != null) {
                                    data = json;
                                    loadDatePick(1);
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
        } else if (stage == 1) {
            RequestParams params = new RequestParams();
            params.put("month", "next");
            params.put("login", Storage.get(context, "login"));
            params.put("password", Storage.get(context, "password"));
            Room101Client.post(context, "newRequest.php", params, new Room101ClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new Room101DatePickParse(new Room101DatePickParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (json != null) {
                                    if (data == null) {
                                        data = json;
                                    } else {
                                        try {
                                            JSONArray jsonArray = data.getJSONArray("data");
                                            JSONArray jsonArrayNew = json.getJSONArray("data");
                                            for (int i = 0; i < jsonArrayNew.length(); i++) jsonArray.put(jsonArrayNew.getString(i));
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
        } else {
            failed();
        }
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
        Room101Client.post(context, "newRequest.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new Room101TimeStartPickParse(new Room101TimeStartPickParse.response() {
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
        Room101Client.post(context, "newRequest.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new Room101TimeEndPickParse(new Room101TimeEndPickParse.response() {
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
        Room101Client.post(context, "newRequest.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
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
            public void onFailure(int state, int statusCode, Header[] headers) {
                if(statusCode == 302){
                    try {
                        data = new JSONObject();
                        data.put("done", true);
                        CURRENT_STAGE++;
                        proceedStage();
                    } catch (JSONException e) {
                        Static.error(e);
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
                                Static.error(e);
                                failed();
                            }
                        }
                    }
                }));
            } else {
                callback.onDraw(getEmptyLayout(context.getString(R.string.no_date_to_peek)));
            }
        } catch (Exception e){
            Static.error(e);
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
                                Static.error(e);
                                failed();
                            }
                        }
                    }
                }));
            } else {
                callback.onDraw(getEmptyLayout(context.getString(R.string.no_time_to_peek)));
            }
        } catch (Exception e){
            Static.error(e);
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
                                Static.error(e);
                                failed();
                            }
                        }
                    }
                }));
            } else {
                callback.onDraw(getEmptyLayout(context.getString(R.string.no_time_to_peek)));
            }
        } catch (Exception e){
            Static.error(e);
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
            Static.error(e);
            failed();
        }
    }
    private void done(){
        try {
            if (data == null) throw new NullPointerException("data cannot be null");
            if (!data.has("done")) throw new Exception("Empty data.done");
            callback.onDraw(getChooserLayout(data.getBoolean("done") ? context.getString(R.string.request_accepted) : context.getString(R.string.request_denied), "", null, null));
        } catch (Exception e){
            Static.error(e);
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
            context.getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, Static.typedValue, true);
            snackbar.getView().setBackgroundColor(Static.typedValue.data);
            snackbar.show();
        } catch (Exception e){
            Static.error(e);
        }
    }
    private LinearLayout getLoadingLayout(String text){
        LinearLayout loading = new LinearLayout(context);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        loading.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        progressBar.setPadding(0, (int) (16 * Static.destiny), 0, (int) (10 * Static.destiny));
        loading.addView(progressBar);
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setTextColor(Static.textColorPrimary);
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
        textView.setTextColor(Static.textColorPrimary);
        textView.setPadding(0, (int) (24 * Static.destiny), 0, (int) (24 * Static.destiny));
        empty.addView(textView);
        return empty;
    }
    private LinearLayout getChooserLayout(String header, String desc, JSONArray array, CompoundButton.OnCheckedChangeListener onCheckedChangeListener){
        LinearLayout chooser = new LinearLayout(context);
        chooser.setOrientation(LinearLayout.VERTICAL);
        chooser.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        chooser.setPadding((int) (16 * Static.destiny), (int) (8 * Static.destiny), (int) (16 * Static.destiny), (int) (8 * Static.destiny));
        try {
            if(pick_date != null || pick_time_start != null || pick_time_end != null){
                LinearLayout requestLayout = new LinearLayout(context);
                requestLayout.setOrientation(LinearLayout.VERTICAL);
                requestLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                requestLayout.setPadding(0, (int) (8 * Static.destiny), 0, (int) (8 * Static.destiny));
                TextView textView = new TextView(context);
                textView.setText(context.getString(R.string.request) + ":");
                textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                textView.setTextColor(Static.textColorPrimary);
                requestLayout.addView(textView);
                if(pick_date != null){
                    textView = new TextView(context);
                    textView.setText(context.getString(R.string.session_date) + ":" + " " + pick_date);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(Static.textColorSecondary);
                    requestLayout.addView(textView);
                }
                if(pick_time_start != null){
                    textView = new TextView(context);
                    textView.setText(context.getString(R.string.time_start) + ":" + " " + pick_time_start.replaceAll(":00$", ""));
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(Static.textColorSecondary);
                    requestLayout.addView(textView);
                }
                if(pick_time_end != null){
                    textView = new TextView(context);
                    textView.setText(context.getString(R.string.time_end) + ":" + " " + pick_time_end.replaceAll(":00$", ""));
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(Static.textColorSecondary);
                    requestLayout.addView(textView);
                }
                chooser.addView(requestLayout);
            }
            TextView textView = new TextView(context);
            textView.setText(header);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(Static.textColorPrimary);
            textView.setPadding(0, (int) (8 * Static.destiny), 0, (int) (8 * Static.destiny));
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
                descView.setTextColor(Static.textColorSecondary);
                descView.setPadding(0, (int) (8 * Static.destiny), 0, (int) (8 * Static.destiny));
                chooser.addView(descView);
            }
        } catch (Exception e){
            Static.error(e);
            failed();
        }
        return chooser;
    }

}