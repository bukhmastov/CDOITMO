package com.bukhmastov.cdoitmo.model.university.news;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UNewsItem extends JsonEntity {

    @JsonProperty("news_id")
    private int id;

    @JsonProperty("news_title")
    private String title;

    @JsonProperty("link")
    private String link;

    @JsonProperty("anons")
    private String anons;

    @JsonProperty("text")
    private String text;

    @JsonProperty("data")
    private String data;

    @JsonProperty("status")
    private int status;

    @JsonProperty("news_status_id")
    private int statusId;

    @JsonProperty("data_start")
    private String dataStart;

    @JsonProperty("data_end")
    private String dataEnd;

    @JsonProperty("blok_comment")
    private int blockComment;

    @JsonProperty("album_id")
    private int albumId;

    @JsonProperty("video_online")
    private String videoOnline;

    @JsonProperty("pos")
    private int pos;

    @JsonProperty("output")
    private int output;

    @JsonProperty("photo")
    private String photo;

    @JsonProperty("count_view")
    private int viewCount;

    @JsonProperty("genre")
    private int genre;

    @JsonProperty("pic_copy")
    private String picCopy;

    @JsonProperty("news_category_id")
    private int categoryId;

    @JsonProperty("author_id")
    private int authorId;

    @JsonProperty("img")
    private String img;

    @JsonProperty("img_small")
    private String imgSmall;

    // "keywords": null

    @JsonProperty("creation_date")
    private String dateCreation;

    @JsonProperty("pub_date")
    private String datePublication;

    @JsonProperty("lastup")
    private String lastUp;

    @JsonProperty("category_child")
    private String categoryChild;

    @JsonProperty("category_parent")
    private String categoryParent;

    @JsonProperty("color_hex")
    private String colorHex;

    @JsonProperty("url_original")
    private String urlOriginal;

    @JsonProperty("url_webview")
    private String urlWebview;

    @JsonProperty("main")
    private boolean isMain;

    public UNewsItem() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getAnons() {
        return anons;
    }

    public void setAnons(String anons) {
        this.anons = anons;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public String getDataStart() {
        return dataStart;
    }

    public void setDataStart(String dataStart) {
        this.dataStart = dataStart;
    }

    public String getDataEnd() {
        return dataEnd;
    }

    public void setDataEnd(String dataEnd) {
        this.dataEnd = dataEnd;
    }

    public int getBlockComment() {
        return blockComment;
    }

    public void setBlockComment(int blockComment) {
        this.blockComment = blockComment;
    }

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public String getVideoOnline() {
        return videoOnline;
    }

    public void setVideoOnline(String videoOnline) {
        this.videoOnline = videoOnline;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getOutput() {
        return output;
    }

    public void setOutput(int output) {
        this.output = output;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getGenre() {
        return genre;
    }

    public void setGenre(int genre) {
        this.genre = genre;
    }

    public String getPicCopy() {
        return picCopy;
    }

    public void setPicCopy(String picCopy) {
        this.picCopy = picCopy;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getImgSmall() {
        return imgSmall;
    }

    public void setImgSmall(String imgSmall) {
        this.imgSmall = imgSmall;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(String datePublication) {
        this.datePublication = datePublication;
    }

    public String getLastUp() {
        return lastUp;
    }

    public void setLastUp(String lastUp) {
        this.lastUp = lastUp;
    }

    public String getCategoryChild() {
        return categoryChild;
    }

    public void setCategoryChild(String categoryChild) {
        this.categoryChild = categoryChild;
    }

    public String getCategoryParent() {
        return categoryParent;
    }

    public void setCategoryParent(String categoryParent) {
        this.categoryParent = categoryParent;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
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

    public boolean isMain() {
        return isMain;
    }

    public void setMain(boolean main) {
        isMain = main;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UNewsItem)) return false;
        UNewsItem uNewsItem = (UNewsItem) o;
        return id == uNewsItem.id &&
                Objects.equals(title, uNewsItem.title) &&
                Objects.equals(urlWebview, uNewsItem.urlWebview);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, urlWebview);
    }

    @Override
    public String toString() {
        return "UNewsItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", anons='" + anons + '\'' +
                ", text='" + text + '\'' +
                ", data='" + data + '\'' +
                ", status=" + status +
                ", statusId=" + statusId +
                ", dataStart='" + dataStart + '\'' +
                ", dataEnd='" + dataEnd + '\'' +
                ", blockComment=" + blockComment +
                ", albumId=" + albumId +
                ", videoOnline='" + videoOnline + '\'' +
                ", pos=" + pos +
                ", output=" + output +
                ", photo='" + photo + '\'' +
                ", viewCount=" + viewCount +
                ", genre=" + genre +
                ", picCopy='" + picCopy + '\'' +
                ", categoryId=" + categoryId +
                ", authorId=" + authorId +
                ", img='" + img + '\'' +
                ", imgSmall='" + imgSmall + '\'' +
                ", dateCreation='" + dateCreation + '\'' +
                ", datePublication='" + datePublication + '\'' +
                ", lastUp='" + lastUp + '\'' +
                ", categoryChild='" + categoryChild + '\'' +
                ", categoryParent='" + categoryParent + '\'' +
                ", colorHex='" + colorHex + '\'' +
                ", urlOriginal='" + urlOriginal + '\'' +
                ", urlWebview='" + urlWebview + '\'' +
                ", isMain=" + isMain +
                '}';
    }
}