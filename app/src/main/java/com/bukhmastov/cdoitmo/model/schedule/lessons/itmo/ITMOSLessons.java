package com.bukhmastov.cdoitmo.model.schedule.lessons.itmo;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Model, obtained from _ITMO_ servers
 */
public class ITMOSLessons extends JsonEntity {

    @JsonProperty("query")
    private String query;

    @JsonProperty("label")
    private String label;

    @JsonProperty("type_title")
    private String typeTitle;

    @JsonProperty("schedule")
    private ArrayList<ITMOSLesson> schedule;

    public ITMOSLessons() {
        super();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTypeTitle() {
        return typeTitle;
    }

    public void setTypeTitle(String typeTitle) {
        this.typeTitle = typeTitle;
    }

    public ArrayList<ITMOSLesson> getSchedule() {
        return schedule;
    }

    public void setSchedule(ArrayList<ITMOSLesson> schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ITMOSLessons)) return false;
        ITMOSLessons that = (ITMOSLessons) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(label, that.label) &&
                Objects.equals(typeTitle, that.typeTitle) &&
                Objects.equals(schedule, that.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, label, typeTitle, schedule);
    }

    @Override
    public String toString() {
        return "ITMOSLessons{" +
                "query='" + query + '\'' +
                ", label='" + label + '\'' +
                ", typeTitle='" + typeTitle + '\'' +
                ", schedule=" + schedule +
                '}';
    }
}

/*
"query":105840,
"label":"\u0417\u0438\u043d\u0447\u0438\u043a \u0410\u043b\u0435\u043a\u0441\u0430\u043d\u0434\u0440 \u0410\u0434\u043e\u043b\u044c\u0444\u043e\u0432\u0438\u0447",
"type_title":"\u0420\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u043f\u0440\u0435\u043f\u043e\u0434\u0430\u0432\u0430\u0442\u0435\u043b\u044f",
"pid":105840,
"today":"2018-10-07",
"today_data_day":6,
"current_week":6,
 */