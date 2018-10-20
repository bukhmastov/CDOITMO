package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.protocol.PChange;

import java.util.ArrayList;
import java.util.Objects;

public class RVAProtocolGroup extends JsonEntity {

    @JsonProperty("token")
    private String token;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("changes")
    private ArrayList<PChange> changes;

    public RVAProtocolGroup() {
        super();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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
        if (!(o instanceof RVAProtocolGroup)) return false;
        RVAProtocolGroup group = (RVAProtocolGroup) o;
        return Objects.equals(token, group.token) &&
                Objects.equals(subject, group.subject) &&
                Objects.equals(changes, group.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, subject, changes);
    }

    @Override
    public String toString() {
        return "RVAProtocolGroup{" +
                "token='" + token + '\'' +
                ", subject='" + subject + '\'' +
                ", changes=" + changes +
                '}';
    }
}
