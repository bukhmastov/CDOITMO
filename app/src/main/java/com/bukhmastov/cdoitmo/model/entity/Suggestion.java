package com.bukhmastov.cdoitmo.model.entity;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class Suggestion extends JsonEntity {

    @JsonProperty("icon")
    public int icon;

    @JsonProperty("query")
    public String query;

    @JsonProperty("title")
    public String title;

    @JsonProperty("removable")
    public boolean removable;

    public Suggestion(String query, String title, int icon, boolean removable) {
        this.icon = icon;
        this.query = query;
        this.title = title;
        this.removable = removable;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Suggestion)) return false;
        Suggestion that = (Suggestion) o;
        return icon == that.icon &&
                removable == that.removable &&
                Objects.equals(query, that.query) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icon, query, title, removable);
    }

    @Override
    public String toString() {
        return "Suggestion{" +
                "icon=" + icon +
                ", query='" + query + '\'' +
                ", title='" + title + '\'' +
                ", removable=" + removable +
                '}';
    }
}