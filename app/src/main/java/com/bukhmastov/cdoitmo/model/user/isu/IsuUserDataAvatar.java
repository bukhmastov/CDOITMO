package com.bukhmastov.cdoitmo.model.user.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class IsuUserDataAvatar extends JsonEntity {

    @JsonProperty("url")
    private String url;

    @JsonProperty("mime")
    private String mime;

    public String getUrl() {
        return url;
    }

    public String getMime() {
        return mime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IsuUserDataAvatar)) return false;
        IsuUserDataAvatar that = (IsuUserDataAvatar) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(mime, that.mime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, mime);
    }

    @Override
    public String toString() {
        return "IsuUserDataAvatar{" +
                "url='" + url + '\'' +
                ", mime='" + mime + '\'' +
                '}';
    }
}
