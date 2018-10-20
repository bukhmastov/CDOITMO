package com.bukhmastov.cdoitmo.model.entity;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class ShortcutQuery extends JsonEntity {

    @JsonProperty("query")
    private String query;

    @JsonProperty("label")
    private String label;

    public ShortcutQuery() {
        super();
    }

    public ShortcutQuery(String query, String label) {
        super();
        this.query = query;
        this.label = label;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortcutQuery)) return false;
        ShortcutQuery that = (ShortcutQuery) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, label);
    }

    @Override
    public String toString() {
        return "ShortcutQuery{" +
                "query='" + query + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
