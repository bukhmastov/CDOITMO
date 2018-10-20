package com.bukhmastov.cdoitmo.model.entity;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class Suggestions extends JsonEntity {

    @JsonProperty("suggestions")
    public ArrayList<String> suggestions;

    public Suggestions() {
        super();
    }

    public ArrayList<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(ArrayList<String> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Suggestions)) return false;
        Suggestions that = (Suggestions) o;
        return Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suggestions);
    }

    @Override
    public String toString() {
        return "Suggestions{" +
                "suggestions=" + suggestions +
                '}';
    }
}
