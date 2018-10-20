package com.bukhmastov.cdoitmo.model.university.events;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UEvent extends JsonEntity {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type_name")
    private String typeName;

    @JsonProperty("status_name")
    private String statusName;

    @JsonProperty("date_begin")
    private String dateBegin;

    @JsonProperty("date_end")
    private String dateEnd;

    @JsonProperty("preview")
    private String preview;

    @JsonProperty("logo")
    private String logo;

    @JsonProperty("lang_id")
    private int langId;

    @JsonProperty("m_prov_t_id")
    private int mProvTId;

    @JsonProperty("url_original")
    private String urlOriginal;

    @JsonProperty("url_webview")
    private String urlWebview;

    public UEvent() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(String dateBegin) {
        this.dateBegin = dateBegin;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public int getLangId() {
        return langId;
    }

    public void setLangId(int langId) {
        this.langId = langId;
    }

    public int getmProvTId() {
        return mProvTId;
    }

    public void setmProvTId(int mProvTId) {
        this.mProvTId = mProvTId;
    }

    public String getUrlOriginal() {
        return urlOriginal;
    }

    public void setUrlOriginal(String urlOriginal) {
        this.urlOriginal = urlOriginal;
    }

    public String getUrlWebview() {
        return urlWebview;
    }

    public void setUrlWebview(String urlWebview) {
        this.urlWebview = urlWebview;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UEvent)) return false;
        UEvent uEvent = (UEvent) o;
        return id == uEvent.id &&
                Objects.equals(name, uEvent.name) &&
                Objects.equals(typeName, uEvent.typeName) &&
                Objects.equals(statusName, uEvent.statusName) &&
                Objects.equals(dateBegin, uEvent.dateBegin) &&
                Objects.equals(dateEnd, uEvent.dateEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, typeName, statusName, dateBegin, dateEnd);
    }

    @Override
    public String toString() {
        return "UEvent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", typeName='" + typeName + '\'' +
                ", statusName='" + statusName + '\'' +
                ", dateBegin='" + dateBegin + '\'' +
                ", dateEnd='" + dateEnd + '\'' +
                ", preview='" + preview + '\'' +
                ", logo='" + logo + '\'' +
                ", langId=" + langId +
                ", mProvTId=" + mProvTId +
                ", urlOriginal='" + urlOriginal + '\'' +
                ", urlWebview='" + urlWebview + '\'' +
                '}';
    }
}
