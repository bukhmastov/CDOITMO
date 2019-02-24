package com.bukhmastov.cdoitmo.object.impl;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.model.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.model.protocol.Protocol;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.object.ProtocolTrackerJobService;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import dagger.Lazy;

public class ProtocolTrackerImpl implements ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private static final String TRUE = "1";
    private static final String FALSE = "0";
    private final int jobID = 0;

    private JobScheduler jobScheduler = null;
    private JobScheduler getJobScheduler(@NonNull Context context) {
        if (jobScheduler == null) {
            jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        }
        if (jobScheduler == null) {
            throw new UnsupportedOperationException("JobScheduler is null. Unable to use ProtocolTracker");
        }
        return jobScheduler;
    }

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    Lazy<DeIfmoRestClient> deIfmoRestClient;
    @Inject
    Lazy<Time> time;

    public ProtocolTrackerImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public ProtocolTracker check(@NonNull Context context) {
        return check(context, null);
    }

    @Override
    public ProtocolTracker check(@NonNull Context context, @Nullable Callable callback) {
        log.v(TAG, "check");
        thread.assertNotUI();
        boolean enabled = storagePref.get(context, "pref_use_notifications", true);
        boolean running = TRUE.equals(storage.get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", FALSE));
        log.v(TAG, "check | enabled=", enabled, " | running=", running);
        if (enabled && !running) {
            start(context, callback);
            return this;
        }
        if (!enabled && running) {
            stop(context, callback);
            return this;
        }
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && enabled && getJobScheduler(context).getPendingJob(jobID) == null) {
            log.v(TAG, "check | enabled=", true, " | job is null");
            restart(context, callback);
            return this;
        }*/
        if (callback != null) {
            callback.call();
        }
        return this;
    }

    @Override
    public ProtocolTracker restart(@NonNull Context context) {
        return restart(context, null);
    }

    @Override
    public ProtocolTracker restart(@NonNull Context context, @Nullable Callable callback) {
        log.v(TAG, "restart");
        stop(context, () -> start(context, callback));
        return this;
    }

    @Override
    public ProtocolTracker start(@NonNull Context context) {
        return start(context, null);
    }

    @Override
    public ProtocolTracker start(@NonNull Context context, @Nullable Callable callback) {
        log.v(TAG, "start");
        thread.assertNotUI();
        if (App.UNAUTHORIZED_MODE) {
            log.v(TAG, "start | UNAUTHORIZED_MODE");
            stop(context, callback);
            return this;
        }
        boolean enabled = storagePref.get(context, "pref_use_notifications", true);
        boolean running = TRUE.equals(storage.get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", FALSE));
        log.v(TAG, "start | enabled=", enabled, " | running=", running);
        if (enabled && !running) {
            log.v(TAG, "start | starting");
            try {
                int frequency = Integer.parseInt(storagePref.get(context, "pref_notify_frequency", "30"));
                long intervalMillis = (long) frequency * 60000L;
                long intervalFlexMillis = 5 * intervalMillis / 100;
                boolean network_unmetered = storagePref.get(context, "pref_notify_network_unmetered", false);
                JobInfo.Builder builder = new JobInfo.Builder(jobID, new ComponentName(context, ProtocolTrackerJobService.class));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setPeriodic(intervalMillis, Math.max(intervalFlexMillis, JobInfo.getMinFlexMillis()));
                } else {
                    builder.setPeriodic(intervalMillis);
                }
                builder.setPersisted(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    NetworkRequest.Builder nrBuilder = new NetworkRequest.Builder();
                    nrBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
                    nrBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    if (network_unmetered) {
                        nrBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
                    }
                    builder.setRequiredNetwork(nrBuilder.build());
                } else {
                    builder.setRequiredNetworkType(network_unmetered ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY);
                }
                int result = getJobScheduler(context).schedule(builder.build());
                if (JobScheduler.RESULT_SUCCESS == result) {
                    storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", TRUE);
                    storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", "");
                    log.i(TAG, "start | started | user=", storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login"), " | frequency=", frequency);
                } else {
                    log.e(TAG, "start | failed to schedule job | result=", result);
                }
                if (callback != null) {
                    callback.call();
                }
            } catch (Exception e) {
                log.e(TAG, "start | failed to schedule job");
                log.exception(e);
                stop(context, callback);
            }
            return this;
        }
        if (callback != null) {
            callback.call();
        }
        return this;
    }

    @Override
    public ProtocolTracker stop(@NonNull Context context) {
        return stop(context, null);
    }

    @Override
    public ProtocolTracker stop(@NonNull Context context, @Nullable Callable callback) {
        log.v(TAG, "stop");
        thread.assertNotUI();
        boolean running = TRUE.equals(storage.get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", FALSE));
        log.v(TAG, "stop | running=", running);
        if (running) {
            try {
                log.v(TAG, "stop | stopping");
                getJobScheduler(context).cancel(jobID);
                storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", FALSE);
                storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", "");
                log.i(TAG, "stop | stopped");
            } catch (Exception e) {
                log.e(TAG, "stop | failed to stop job");
                log.exception(e);
            }
        }
        if (callback != null) {
            callback.call();
        }
        return this;
    }

    @Override
    public ProtocolTracker reset(@NonNull Context context) {
        return reset(context, null);
    }

    @Override
    public ProtocolTracker reset(@NonNull Context context, @Nullable Callable callback) {
        log.v(TAG, "reset");
        thread.assertNotUI();
        try {
            getJobScheduler(context).cancelAll();
            storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", FALSE);
            storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", "");
        } catch (Exception e) {
            log.e(TAG, "reset | failed to reset job");
            log.exception(e);
        }
        check(context, callback);
        return this;
    }

    @Override
    public void setup(@NonNull Context context) {
        setup(context, 0);
    }

    private void setup(@NonNull Context context, int attempt) {
        log.v(TAG, "setup | attempt=", attempt);
        thread.assertNotUI();
        if (!storagePref.get(context, "pref_protocol_changes_track", true)) {
            log.v(TAG, "setup | pref_protocol_changes_track=false");
            return;
        }
        if (attempt >= 3) {
            log.v(TAG, "setup | failed to setup, number of attempts exceeded the limit");
            return;
        }
        deIfmoRestClient.get().get(context, "eregisterlog?days=126", null, new RestResponseHandler<Protocol>() {
            @Override
            public void onSuccess(int code, Client.Headers headers, Protocol response) throws Exception {
                if (code != 200 || response == null) {
                    setup(context, attempt + 1);
                    return;
                }
                try {
                    response.setTimestamp(time.get().getTimeInMillis());
                    response.setNumberOfWeeks(18);
                    new ProtocolConverter(response).convert();
                    log.i(TAG, "setup | uploaded");
                } catch (Exception e) {
                    setup(context, attempt + 1);
                }
            }
            @Override
            public void onFailure(int code, Client.Headers headers, int state) {
                setup(context, attempt + 1);
            }
            @Override
            public void onProgress(int state) {}
            @Override
            public void onNewRequest(Client.Request request) {}
            @Override
            public Protocol newInstance() {
                return new Protocol();
            }
            @Override
            public JSONObject convertArray(JSONArray arr) throws JSONException {
                return new JSONObject().put("protocol", arr);
            }
        });
    }
}
