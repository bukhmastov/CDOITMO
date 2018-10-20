package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class FSLessonsContent extends JsonEntity {

    @JsonProperty("query")
    private String query;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("added")
    private ArrayList<FSLAddedDay> added;

    @JsonProperty("reduced")
    private ArrayList<FSLReducedDay> reduced;

    public FSLessonsContent() {
        super();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<FSLAddedDay> getAdded() {
        return added;
    }

    public void setAdded(ArrayList<FSLAddedDay> added) {
        this.added = added;
    }

    public ArrayList<FSLReducedDay> getReduced() {
        return reduced;
    }

    public void setReduced(ArrayList<FSLReducedDay> reduced) {
        this.reduced = reduced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSLessonsContent)) return false;
        FSLessonsContent that = (FSLessonsContent) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(added, that.added) &&
                Objects.equals(reduced, that.reduced);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, type, title, added, reduced);
    }

    @Override
    public String toString() {
        return "FSLessonsContent{" +
                "query='" + query + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", added=" + added +
                ", reduced=" + reduced +
                '}';
    }
}
