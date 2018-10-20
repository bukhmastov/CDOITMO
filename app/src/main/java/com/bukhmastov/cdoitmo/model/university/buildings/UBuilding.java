package com.bukhmastov.cdoitmo.model.university.buildings;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UBuilding extends JsonEntity {

    @JsonProperty("id")
    private int id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("text")
    private String text;

    @JsonProperty("N")
    private double n;

    @JsonProperty("E")
    private double e;

    @JsonProperty("type_id")
    private int typeId;

    @JsonProperty("major")
    private int major;

    @JsonProperty("pos")
    private int pos;

    @JsonProperty("lastup")
    private String lastUp;

    @JsonProperty("image")
    private String image;

    public UBuilding() {
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getN() {
        return n;
    }

    public void setN(double n) {
        this.n = n;
    }

    public double getE() {
        return e;
    }

    public void setE(double e) {
        this.e = e;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getLastUp() {
        return lastUp;
    }

    public void setLastUp(String lastUp) {
        this.lastUp = lastUp;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UBuilding)) return false;
        UBuilding uBuilding = (UBuilding) o;
        return id == uBuilding.id &&
                Double.compare(uBuilding.n, n) == 0 &&
                Double.compare(uBuilding.e, e) == 0 &&
                typeId == uBuilding.typeId &&
                major == uBuilding.major &&
                pos == uBuilding.pos &&
                Objects.equals(title, uBuilding.title) &&
                Objects.equals(text, uBuilding.text) &&
                Objects.equals(lastUp, uBuilding.lastUp) &&
                Objects.equals(image, uBuilding.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, text, n, e, typeId, major, pos, lastUp, image);
    }

    @Override
    public String toString() {
        return "UBuilding{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", n=" + n +
                ", e=" + e +
                ", typeId=" + typeId +
                ", major=" + major +
                ", pos=" + pos +
                ", lastUp='" + lastUp + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
