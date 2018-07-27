package com.bukhmastov.cdoitmo.event.bus.impl;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.SubscribersFinder;
import com.bukhmastov.cdoitmo.event.bus.annotation.Tag;
import com.bukhmastov.cdoitmo.event.bus.entity.EventType;
import com.bukhmastov.cdoitmo.event.bus.entity.SubscriberEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.inject.Inject;

import dagger.Lazy;

public class EventBusImpl implements EventBus {

    private static final String TAG = "EventBus";
    private static final String DEFAULT_IDENTIFIER = "default";
    private final String identifier;
    private final ConcurrentMap<EventType, Set<SubscriberEvent>> subscribersByType = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Set<Class<?>>> flattenHierarchyCache = new ConcurrentHashMap<>();

    @Inject
    Lazy<Log> log;

    public EventBusImpl() {
        this(DEFAULT_IDENTIFIER);
    }

    public EventBusImpl(String identifier) {
        AppComponentProvider.getComponent().inject(this);
        this.identifier = identifier;
    }

    @Override
    public void register(@NonNull Object object) {

        Map<EventType, Set<SubscriberEvent>> foundSubscribersMap;
        try {
            foundSubscribersMap = SubscribersFinder.findAllSubscribers(object);
        } catch (IllegalArgumentException e) {
            log.get().v(TAG, "register | ", e.getMessage());
            foundSubscribersMap = new HashMap<>();
        }

        for (EventType type : foundSubscribersMap.keySet()) {

            Set<SubscriberEvent> subscribers = subscribersByType.get(type);

            if (subscribers == null) {
                Set<SubscriberEvent> subscriberEvents = new CopyOnWriteArraySet<>();
                subscribers = subscribersByType.putIfAbsent(type, subscriberEvents);
                if (subscribers == null) {
                    subscribers = subscriberEvents;
                }
            }

            Set<SubscriberEvent> foundSubscribers = foundSubscribersMap.get(type);

            if (!subscribers.addAll(foundSubscribers)) {
                log.get().v(TAG, "register | Object ", object.getClass(), " has already been registered");
                return;
            }
        }
    }

    @Override
    public void unregister(@NonNull Object object) {

        Map<EventType, Set<SubscriberEvent>> foundSubscribersMap;
        try {
            foundSubscribersMap = SubscribersFinder.findAllSubscribers(object);
        } catch (IllegalArgumentException e) {
            log.get().v(TAG, "unregister | ", e.getMessage());
            foundSubscribersMap = new HashMap<>();
        }

        for (Map.Entry<EventType, Set<SubscriberEvent>> entry : foundSubscribersMap.entrySet()) {

            Set<SubscriberEvent> currentSubscribers = subscribersByType.get(entry.getKey());

            Collection<SubscriberEvent> eventMethodsInListener = entry.getValue();

            if (currentSubscribers == null || !currentSubscribers.containsAll(eventMethodsInListener)) {
                log.get().v(TAG, "unregister | Object ", object.getClass(), " was not registered");
                return;
            }

            for (SubscriberEvent subscriber : currentSubscribers) {
                if (eventMethodsInListener.contains(subscriber)) {
                    subscriber.invalidate();
                }
            }

            currentSubscribers.removeAll(eventMethodsInListener);
        }
    }

    @Override
    public void fire(@NonNull Object event) {
        fire(Tag.DEFAULT, event);
    }

    @Override
    public void fire(@NonNull String tag, @NonNull Object event) {

        Set<Class<?>> classes = flattenHierarchy(event.getClass());

        boolean dispatched = false;

        for (Class<?> clazz : classes) {

            Set<SubscriberEvent> subscribers = subscribersByType.get(new EventType(tag, clazz));

            if (subscribers != null && !subscribers.isEmpty()) {
                dispatched = true;
                for (SubscriberEvent subscriber : subscribers) {
                    dispatch(event, subscriber);
                }
            }
        }

        if (!dispatched) {
            log.get().v(TAG, "fire | Event ", event.getClass(), " with tag ", tag, " was not delivered, there is no receivers for it");
        }
    }

    private Set<Class<?>> flattenHierarchy(Class<?> clazz) {

        Set<Class<?>> classes = flattenHierarchyCache.get(clazz);

        if (classes == null) {
            Set<Class<?>> classesCreation = getClassesFor(clazz);
            classes = flattenHierarchyCache.putIfAbsent(clazz, classesCreation);
            if (classes == null) {
                classes = classesCreation;
            }
        }

        return classes;
    }

    private Set<Class<?>> getClassesFor(Class<?> clazz) {

        List<Class<?>> parents = new LinkedList<>();
        Set<Class<?>> classes = new HashSet<>();

        parents.add(clazz);

        while (!parents.isEmpty()) {
            Class<?> cl = parents.remove(0);
            classes.add(cl);
            Class<?> parent = cl.getSuperclass();
            if (parent != null) {
                parents.add(parent);
            }
        }

        return classes;
    }

    private void dispatch(Object event, SubscriberEvent subscriber) {
        if (subscriber.isValid()) {
            subscriber.handle(event);
        }
    }

    @Override
    public String toString() {
        return "[Bus \"" + identifier + "\"]";
    }
}
