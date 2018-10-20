package com.bukhmastov.cdoitmo.model.protocol;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class Protocol extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("number_of_weeks")
    private int numberOfWeeks;

    @JsonProperty("protocol")
    private ArrayList<PChange> changes;

    public Protocol() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getNumberOfWeeks() {
        return numberOfWeeks;
    }

    public void setNumberOfWeeks(int numberOfWeeks) {
        this.numberOfWeeks = numberOfWeeks;
    }

    public ArrayList<PChange> getChanges() {
        return changes;
    }

    public void setChanges(ArrayList<PChange> changes) {
        this.changes = changes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Protocol)) return false;
        Protocol protocol = (Protocol) o;
        return timestamp == protocol.timestamp &&
                numberOfWeeks == protocol.numberOfWeeks &&
                Objects.equals(changes, protocol.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, numberOfWeeks, changes);
    }

    @Override
    public String toString() {
        return "Protocol{" +
                "timestamp=" + timestamp +
                ", numberOfWeeks=" + numberOfWeeks +
                ", changes=" + changes +
                '}';
    }
}
