package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class FSLReduced extends JsonEntity {

    @JsonProperty("hash")
    private String hash;

    public FSLReduced() {
        super();
    }

    public FSLReduced(String hash) {
        super();
        setHash(hash);
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSLReduced)) return false;
        FSLReduced that = (FSLReduced) o;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public String toString() {
        return "FSLReduced{" +
                "hash='" + hash + '\'' +
                '}';
    }
}
