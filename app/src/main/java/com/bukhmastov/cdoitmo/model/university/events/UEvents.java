package com.bukhmastov.cdoitmo.model.university.events;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UEvents extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("search")
    private String search;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("count")
    private int count;

    @JsonProperty("list")
    private ArrayList<UEvent> events;

    public UEvents() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ArrayList<UEvent> getEvents() {
        return events;
    }

    public void setEvents(ArrayList<UEvent> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UEvents)) return false;
        UEvents uEvents = (UEvents) o;
        return timestamp == uEvents.timestamp &&
                limit == uEvents.limit &&
                offset == uEvents.offset &&
                count == uEvents.count &&
                Objects.equals(search, uEvents.search) &&
                Objects.equals(events, uEvents.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, search, limit, offset, count, events);
    }

    @Override
    public String toString() {
        return "UEvents{" +
                "timestamp=" + timestamp +
                ", search='" + search + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", count=" + count +
                ", events=" + events +
                '}';
    }
}
