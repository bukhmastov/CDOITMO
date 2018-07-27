package com.bukhmastov.cdoitmo.event.bus;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.bus.annotation.Tag;
import com.bukhmastov.cdoitmo.event.bus.entity.EventType;
import com.bukhmastov.cdoitmo.event.bus.entity.SubscriberEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SubscribersFinder {

    private static final ConcurrentMap<Class<?>, Map<EventType, Set<Method>>> subscribersCache = new ConcurrentHashMap<>();

    public static Map<EventType, Set<SubscriberEvent>> findAllSubscribers(@NonNull Object listener) throws IllegalArgumentException {

        Class<?> listenerClass = listener.getClass();
        Map<EventType, Set<SubscriberEvent>> subscribersInMethod = new HashMap<>();
        Map<EventType, Set<Method>> methods = subscribersCache.get(listenerClass);

        if (methods == null) {
            methods = new HashMap<>();
            loadAnnotatedMethods(listenerClass, methods);
        }

        if (!methods.isEmpty()) {
            for (Map.Entry<EventType, Set<Method>> entry : methods.entrySet()) {
                Set<SubscriberEvent> subscribers = new HashSet<>();
                for (Method method : entry.getValue()) {
                    subscribers.add(new SubscriberEvent(listener, method));
                }
                subscribersInMethod.put(entry.getKey(), subscribers);
            }
        }

        return subscribersInMethod;
    }

    private static void loadAnnotatedMethods(@NonNull Class<?> listenerClass, @NonNull Map<EventType, Set<Method>> subscriberMethods) throws IllegalArgumentException {

        for (Method method : listenerClass.getDeclaredMethods()) {

            if (method.isBridge()) {
                continue;
            }

            if (method.isAnnotationPresent(Event.class)) {

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalArgumentException("Method " + method + " has @Event annotation but requires " + parameterTypes.length + " arguments. Methods must require a single argument.");
                }

                Class<?> parameterClazz = parameterTypes[0];
                if (parameterClazz.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + " has @Event annotation on " + parameterClazz + " which is an interface. Event subscription must be on a concrete class type.");
                }

                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method " + method + " has @Event annotation on " + parameterClazz + " but is not 'public'.");
                }

                Event annotation = method.getAnnotation(Event.class);
                Tag[] tags = annotation.tags();
                int tagsLength = tags.length;

                do {
                    String tag = Tag.DEFAULT;
                    if (tagsLength > 0) {
                        tag = tags[tagsLength - 1].value();
                    }
                    EventType type = new EventType(tag, parameterClazz);
                    Set<Method> methods = subscriberMethods.get(type);
                    if (methods == null) {
                        methods = new HashSet<>();
                        subscriberMethods.put(type, methods);
                    }
                    methods.add(method);
                    tagsLength--;
                } while (tagsLength > 0);
            }
        }

        subscribersCache.put(listenerClass, subscriberMethods);
    }
}
