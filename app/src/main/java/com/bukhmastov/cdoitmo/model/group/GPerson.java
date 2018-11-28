package com.bukhmastov.cdoitmo.model.group;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class GPerson extends JsonEntity {

    @JsonProperty("rn")
    private int number;

    @JsonProperty("id")
    private int personId;

    @JsonProperty("photoUrl")
    private String photoUrl;

    @JsonProperty("url")
    private String url;

    @JsonProperty("fio")
    private String fio;

    @JsonProperty("sciTitle")
    private String sciTitle;

    @JsonProperty("sciGrade")
    private String sciGrade;

    public GPerson() {
        super();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getSciTitle() {
        return sciTitle;
    }

    public void setSciTitle(String sciTitle) {
        this.sciTitle = sciTitle;
    }

    public String getSciGrade() {
        return sciGrade;
    }

    public void setSciGrade(String sciGrade) {
        this.sciGrade = sciGrade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPerson)) return false;
        GPerson that = (GPerson) o;
        return personId == that.personId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(personId);
    }

    @Override
    public String toString() {
        return "GPerson{" +
                "number=" + number +
                ", personId=" + personId +
                ", photoUrl='" + photoUrl + '\'' +
                ", url='" + url + '\'' +
                ", fio='" + fio + '\'' +
                ", sciTitle='" + sciTitle + '\'' +
                ", sciGrade='" + sciGrade + '\'' +
                '}';
    }
}
