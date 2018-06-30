package com.bukhmastov.cdoitmo.converter;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Converter implements Runnable {

    private final Response delegate;

    public interface Response {
        void finish(JSONObject json);
    }

    //@Inject
    private FirebasePerformanceProvider firebasePerformanceProvider = FirebasePerformanceProvider.instance();

    public Converter(Response delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        String trace = firebasePerformanceProvider.startTrace(getTraceName());
        try {
            JSONObject json = convert();
            firebasePerformanceProvider.putAttribute(trace, "state", "done");
            delegate.finish(json);
        } catch (SilentException silent) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            delegate.finish(null);
        } catch (Throwable e) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            Log.exception(e);
            delegate.finish(null);
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    abstract protected JSONObject convert() throws Throwable;

    abstract protected @FirebasePerformanceProvider.TRACE String getTraceName();

    protected String getString(JSONObject json, String key) throws JSONException {
        return getString(json, key, "");
    }
    protected String getString(JSONObject json, String key, String replace) throws JSONException {
        return json.has(key) && !json.isNull(key) && !json.get(key).toString().isEmpty() && !json.get(key).toString().equals("null") ? json.get(key).toString() : replace;
    }
}
