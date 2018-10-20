package com.bukhmastov.cdoitmo.model.rating.pickerall;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class RatingPickerAll extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("faculties")
    private ArrayList<RFaculty> faculties;

    public RatingPickerAll() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<RFaculty> getFaculties() {
        return faculties;
    }

    public void setFaculties(ArrayList<RFaculty> faculties) {
        this.faculties = faculties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RatingPickerAll)) return false;
        RatingPickerAll that = (RatingPickerAll) o;
        return timestamp == that.timestamp &&
                Objects.equals(faculties, that.faculties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, faculties);
    }

    @Override
    public String toString() {
        return "RatingPickerAll{" +
                "timestamp=" + timestamp +
                ", faculties=" + faculties +
                '}';
    }
}
