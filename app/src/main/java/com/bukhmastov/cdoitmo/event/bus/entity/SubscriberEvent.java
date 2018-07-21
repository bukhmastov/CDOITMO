package com.bukhmastov.cdoitmo.event.bus.entity;

import android.support.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class SubscriberEvent {

    private final Object target;
    private final Method method;
    private Subject<Object> subject;
    private Disposable disposable;
    private boolean valid = true;
    private final int hashCode;

    public SubscriberEvent(@NonNull Object target, @NonNull Method method) {
        int prime = 31;
        method.setAccessible(true);
        this.target = target;
        this.method = method;
        this.hashCode = (prime + method.hashCode()) * prime + target.hashCode();
        this.subject = PublishSubject.create();
        this.disposable = subject.subscribe(event -> {
            try {
                if (valid) {
                    handleEvent(event);
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Could not dispatch event: " + event.getClass() + " to subscriber " + SubscriberEvent.this, e);
            }
        });
    }

    private void handleEvent(Object event) throws InvocationTargetException {
        if (!valid) {
            throw new IllegalStateException(toString() + " has been invalidated and can no longer handle events");
        }
        try {
            method.invoke(target, event);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
        disposable.dispose();
    }

    public void handle(Object event) {
        subject.onNext(event);
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final SubscriberEvent other = (SubscriberEvent) obj;

        return method.equals(other.method) && target == other.target;
    }

    @Override
    public String toString() {
        return "[SubscriberEvent " + method + "]";
    }
}
