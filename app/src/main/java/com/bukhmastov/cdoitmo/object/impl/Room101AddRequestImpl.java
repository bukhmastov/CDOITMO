package com.bukhmastov.cdoitmo.object.impl;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
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
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.Room101FragmentPresenter;
import com.bukhmastov.cdoitmo.model.parser.Room101DatePickParser;
import com.bukhmastov.cdoitmo.model.parser.Room101TimeEndPickParser;
import com.bukhmastov.cdoitmo.model.parser.Room101TimeStartPickParser;
import com.bukhmastov.cdoitmo.model.room101.request.ROption;
import com.bukhmastov.cdoitmo.model.room101.request.Room101Request;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.Room101AddRequest;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.Lazy;

public class Room101AddRequestImpl implements Room101AddRequest {

    private static final String TAG = "Room101AddRequest";
    private Activity activity = null;
    private Callback callback = null;
    private Pattern timePickerPattern;
    private int currentStage = 0;
    private Client.Request requestHandle = null;
    private Room101Request data = null;
    private String pickDate = null;
    private String pickTimeStart = null;
    private String pickTimeEnd = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    Lazy<Room101FragmentPresenter> room101FragmentPresenter; // !! presenter without setFragment call | only presenter.execute() method available
    @Inject
    Room101Client room101Client;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Static staticUtil;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public Room101AddRequestImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void start(@NonNull Activity activity, @NonNull Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.timePickerPattern = Pattern.compile("^(\\d{1,2}:\\d{2})\\s?(\\((" + activity.getString(R.string.room101_available) + ":\\s)?(\\d*)\\))?$");
        reset();
        log.v(TAG, "start");
        proceedStage();
    }

    @Override
    public void back() {
        log.v(TAG, "back");
        switch (currentStage) {
            case STAGE_PICK_DATE_LOAD:
            case STAGE_PICK_TIME_START_LOAD:
            case STAGE_PICK_TIME_END_LOAD:
            case STAGE_PICK_CONFIRMATION_LOAD:
            case STAGE_PICK_CREATE:
            case STAGE_PICK_DONE:
                return;
        }
        currentStage -= 3;
        data = null;
        if (currentStage < 0) {
            close(false);
        } else {
            proceedStage();
        }
    }

