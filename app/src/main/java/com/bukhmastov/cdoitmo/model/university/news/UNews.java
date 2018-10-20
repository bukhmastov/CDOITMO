package com.bukhmastov.cdoitmo.model.university.news;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UNews extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("search")
    private String search;

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("count")
    private int count;

    @JsonProperty("list")
    private ArrayList<UNewsItem> newsItems;

    public UNews() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public ArrayList<UNewsItem> getNewsItems() {
        return newsItems;
    }

    public void setNewsItems(ArrayList<UNewsItem> newsItems) {
        this.newsItems = newsItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UNews)) return false;
        UNews uNews = (UNews) o;
        return timestamp == uNews.timestamp &&
                limit == uNews.limit &&
                offset == uNews.offset &&
                count == uNews.count &&
                Objects.equals(search, uNews.search) &&
                Objects.equals(tag, uNews.tag) &&
                Objects.equals(newsItems, uNews.newsItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, search, tag, limit, offset, count, newsItems);
    }

    @Override
    public String toString() {
        return "UNews{" +
                "timestamp=" + timestamp +
                ", search='" + search + '\'' +
                ", tag='" + tag + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", count=" + count +
                ", newsItems=" + newsItems +
                '}';
    }
}
