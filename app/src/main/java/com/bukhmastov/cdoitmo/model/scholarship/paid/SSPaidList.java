package com.bukhmastov.cdoitmo.model.scholarship.paid;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SSPaidList extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("list")
    private ArrayList<SSPaid> list;

    public SSPaidList() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<SSPaid> getList() {
        return list;
    }

    public void setList(ArrayList<SSPaid> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSPaidList)) return false;
        SSPaidList that = (SSPaidList) o;
        return timestamp == that.timestamp &&
                Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, list);
    }

    @Override
    public String toString() {
        return "SSPaidList{" +
                "timestamp=" + timestamp +
                ", list=" + list +
                '}';
    }
}
