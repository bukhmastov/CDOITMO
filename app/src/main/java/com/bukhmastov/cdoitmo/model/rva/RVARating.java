package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RVARating extends JsonEntity {

    @JsonProperty("title")
    private String title;

    @JsonProperty("position")
    private String position;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("meta")
    private String meta;

    @JsonProperty("extra")
    private String extra;

    public RVARating() {
        super();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVARating)) return false;
        RVARating rvaRating = (RVARating) o;
        return Objects.equals(title, rvaRating.title) &&
                Objects.equals(position, rvaRating.position) &&
                Objects.equals(desc, rvaRating.desc) &&
                Objects.equals(meta, rvaRating.meta) &&
                Objects.equals(extra, rvaRating.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, position, desc, meta, extra);
    }

    @Override
    public String toString() {
        return "RVARating{" +
                "title='" + title + '\'' +
                ", position='" + position + '\'' +
                ", desc='" + desc + '\'' +
                ", meta='" + meta + '\'' +
                ", extra='" + extra + '\'' +
                '}';
    }
}
