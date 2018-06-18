package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Log;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONObject;

public abstract class Parse<T> implements Runnable {

    private final Response<T> delegate;
    private final String data;

    public interface Response<T> {
        void finish(T json);
    }

    public Parse(String data, Response<T> delegate) {
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        String trace = FirebasePerformanceProvider.startTrace(getTraceName());
        try {
            TagNode root = new HtmlCleaner().clean(data.replace("&nbsp;", " "));
            if (root == null) {
                throw new SilentException();
            }
            T data = parse(root);
            FirebasePerformanceProvider.putAttribute(trace, "state", "done");
            delegate.finish(data);
        } catch (SilentException silent) {
            FirebasePerformanceProvider.putAttribute(trace, "state", "failed");
            delegate.finish(null);
        } catch (Throwable e) {
            FirebasePerformanceProvider.putAttribute(trace, "state", "failed");
            FirebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            Log.exception(e);
            delegate.finish(null);
        } finally {
            FirebasePerformanceProvider.stopTrace(trace);
        }
    }

    abstract protected T parse(TagNode root) throws Throwable;

    abstract protected @FirebasePerformanceProvider.TRACE String getTraceName();
}
