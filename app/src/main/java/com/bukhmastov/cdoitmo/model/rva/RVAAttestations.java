package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SSubject;

import java.util.ArrayList;
import java.util.Objects;

public class RVAAttestations extends JsonEntity {

    @JsonProperty("subjects")
    private ArrayList<SSubject> subjects;

    public RVAAttestations() {
        super();
    }

    // menu share click
    public RVAAttestations(ArrayList<SSubject> subjects) {
        this.subjects = subjects;
    }

    public ArrayList<SSubject> getSubjects() {
        return subjects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVAAttestations)) return false;
        RVAAttestations that = (RVAAttestations) o;
        return Objects.equals(subjects, that.subjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjects);
    }

    @Override
    public String toString() {
        return "RVAAttestations{" +
                "subjects=" + subjects +
                '}';
    }
}
