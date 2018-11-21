package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUScheduleApiResponse extends JsonEntity {

    @JsonProperty("faculties")
    private ArrayList<ISUFaculty> faculties;

    public ISUScheduleApiResponse() {
        super();
    }

    public ArrayList<ISUFaculty> getFaculties() {
        return faculties;
    }

    public void setFaculties(ArrayList<ISUFaculty> faculties) {
        this.faculties = faculties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUScheduleApiResponse)) return false;
        ISUScheduleApiResponse that = (ISUScheduleApiResponse) o;
        return Objects.equals(faculties, that.faculties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faculties);
    }

    @Override
    public String toString() {
        return "ISUScheduleApiResponse{" +
                "faculties=" + faculties +
                '}';
    }
}
