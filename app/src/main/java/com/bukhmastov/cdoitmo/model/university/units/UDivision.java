package com.bukhmastov.cdoitmo.model.university.units;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UDivision extends JsonEntity {

    @JsonProperty("unit_id")
    private int id;

    @JsonProperty("unit_title")
    private String title;

    public UDivision() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UDivision)) return false;
        UDivision uDivision = (UDivision) o;
        return id == uDivision.id &&
                Objects.equals(title, uDivision.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }

    @Override
    public String toString() {
        return "UDivision{" +
                "id=" + id +
                ", title='" + title + '\'' +
                '}';
    }
}
