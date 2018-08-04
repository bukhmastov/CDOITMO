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
import com.bukhmastov.cdoitmo.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.object.ProtocolTrackerJobService;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

public class ProtocolTrackerImpl implements ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
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

    public ProtocolTrackerImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public ProtocolTracker check(@NonNull Context context) {
        return check(context, null);
    }

    @Override
    public ProtocolTracker check(@NonNull Context context, @Nullable final Callable callback) {
        log.v(TAG, "check");
        boolean enabled = storagePref.get(context, "pref_use_notifications", true);
        boolean running = "1".equals(storage.get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", "0"));
        if (enabled && !running) {
            start(context, callback);
        } else if (!enabled && running) {
            stop(context, callback);
        } else if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (getJobScheduler(context).getPendingJob(jobID) == null) throw new Exception("job is null");
                if (callback != null) {
                    callback.call();
                }
            } catch (Exception e) {
                log.w(TAG, e.getMessage());
                restart(context, callback);
            }
        } else {
            if (callback != null) {
                callback.call();
            }
        }
        return this;
    }

    @Override
    public ProtocolTracker restart(@NonNull Context context) {
        return restart(context, null);
    }

    @Override
    public ProtocolTracker restart(@NonNull Context context, @Nullable final Callable callback) {
        log.v(TAG, "restart");
        stop(context, () -> start(context, callback));
        return this;
    }

    @Override
    public ProtocolTracker start(@NonNull Context context) {
        return start(context, null);
    }

    @Override
    public ProtocolTracker start(@NonNull Context context, @Nullable final Callable callback) {
        log.v(TAG, "start");
        if (App.UNAUTHORIZED_MODE) {
            log.v(TAG, "start | UNAUTHORIZED_MODE");
            stop(context, callback);
            return this;
        }
        boolean enabled = storagePref.get(context, "pref_use_notifications", true);
        boolean running = "1".equals(storage.get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", "0"));
        if (enabled && !running) {
            log.v(TAG, "Starting");
            try {
                int frequency = Integer.parseInt(storagePref.get(context, "pref_notify_frequency", "30"));
                boolean network_unmetered = storagePref.get(context, "pref_notify_network_unmetered", false);
                JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, ProtocolTrackerJobService.class));
                builder.setPeriodic(frequency * 60000);
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
                getJobScheduler(context).schedule(builder.build());
                storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", "1");
                storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", "");
                log.i(TAG, "Started | user = " + storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login") + " | frequency = " + frequency);
                if (callback != null) {
                    callback.call();
                }
            } catch (Exception e){
                log.e(TAG, "Failed to schedule job");
                log.exception(e);
                stop(context, callback);
            }
        } else {
            if (callback != null) {
                callback.call();
            }
        }
        return this;
    }

    @Override
    public ProtocolTracker stop(@NonNull Context context) {
        return stop(context, null);
    }

    @Override
    public ProtocolTracker stop(@NonNull Context context, @Nullable final Callable callback) {
        log.v(TAG, "stop");
        boolean running = "1".equals(storage.get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", "0"));
        if (running) {
            log.v(TAG, "Stopping");
            getJobScheduler(context).cancel(jobID);
            storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", "0");
            storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", "");
            log.i(TAG, "Stopped");
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
    public ProtocolTracker reset(@NonNull Context context, @Nullable final Callable callback) {
        log.v(TAG, "reset");
        getJobScheduler(context).cancelAll();
        storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#job_service_running", "0");
        storage.put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", "");
        check(context, callback);
        return this;
    }

    @Override
    public void setup(@NonNull Context context, @NonNull DeIfmoRestClient deIfmoRestClient, int attempt) {
        thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "setup | attempt=" + attempt);
            if (!storagePref.get(context, "pref_protocol_changes_track", true)) {
                log.v(TAG, "setup | pref_protocol_changes_track=false");
                return;
            }
            if (attempt < 3) {
                deIfmoRestClient.get(context, "eregisterlog?days=126", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, Client.Headers headers, JSONObject responseObj, final JSONArray responseArr) {
                        thread.run(Thread.BACKGROUND, () -> {
                            if (statusCode == 200 && responseArr != null) {
                                new ProtocolConverter(context, responseArr, 18, json -> log.i(TAG, "setup | uploaded")).run();
                            } else {
                                setup(context, deIfmoRestClient, attempt + 1);
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        thread.run(Thread.BACKGROUND, () -> setup(context, deIfmoRestClient, attempt + 1));
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {}
                });
            }
        });
    }
}
