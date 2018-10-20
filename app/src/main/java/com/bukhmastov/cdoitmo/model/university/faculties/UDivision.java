package com.bukhmastov.cdoitmo.model.university.faculties;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UDivision extends JsonEntity {

    @JsonProperty("cis_dep_id")
    private int cisDepId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("faculties_id")
    private int facultiesId;

    @JsonProperty("departments_id")
    private int departmentsId;

    @JsonProperty("unit_id")
    private int unitId;

    @JsonProperty("sub_units")
    private boolean isSubUnits;

    @JsonProperty("link")
    private String link;

    public UDivision() {
        super();
    }

    public int getCisDepId() {
        return cisDepId;
    }

    public void setCisDepId(int cisDepId) {
        this.cisDepId = cisDepId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFacultiesId() {
        return facultiesId;
    }

    public void setFacultiesId(int facultiesId) {
        this.facultiesId = facultiesId;
    }

    public int getDepartmentsId() {
        return departmentsId;
    }

    public void setDepartmentsId(int departmentsId) {
        this.departmentsId = departmentsId;
    }

    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    public boolean isSubUnits() {
        return isSubUnits;
    }

    public void setSubUnits(boolean subUnits) {
        isSubUnits = subUnits;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UDivision)) return false;
        UDivision uDivision = (UDivision) o;
        return cisDepId == uDivision.cisDepId &&
                Objects.equals(name, uDivision.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cisDepId, name);
    }

    @Override
    public String toString() {
        return "UDivision{" +
                "cisDepId=" + cisDepId +
                ", name='" + name + '\'' +
                ", facultiesId=" + facultiesId +
                ", departmentsId=" + departmentsId +
                ", unitId=" + unitId +
                ", isSubUnits=" + isSubUnits +
                ", link='" + link + '\'' +
                '}';
    }
}
