package com.bukhmastov.cdoitmo.model.user;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UserData extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("group")
    private String group;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("week")
    private int week;

    public UserData() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserData)) return false;
        UserData userData = (UserData) o;
        return week == userData.week &&
                Objects.equals(name, userData.name) &&
                Objects.equals(group, userData.group) &&
                Objects.equals(avatar, userData.avatar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, group, avatar, week);
    }

    @Override
    public String toString() {
        return "UserData{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", avatar='" + avatar + '\'' +
                ", week=" + week +
                '}';
    }
}
