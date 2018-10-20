package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class FSLessons extends JsonEntity {

    @JsonProperty("type")
    private String type;

    @JsonProperty("version")
    private int version;

    @JsonProperty("content")
    private FSLessonsContent content;

    public FSLessons() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public FSLessonsContent getContent() {
        return content;
    }

    public void setContent(FSLessonsContent content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSLessons)) return false;
        FSLessons fsLessons = (FSLessons) o;
        return version == fsLessons.version &&
                Objects.equals(type, fsLessons.type) &&
                Objects.equals(content, fsLessons.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version, content);
    }

    @Override
    public String toString() {
        return "FSLessons{" +
                "type='" + type + '\'' +
                ", version=" + version +
                ", content=" + content +
                '}';
    }
}
