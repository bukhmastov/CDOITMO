package com.bukhmastov.cdoitmo.model.schedule.teachers;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class STeacher extends JsonEntity {

    @JsonProperty("person")
    private String person;

    @JsonProperty("post")
    private String post;

    @JsonProperty("pid")
    private int personId;

    public STeacher() {
        super();
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public String getPersonId() {
        return String.valueOf(personId);
    }

    public int getPersonIdAsNumber() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof STeacher)) return false;
        STeacher sTeacher = (STeacher) o;
        return Objects.equals(person, sTeacher.person) &&
                Objects.equals(post, sTeacher.post) &&
                Objects.equals(personId, sTeacher.personId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(person, post, personId);
    }

    @Override
    public String toString() {
        return "STeacher{" +
                "person='" + person + '\'' +
                ", post='" + post + '\'' +
                ", personId=" + personId +
                '}';
    }
}
