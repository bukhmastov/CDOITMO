package com.bukhmastov.cdoitmo.model.entity;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SettingsQuery extends JsonEntity {

    @JsonProperty("query")
    private String query;

    @JsonProperty("title")
    private String title;

    public SettingsQuery() {
        super();
    }

    public SettingsQuery(String query, String title) {
        super();
        this.query = query;
        this.title = title;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SettingsQuery)) return false;
        SettingsQuery that = (SettingsQuery) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, title);
    }

    @Override
    public String toString() {
        return "SettingsQuery{" +
                "query='" + query + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