    @Override
    public void forward() {
        log.v(TAG, "forward");
        switch (currentStage) {
            case STAGE_PICK_DATE_LOAD:
            case STAGE_PICK_TIME_START_LOAD:
            case STAGE_PICK_TIME_END_LOAD:
            case STAGE_PICK_CONFIRMATION_LOAD:
            case STAGE_PICK_CREATE:
                return;
        }
        switch (currentStage) {
            case STAGE_PICK_DATE:
                if (pickDate == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_peek_date));
                    return;
                }
                break;
            case STAGE_PICK_TIME_START:
                if (pickTimeStart == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_peek_time_start));
                    return;
                }
                break;
            case STAGE_PICK_TIME_END:
                if (pickTimeEnd == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_peek_time_end));
                    return;
                }
                break;
            case STAGE_PICK_DONE:
                close(true);
                return;
        }
        currentStage++;
        proceedStage();
    }

    @Override
    public void close(boolean done) {
        log.v(TAG, "close | done=", done);
        if (requestHandle != null) {
            requestHandle.cancel();
        }
        if (done) {
            callback.onDone();
        } else {
            callback.onClose();
        }
    }

    @Override
    public void reset() {
        log.v(TAG, "reset");
        currentStage = 0;
        if (requestHandle != null) {
            requestHandle.cancel();
        }
        requestHandle = null;
        data = null;
        pickDate = null;
        pickTimeStart = null;
        pickTimeEnd = null;
    }

    private void proceedStage() {
        log.v(TAG, "proceedStage | currentStage=", currentStage);
        callback.onProgress(currentStage);
        switch (currentStage){
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
            log.v(TAG, "loadDatePick | stage=", stage);
            if (stage == 0) {
                callback.onDraw(getLoadingLayout(activity.getString(R.string.data_loading)));
                data = null;
                pickDate = null;
                room101FragmentPresenter.get().execute(activity, "newRequest", new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        thread.run(() -> {
                            if (statusCode == 200) {
                                Room101Request room101Request = new Room101DatePickParser(response).parse();
                                if (room101Request != null) {
                                    data = room101Request;
                                    loadDatePick(1);
                                    return;
                                }
                            }
                            failed();
                        }, throwable -> {
                            failed(throwable);
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
                                Room101Request room101Request = new Room101DatePickParser(response).parse();
                                if (room101Request != null) {
                                    if (data == null) {
                                        data = room101Request;
                                    } else {
                                        data.getOptions().addAll(room101Request.getOptions());
                                    }
                                }
                                currentStage++;
                                proceedStage();
                                return;
                            }
                            failed();
                        }, throwable -> {
                            failed(throwable);
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
        }, throwable -> {
            failed(throwable);
        });
    }
    private void loadTimeStartPick() {
        thread.run(() -> {
            log.v(TAG, "loadTimeStartPick");
            callback.onDraw(getLoadingLayout(activity.getString(R.string.data_handling)));
            data = null;
            pickTimeStart = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "getWindowBegin");
            params.put("dateRequest", pickDate);
            params.put("timeBegin", "");
            params.put("timeEnd", "");
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.run(() -> {
                        if (statusCode == 200) {
                            Room101Request room101Request = new Room101TimeStartPickParser(response).parse();
                            if (room101Request != null) {
                                data = room101Request;
                                currentStage++;
                                proceedStage();
                                return;
                            }
                        }
                        failed();
                    }, throwable -> {
                        failed(throwable);
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
        }, throwable -> {
            failed(throwable);
        });
    }
    private void loadTimeEndPick() {
        thread.run(() -> {
            log.v(TAG, "loadTimeEndPick");
            callback.onDraw(getLoadingLayout(activity.getString(R.string.data_handling)));
            data = null;
            pickTimeEnd = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "getWindowEnd");
            params.put("dateRequest", pickDate);
            params.put("timeBegin", pickTimeStart);
            params.put("timeEnd", "");
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.run(() -> {
                        if (statusCode == 200) {
                            Room101Request room101Request = new Room101TimeEndPickParser(response).parse();
                            if (room101Request != null) {
                                data = room101Request;
                                currentStage++;
                                proceedStage();
                            }
                            return;
                        }
                        failed();
                    }, throwable -> {
                        failed(throwable);
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
        }, throwable -> {
            failed(throwable);
        });
    }
    private void loadConfirmation() {
        thread.run(() -> {
            log.v(TAG, "loadConfirmation");
            data = null;
            currentStage++;
            proceedStage();
        }, throwable -> {
            failed(throwable);
        });
    }
    private void create() {
        thread.run(() -> {
            log.v(TAG, "create");
            callback.onDraw(getLoadingLayout(activity.getString(R.string.add_request)));
            data = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "saveRequest");
            params.put("dateRequest", pickDate);
            params.put("timeBegin", pickTimeStart);
            params.put("timeEnd", pickTimeEnd);
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "newRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    data = new Room101Request();
                    data.setDone(statusCode == 302);
                    currentStage++;
                    proceedStage();
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    data = new Room101Request();
                    data.setDone(false);
                    data.setMessage(state == Room101Client.FAILED_SERVER_ERROR ? Room101Client.getFailureMessage(activity, statusCode) : activity.getString(R.string.request_denied));
                    currentStage++;
                    proceedStage();
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        }, throwable -> {
            failed(throwable);
        });
    }

    private void datePick() {
        thread.run(() -> {
            log.v(TAG, "datePick | pickDate=", pickDate, " | pickTimeStart=", pickTimeStart, " | pickTimeEnd=", pickTimeEnd);
            if (data == null) {
                throw new NullPointerException("Data cannot be null");
            }
            if (!"date_pick".equals(data.getType())) {
                throw new Exception("Wrong data.type, expected 'date_pick', got '" + data.getType() + "'");
            }
            if (data.getOptions().size() == 0) {
                callback.onDraw(getEmptyLayout(activity.getString(R.string.no_date_to_peek)));
                return;
            }
            callback.onDraw(getChooserLayout(activity.getString(R.string.peek_date), null, data.getOptions(), (buttonView, isChecked) -> {
                if (isChecked) {
                    try {
                        pickDate = buttonView.getText().toString().trim();
                    } catch (Exception e) {
                        log.exception(e);
                        failed();
                    }
                }
            }));
        }, throwable -> {
            log.exception(throwable);
            failed(throwable);
        });
    }
    private void timeStartPick() {
        thread.run(() -> {
            log.v(TAG, "timeStartPick | pickDate=", pickDate, " | pickTimeStart=", pickTimeStart, " | pickTimeEnd=", pickTimeEnd);
            if (data == null) {
                throw new NullPointerException("Data cannot be null");
            }
            if (!"time_start_pick".equals(data.getType())) {
                throw new Exception("Wrong data.type, expected 'time_start_pick', got '" + data.getType() + "'");
            }
            if (data.getOptions().size() == 0) {
                callback.onDraw(getEmptyLayout(activity.getString(R.string.no_time_to_peek)));
                return;
            }
            callback.onDraw(getChooserLayout(activity.getString(R.string.peek_time_start), null, data.getOptions(), (buttonView, isChecked) -> {
                if (isChecked) {
                    try {
                        String value = buttonView.getText().toString().trim();
                        Matcher m = timePickerPattern.matcher(value);
                        if (m.find()) {
                            value = m.group(1);
                        }
                        pickTimeStart = value + ":00";
                    } catch (Exception e) {
                        log.exception(e);
                        failed();
                    }
                }
            }));
        }, throwable -> {
            log.exception(throwable);
            failed(throwable);
        });
    }
    private void timeEndPick() {
        thread.run(() -> {
            log.v(TAG, "timeEndPick | pickDate=", pickDate, " | pickTimeStart=", pickTimeStart, " | pickTimeEnd=", pickTimeEnd);
            if (data == null) {
                throw new NullPointerException("Data cannot be null");
            }
            if (!"time_end_pick".equals(data.getType())) {
                throw new Exception("Wrong data.type, expected 'time_end_pick', got '" + data.getType() + "'");
            }
            if (data.getOptions().size() == 0) {
                callback.onDraw(getEmptyLayout(activity.getString(R.string.no_time_to_peek)));
                return;
            }
            callback.onDraw(getChooserLayout(activity.getString(R.string.peek_time_end), activity.getString(R.string.peek_time_end_desc), data.getOptions(), (buttonView, isChecked) -> {
                if (isChecked) {
                    try {
                        String value = buttonView.getText().toString().trim();
                        Matcher m = timePickerPattern.matcher(value);
                        if (m.find()) {
                            value = m.group(1);
                        }
                        pickTimeEnd = value + ":00";
                    } catch (Exception e) {
                        log.exception(e);
                        failed();
                    }
                }
            }));
        }, throwable -> {
            log.exception(throwable);
            failed(throwable);
        });
    }
    private void confirmation() {
        thread.run(() -> {
            log.v(TAG, "confirmation | pickDate=", pickDate, " | pickTimeStart=", pickTimeStart, " | pickTimeEnd=", pickTimeEnd);
            if (pickDate == null) {
                throw new NullPointerException("pickDate cannot be null");
            }
            if (pickTimeStart == null) {
                throw new NullPointerException("pickTimeStart cannot be null");
            }
            if (pickTimeEnd == null) {
                throw new NullPointerException("pickTimeEnd cannot be null");
            }
            callback.onDraw(getChooserLayout(activity.getString(R.string.attention), activity.getString(R.string.room101_warning), null, null));
        }, throwable -> {
            log.exception(throwable);
            failed(throwable);
        });
    }
    private void done() {
        thread.run(() -> {
            log.v(TAG, "done | pickDate=" + pickDate + " | pickTimeStart=" + pickTimeStart + " | pickTimeEnd=" + pickTimeEnd);
            if (data == null) {
                throw new NullPointerException("Data cannot be null");
            }
            String message;
            if (data.isDone()) {
                message = activity.getString(R.string.request_accepted);
            } else if (StringUtils.isNotBlank(data.getMessage())) {
                message = data.getMessage();
            } else {
                message = activity.getString(R.string.request_denied);
            }
            callback.onDraw(getChooserLayout(message, null, null, null));
            if (data.isDone()) {
                firebaseAnalyticsProvider.logEvent(
                        activity,
                        FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_ADDED,
                        firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.ROOM101_REQUEST_DETAILS, pickDate + "#" + pickTimeStart + "#" + pickTimeEnd)
                );
            }
        }, throwable -> {
            log.exception(throwable);
            failed(throwable);
        });
    }

    private void failed() {
        failed(activity.getString(R.string.error_occurred));
    }
    private void failed(Throwable throwable) {
        log.e(TAG, "failed | throwable=", throwable.getMessage());
        failed(activity.getString(R.string.error_occurred));
    }
    private void failed(String message) {
        log.v(TAG, "failed | message=" + message);
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
    private View getChooserLayout(String header, String desc, Collection<ROption> options, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        View view = inflate(R.layout.layout_room101_add_request_state);
        if (view == null) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        if (pickDate != null || pickTimeStart != null || pickTimeEnd != null) {
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_date, pickDate != null, activity.getString(R.string.session_date) + ": " + pickDate);
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_time_start, pickTimeStart != null, activity.getString(R.string.time_start) + ": " + (pickTimeStart == null ? "" : pickTimeStart.replaceAll(":00$", "")));
            setRequestInfo(viewGroup, R.id.ars_request_info_pick_time_end, pickTimeEnd != null, activity.getString(R.string.time_end) + ": " + (pickTimeEnd == null ? "" : pickTimeEnd.replaceAll(":00$", "")));
        } else {
            removeView(view, R.id.ars_request_info);
        }
        TextView headerView = view.findViewById(R.id.ars_request_content_header);
        if (headerView != null) {
            headerView.setText(header);
        }
        if (CollectionUtils.isNotEmpty(options)) {
            RadioGroup radioGroup = view.findViewById(R.id.ars_request_chooser);
            int textColor = Color.resolve(activity, android.R.attr.textColorPrimary);
            for (ROption option : options) {
                String text = option.getTime();
                if (StringUtils.isNotBlank(option.getAvailable())) {
                    text += " (" + activity.getString(R.string.room101_available) + ": " + option.getAvailable() + ")";
                }
                RadioButton radioButton = new RadioButton(activity);
                radioButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                radioButton.setText(text);
                radioButton.setTextColor(textColor);
                radioButton.setOnCheckedChangeListener(onCheckedChangeListener);
                radioGroup.addView(radioButton);
            }
        }
        if (StringUtils.isNotBlank(desc)){
            ((TextView) view.findViewById(R.id.ars_request_desc)).setText(desc);
        } else {
            removeView(view, R.id.ars_request_desc);
        }
        if (CollectionUtils.isEmpty(options) && StringUtils.isBlank(desc)) {
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
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(layout, null);
    }
}
