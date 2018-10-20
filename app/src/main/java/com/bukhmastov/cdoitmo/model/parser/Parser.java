package com.bukhmastov.cdoitmo.model.parser;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Time;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import javax.inject.Inject;

import dagger.Lazy;

public abstract class Parser<T extends JsonEntity> extends ParserBase {

    private final String html;

    public Parser(@NonNull String html) {
        super();
        this.html = html;
    }

    public T parse() {
        String trace = firebasePerformanceProvider.startTrace(getTraceName());
        try {
            TagNode root = new HtmlCleaner().clean(html.replace("&nbsp;", " "));
            if (root == null) {
                throw new SilentException();
            }
            T entity = doParse(root);
            firebasePerformanceProvider.putAttribute(trace, "state", "done");
            return entity;
        } catch (SilentException silent) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            return null;
        } catch (Throwable e) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            log.exception("Failed to parse data to entity", e);
            return null;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    protected abstract T doParse(TagNode root) throws Throwable;

    protected abstract @FirebasePerformanceProvider.TRACE String getTraceName();
}
