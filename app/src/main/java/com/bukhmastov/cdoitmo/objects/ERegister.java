package com.bukhmastov.cdoitmo.objects;

import android.app.Activity;
import android.os.Handler;

import com.bukhmastov.cdoitmo.converters.ERegisterConverter;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

public class ERegister {

    private static final String TAG = "ERegister";
    private Activity activity;
    private JSONObject eregister = null;
    private boolean accessed = false;
    public interface Callback {
        void onDone(JSONObject eregister);
        void onChecked(boolean is);
    }

    public ERegister(final Activity activity){
        Log.v(TAG, "initialized");
        this.activity = activity;
    }
    public void put(JSONObject data, final Handler handler){
        Log.v(TAG, "put");
        new ERegisterConverter(new ERegisterConverter.response() {
            @Override
            public void finish(JSONObject json) {
                Log.v(TAG, "put | ERegisterConverter.finish");
                eregister = json;
                Storage.file.cache.put(activity, "eregister#core", eregister.toString());
                handler.sendEmptyMessage(0);
            }
        }).execute(data);
    }
    public void get(final Callback callback){
        Log.v(TAG, "get");
        if (accessed) {
            done(activity, callback, eregister);
        } else {
            access(callback);
        }
    }
    public void is(final Callback callback){
        Log.v(TAG, "is");
        if (accessed) {
            checked(activity, callback, eregister != null);
        } else {
            access(callback);
        }
    }
    private void access(final Callback callback){
        Log.v(TAG, "access");
        (new Thread(new Runnable() {
            @Override
            public void run() {
                accessed = true;
                final String eRegister = Storage.file.cache.get(activity, "eregister#core");
                if (!eRegister.isEmpty()) {
                    try {
                        eregister = new JSONObject(eRegister);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
                checked(activity, callback, eregister != null);
                done(activity, callback, eregister);
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