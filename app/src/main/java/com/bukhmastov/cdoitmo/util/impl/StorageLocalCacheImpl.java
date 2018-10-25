package com.bukhmastov.cdoitmo.util.impl;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.StorageLocalCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StorageLocalCacheImpl implements StorageLocalCache {

    private static long requests = 0;
    private static final int maxStack = 8;
    private final HashMap<String, ElementMeta> stackOfMeta = new HashMap<>();
    private final HashMap<String, ElementData> stackOfData = new HashMap<>();

    private static class ElementMeta {
        String path = "";
        double priority = 1;
        long requests = 0;
        double rate = 0;
        ElementMeta(String path, double priority){
            this.path = path;
            this.priority = priority;
        }
    }
    private static class ElementData {
        String path = "";
        String data = "";
        ElementData(String path, String data){
            this.path = path;
            this.data = data;
        }
    }

    @Inject
    EventBus eventBus;

    public StorageLocalCacheImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.ALL)) {
            return;
        }
        reset();
    }

    @Override
    public void push(@NonNull String path, String data, double priority) {
        if (stackOfMeta.containsKey(path)) {
            stackOfMeta.get(path).priority = priority;
        } else {
            stackOfMeta.put(path, new ElementMeta(path, priority));
        }
        if (stackOfData.containsKey(path)) {
            stackOfData.get(path).data = data;
        } else {
            stackOfData.put(path, new ElementData(path, data));
        }
        check();
    }

    @Override
    public void access(@NonNull String path) {
        if (stackOfMeta.containsKey(path)) {
            requests++;
            stackOfMeta.get(path).requests++;
        }
    }

    @Override
    public String get(@NonNull String path) {
        if (stackOfData.containsKey(path)) {
            return stackOfData.get(path).data;
        } else {
            return null;
        }
    }

    @Override
    public void delete(@NonNull String path) {
        if (stackOfMeta.containsKey(path)) stackOfMeta.remove(path);
        if (stackOfData.containsKey(path)) stackOfData.remove(path);
    }

    @Override
    public void check() {
        if (requests + 10 > Long.MAX_VALUE) reset();
        if (stackOfData.size() > maxStack) {
            for (Map.Entry<String, ElementMeta> entry : stackOfMeta.entrySet()) {
                ElementMeta elementMeta = entry.getValue();
                elementMeta.rate = ((double) elementMeta.requests / (double) requests) * elementMeta.priority;
            }
            List<ElementMeta> elementMetas = new ArrayList<>(stackOfMeta.values());
            Collections.sort(elementMetas, (s1, s2) -> Double.compare(s2.rate, s1.rate));
            for (int i = maxStack; i < elementMetas.size(); i++) {
                stackOfData.remove(elementMetas.get(i).path);
            }
        }
    }

    @Override
    public void reset() {
        requests = 0;
        stackOfMeta.clear();
        stackOfData.clear();
    }
}
