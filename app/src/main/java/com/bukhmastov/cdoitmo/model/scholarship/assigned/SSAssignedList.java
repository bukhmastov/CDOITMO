package com.bukhmastov.cdoitmo.model.scholarship.assigned;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SSAssignedList extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("list")
    private ArrayList<SSAssigned> list;

    public SSAssignedList() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<SSAssigned> getList() {
        return list;
    }

    public void setList(ArrayList<SSAssigned> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSAssignedList)) return false;
        SSAssignedList that = (SSAssignedList) o;
        return timestamp == that.timestamp &&
                Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, list);
    }

    @Override
    public String toString() {
        return "SSAssignedList{" +
                "timestamp=" + timestamp +
                ", list=" + list +
                '}';
    }
}
