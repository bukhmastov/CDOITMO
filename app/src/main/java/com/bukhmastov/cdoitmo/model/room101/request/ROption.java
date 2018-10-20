package com.bukhmastov.cdoitmo.model.room101.request;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class ROption extends JsonEntity {

    @JsonProperty("time")
    private String time;

    @JsonProperty("available")
    private String available;

    public ROption() {
        super();
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ROption)) return false;
        ROption option = (ROption) o;
        return Objects.equals(time, option.time) &&
                Objects.equals(available, option.available);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, available);
    }

    @Override
    public String toString() {
        return "ROption{" +
                "time='" + time + '\'' +
                ", available='" + available + '\'' +
                '}';
    }
}
