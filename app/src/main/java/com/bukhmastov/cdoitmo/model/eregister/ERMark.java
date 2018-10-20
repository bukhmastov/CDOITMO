package com.bukhmastov.cdoitmo.model.eregister;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class ERMark extends JsonEntity {

    @JsonProperty("mark")
    private String mark;

    @JsonProperty("markdate")
    private String markDate;

    @JsonProperty("worktype")
    private String workType;

    public ERMark() {
        super();
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getMarkDate() {
        return markDate;
    }

    public void setMarkDate(String markDate) {
        this.markDate = markDate;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ERMark)) return false;
        ERMark erMark = (ERMark) o;
        return Objects.equals(mark, erMark.mark) &&
                Objects.equals(markDate, erMark.markDate) &&
                Objects.equals(workType, erMark.workType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mark, markDate, workType);
    }

    @Override
    public String toString() {
        return "ERMark{" +
                "mark='" + mark + '\'' +
                ", markDate='" + markDate + '\'' +
                ", workType='" + workType + '\'' +
                '}';
    }
}
