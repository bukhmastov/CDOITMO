package com.bukhmastov.cdoitmo.model.firebase.config;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class FBConfigMessage extends JsonEntity {

    @JsonProperty("type")
    private int type;

    @JsonProperty("message")
    private String message;

    public FBConfigMessage() {
        super();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FBConfigMessage)) return false;
        FBConfigMessage fbConfig = (FBConfigMessage) o;
        return type == fbConfig.type &&
                Objects.equals(message, fbConfig.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, message);
    }

    @Override
    public String toString() {
        return "FBConfigMessage{" +
                "type=" + type +
                ", message='" + message + '\'' +
                '}';
    }
}
