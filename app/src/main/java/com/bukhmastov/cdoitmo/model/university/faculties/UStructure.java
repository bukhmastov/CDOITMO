package com.bukhmastov.cdoitmo.model.university.faculties;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UStructure extends JsonEntity {

    @JsonProperty("cis_dep_id")
    private int cisDepId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("id_type")
    private int typeId;

    @JsonProperty("link")
    private String link;

    @JsonProperty("info")
    private UStructureInfo structureInfo;

    public UStructure() {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public UStructureInfo getStructureInfo() {
        return structureInfo;
    }

    public void setStructureInfo(UStructureInfo structureInfo) {
        this.structureInfo = structureInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UStructure)) return false;
        UStructure that = (UStructure) o;
        return cisDepId == that.cisDepId &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cisDepId, name);
    }

    @Override
    public String toString() {
        return "UStructure{" +
                "cisDepId=" + cisDepId +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", typeId=" + typeId +
                ", link='" + link + '\'' +
                ", structureInfo=" + structureInfo +
                '}';
    }
}
