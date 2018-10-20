package com.bukhmastov.cdoitmo.model.widget.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class WSLSettings extends JsonEntity {

    @JsonProperty("query")
    private String query;

    @JsonProperty("theme")
    private WSLTheme theme;

    @JsonProperty("updateTime")
    private int updateTime;

    @JsonProperty("shift")
    private int shift;

    @JsonProperty("shiftAutomatic")
    private int shiftAutomatic;

    @JsonProperty("useShiftAutomatic")
    private boolean useShiftAutomatic;

    public WSLSettings() {
        super();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public WSLTheme getTheme() {
        return theme;
    }

    public void setTheme(WSLTheme theme) {
        this.theme = theme;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public int getShift() {
        return shift;
    }

    public void setShift(int shift) {
        this.shift = shift;
    }

    public int getShiftAutomatic() {
        return shiftAutomatic;
    }

    public void setShiftAutomatic(int shiftAutomatic) {
        this.shiftAutomatic = shiftAutomatic;
    }

    public boolean isUseShiftAutomatic() {
        return useShiftAutomatic;
    }

    public void setUseShiftAutomatic(boolean useShiftAutomatic) {
        this.useShiftAutomatic = useShiftAutomatic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WSLSettings)) return false;
        WSLSettings that = (WSLSettings) o;
        return updateTime == that.updateTime &&
                shift == that.shift &&
                shiftAutomatic == that.shiftAutomatic &&
                useShiftAutomatic == that.useShiftAutomatic &&
                Objects.equals(query, that.query) &&
                Objects.equals(theme, that.theme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, theme, updateTime, shift, shiftAutomatic, useShiftAutomatic);
    }

    @Override
    public String toString() {
        return "WSLSettings{" +
                "query='" + query + '\'' +
                ", theme=" + theme +
                ", updateTime=" + updateTime +
                ", shift=" + shift +
                ", shiftAutomatic=" + shiftAutomatic +
                ", useShiftAutomatic=" + useShiftAutomatic +
                '}';
    }
}
