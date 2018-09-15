package com.bukhmastov.cdoitmo.event.events;

public class ShareTextEvent {

    private final String text;
    private final String analyticsType;

    public ShareTextEvent(String text) {
        this.text = text;
        this.analyticsType = null;
    }

    public ShareTextEvent(String text, String analyticsType) {
        this.text = text;
        this.analyticsType = analyticsType;
    }

    public String getText() {
        return text;
    }

    public String getAnalyticsType() {
        return analyticsType;
    }
}
