package com.bukhmastov.cdoitmo.model.university.persons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UPersons extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("lastname")
    private String lastName;

    @JsonProperty("search")
    private String search;

    @JsonProperty("public")
    private int isPublic;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("count")
    private int count;

    @JsonProperty("list")
    private ArrayList<UPerson> people;

    public UPersons() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public int getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(int isPublic) {
        this.isPublic = isPublic;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ArrayList<UPerson> getPeople() {
        return people;
    }

    public void setPeople(ArrayList<UPerson> people) {
        this.people = people;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UPersons)) return false;
        UPersons uPersons = (UPersons) o;
        return timestamp == uPersons.timestamp &&
                isPublic == uPersons.isPublic &&
                limit == uPersons.limit &&
                offset == uPersons.offset &&
                count == uPersons.count &&
                Objects.equals(lastName, uPersons.lastName) &&
                Objects.equals(search, uPersons.search) &&
                Objects.equals(people, uPersons.people);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, lastName, search, isPublic, limit, offset, count, people);
    }

    @Override
    public String toString() {
        return "UPersons{" +
                "timestamp=" + timestamp +
                ", lastName='" + lastName + '\'' +
                ", search='" + search + '\'' +
                ", isPublic=" + isPublic +
                ", limit=" + limit +
                ", offset=" + offset +
                ", count=" + count +
                ", people=" + people +
                '}';
    }
}
