package com.bukhmastov.cdoitmo.model.room101.request;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class Room101Request extends JsonEntity {

    @JsonProperty("type")
    private String type;

    @JsonProperty("done")
    private boolean done;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private ArrayList<ROption> options;

    public Room101Request() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArrayList<ROption> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<ROption> options) {
        this.options = options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room101Request)) return false;
        Room101Request that = (Room101Request) o;
        return done == that.done &&
                Objects.equals(type, that.type) &&
                Objects.equals(message, that.message) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, done, message, options);
    }

    @Override
    public String toString() {
        return "Room101Request{" +
                "type='" + type + '\'' +
                ", done=" + done +
                ", message='" + message + '\'' +
                ", options=" + options +
                '}';
    }
}
