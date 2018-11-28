package com.bukhmastov.cdoitmo.model.group;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class GList extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("list")
    private ArrayList<GGroup> list;

    public GList() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<GGroup> getList() {
        return list;
    }

    public void setList(ArrayList<GGroup> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GList)) return false;
        GList gList = (GList) o;
        return timestamp == gList.timestamp &&
                Objects.equals(list, gList.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, list);
    }

    @Override
    public String toString() {
        return "GList{" +
                "timestamp=" + timestamp +
                ", list=" + list +
                '}';
    }
}
