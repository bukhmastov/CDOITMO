package com.bukhmastov.cdoitmo.event.bus.entity;

import android.support.annotation.NonNull;

public class EventType {

    private final String tag;

    private final Class<?> clazz;

    private final int hashCode;

    public EventType(@NonNull String tag, @NonNull Class<?> clazz) {
        int prime = 31;
        this.tag = tag;
        this.clazz = clazz;
        this.hashCode = (prime + tag.hashCode()) * prime + clazz.hashCode();
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

        EventType other = (EventType) obj;

        return tag.equals(other.tag) && clazz == other.clazz;
    }

    @Override
    public String toString() {
        return "[EventType " + tag + " && " + clazz + "]";
    }
}
