package com.bukhmastov.cdoitmo.model.user.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class IsuUserData extends JsonEntity {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("name")
    private String name;

    @JsonProperty("patronymic")
    private String patronymic;

    @JsonProperty("sex")
    private String sex;

    @JsonProperty("avatar")
    private IsuUserDataAvatar avatar;

    @JsonProperty("groups")
    private ArrayList<IsuUserDataGroup> groups;

    public Integer getId() {
        return id;
    }

    public String getSurname() {
        return surname;
    }

    public String getName() {
        return name;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public String getSex() {
        return sex;
    }

    public IsuUserDataAvatar getAvatar() {
        return avatar;
    }

    public ArrayList<IsuUserDataGroup> getGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IsuUserData)) return false;
        IsuUserData that = (IsuUserData) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(surname, that.surname) &&
                Objects.equals(name, that.name) &&
                Objects.equals(patronymic, that.patronymic) &&
                Objects.equals(sex, that.sex) &&
                Objects.equals(avatar, that.avatar) &&
                Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, surname, name, patronymic, sex, avatar, groups);
    }

    @Override
    public String toString() {
        return "IsuUserData{" +
                "id=" + id +
                ", surname='" + surname + '\'' +
                ", name='" + name + '\'' +
                ", patronymic='" + patronymic + '\'' +
                ", sex='" + sex + '\'' +
                ", avatar=" + avatar +
                ", groups=" + groups +
                '}';
    }
}
