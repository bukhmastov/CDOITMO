package com.bukhmastov.cdoitmo.model.schedule.attestations;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SSubject extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("term")
    private int term;

    @JsonProperty("attestations")
    private ArrayList<SAttestation> attestations;

    public SSubject() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public ArrayList<SAttestation> getAttestations() {
        return attestations;
    }

    public void setAttestations(ArrayList<SAttestation> attestations) {
        this.attestations = attestations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSubject)) return false;
        SSubject sSubject = (SSubject) o;
        return term == sSubject.term &&
                Objects.equals(name, sSubject.name) &&
                Objects.equals(attestations, sSubject.attestations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, term, attestations);
    }

    @Override
    public String toString() {
        return "SSubject{" +
                "name='" + name + '\'' +
                ", term=" + term +
                ", attestations=" + attestations +
                '}';
    }
}
