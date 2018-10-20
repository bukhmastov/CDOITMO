package com.bukhmastov.cdoitmo.model.user;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UsersList extends JsonEntity {

    @JsonProperty("logins")
    private ArrayList<String> logins;

    public UsersList() {
        super();
    }

    public ArrayList<String> getLogins() {
        return logins;
    }

    public void setLogins(ArrayList<String> logins) {
        this.logins = logins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsersList)) return false;
        UsersList usersList = (UsersList) o;
        return Objects.equals(logins, usersList.logins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logins);
    }

    @Override
    public String toString() {
        return "UsersList{" +
                "logins=" + logins +
                '}';
    }
}
