package com.bukhmastov.cdoitmo.model.scholarship.detailed;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SSDetailedList extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("list")
    private ArrayList<SSDetailed> list;

    public SSDetailedList() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<SSDetailed> getList() {
        return list;
    }

    public void setList(ArrayList<SSDetailed> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSDetailedList)) return false;
        SSDetailedList that = (SSDetailedList) o;
        return timestamp == that.timestamp &&
                Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, list);
    }

    @Override
    public String toString() {
        return "SSDetailedList{" +
                "timestamp=" + timestamp +
                ", list=" + list +
                '}';
    }
}
