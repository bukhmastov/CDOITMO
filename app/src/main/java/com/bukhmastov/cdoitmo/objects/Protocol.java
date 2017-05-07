package com.bukhmastov.cdoitmo.objects;

import android.app.Activity;
import android.os.Handler;

import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Protocol {

    private static final String TAG = "Protocol";
    private Activity activity;
    private JSONObject protocol = null;
    private boolean accessed = false;
    public interface Callback {
        void onDone(JSONObject protocol);
        void onChecked(boolean is);
    }

    public Protocol(Activity activity){
        this.activity = activity;
    }
    public void put(JSONArray data, int number_of_weeks, final Handler handler){
        Log.v(TAG, "put");
        try {
            JSONArray array = new JSONArray();
            array.put(number_of_weeks);
            new ProtocolConverter(activity, new ProtocolConverter.response() {
                @Override
                public void finish(JSONObject json) {
                    Log.v(TAG, "put | ProtocolConverter.finish");
                    try {
                        protocol = json;
                        Storage.file.cache.put(activity, "protocol#core", protocol.toString());
                        Storage.file.perm.put(activity, "protocol_tracker#protocol", protocol.getJSONArray("protocol").toString());
                        handler.sendEmptyMessage(0);
                    } catch (JSONException e) {
                        Static.error(e);
                    }
                }
            }).execute(data, array);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public void get(final Callback callback){
        Log.v(TAG, "get");
        if (accessed) {
            done(activity, callback, protocol);
        } else {
            access(callback, -1);
        }
    }
    public void is(final Callback callback){
        is(callback, -1);
    }
    public void is(final Callback callback, int number_of_weeks){
        Log.v(TAG, "is | number_of_weeks=" + number_of_weeks);
        if (accessed) {
            checked(activity, callback, protocol != null);
        } else {
            access(callback, number_of_weeks);
        }
    }
    private void access(final Callback callback, final int number_of_weeks){
        Log.v(TAG, "access");
        (new Thread(new Runnable() {
            @Override
            public void run() {
                accessed = true;
                final String cProtocol = Storage.file.cache.get(activity, "protocol#core");
                if (!cProtocol.isEmpty()) {
                    try {
                        protocol = new JSONObject(cProtocol);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
                if (number_of_weeks == -1) {
                    checked(activity, callback, protocol != null);
                } else {
                    try {
                        checked(activity, callback, protocol != null && protocol.getInt("number_of_weeks") == number_of_weeks);
                    } catch (JSONException e) {
                        Static.error(e);
                        checked(activity, callback, false);
                    }
                }
                done(activity, callback, protocol);
            }
        })).start();
    }

    private void done(final Activity activity, final Callback callback, final JSONObject protocol){
        Log.v(TAG, "done");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onDone(protocol);
            }
        });
    }
    private void checked(final Activity activity, final Callback callback, final boolean is){
        Log.v(TAG, "checked");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onChecked(is);
            }
        });
    }

}
