package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Time;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONObject;

public abstract class Parse implements Runnable {

    private final Response delegate;
    private final String data;

    public interface Response {
        void finish(JSONObject json);
    }

    //@Inject
    protected Log log = Log.instance();
    //@Inject
    protected Time time = Time.instance();
    //@Inject
    private FirebasePerformanceProvider firebasePerformanceProvider = FirebasePerformanceProvider.instance();

    public Parse(String data, Response delegate) {
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        String trace = firebasePerformanceProvider.startTrace(getTraceName());
        try {
            TagNode root = new HtmlCleaner().clean(data.replace("&nbsp;", " "));
            if (root == null) {
                throw new SilentException();
            }
            JSONObject json = parse(root);
            firebasePerformanceProvider.putAttribute(trace, "state", "done");
            delegate.finish(json);
        } catch (SilentException silent) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            delegate.finish(null);
        } catch (Throwable e) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            log.exception(e);
            delegate.finish(null);
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    abstract protected JSONObject parse(TagNode root) throws Throwable;

    abstract protected @FirebasePerformanceProvider.TRACE String getTraceName();
}
