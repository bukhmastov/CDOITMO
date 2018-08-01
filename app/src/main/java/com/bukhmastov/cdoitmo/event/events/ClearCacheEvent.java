package com.bukhmastov.cdoitmo.event.events;

public class ClearCacheEvent {

    private final String identity;

    public ClearCacheEvent() {
        this("all");
    }

    public ClearCacheEvent(String identity) {
        this.identity = identity;
    }

    public String getIdentity() {
        return identity;
    }

    public boolean is(String identity) {
        return "all".equals(this.identity) || this.identity.equals(identity);
    }

    public boolean isNot(String identity) {
        return !is(identity);
    }
}
