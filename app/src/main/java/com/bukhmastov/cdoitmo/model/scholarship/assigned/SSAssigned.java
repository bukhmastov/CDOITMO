package com.bukhmastov.cdoitmo.model.scholarship.assigned;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SSAssigned extends JsonEntity {

    @JsonProperty("educType")
    private String type;

    @JsonProperty("contribution")
    private String contribution;

    @JsonProperty("source")
    private String source;

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    @JsonProperty("sum")
    private String sum;

    public SSAssigned() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContribution() {
        return contribution;
    }

    public void setContribution(String contribution) {
        this.contribution = contribution;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSAssigned)) return false;
        SSAssigned that = (SSAssigned) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(contribution, that.contribution) &&
                Objects.equals(source, that.source) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end) &&
                Objects.equals(sum, that.sum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, contribution, source, start, end, sum);
    }

    @Override
    public String toString() {
        return "SSAssigned{" +
                "type='" + type + '\'' +
                ", contribution='" + contribution + '\'' +
                ", source='" + source + '\'' +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                ", sum='" + sum + '\'' +
                '}';
    }
}
