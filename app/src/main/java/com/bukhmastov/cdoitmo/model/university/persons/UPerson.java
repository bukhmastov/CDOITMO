package com.bukhmastov.cdoitmo.model.university.persons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UPerson extends JsonEntity {

    @JsonProperty("persons_id")
    private int id;

    @JsonProperty("title_l")
    private String lastName;

    @JsonProperty("title_f")
    private String firstName;

    @JsonProperty("title_m")
    private String middleName;

    @JsonProperty("text")
    private String text;

    @JsonProperty("output")
    private boolean isOutput;

    @JsonProperty("public")
    private boolean isPublic;

    @JsonProperty("status")
    private String status;

    @JsonProperty("cis_id")
    private String cisId;

    @JsonProperty("www")
    private String www;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("post")
    private String post;

    @JsonProperty("rank")
    private String rank;

    @JsonProperty("degree")
    private String degree;

    @JsonProperty("year")
    private String year;

    @JsonProperty("pub_art")
    private boolean isPubArt;

    @JsonProperty("lastup")
    private String lastUp;

    @JsonProperty("image")
    private String image;

    public UPerson() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isOutput() {
        return isOutput;
    }

    public void setOutput(boolean output) {
        isOutput = output;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCisId() {
        return cisId;
    }

    public void setCisId(String cisId) {
        this.cisId = cisId;
    }

    public String getWww() {
        return www;
    }

    public void setWww(String www) {
        this.www = www;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public boolean isPubArt() {
        return isPubArt;
    }

    public void setPubArt(boolean pubArt) {
        isPubArt = pubArt;
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
        if (!(o instanceof UPerson)) return false;
        UPerson uPerson = (UPerson) o;
        return id == uPerson.id &&
                Objects.equals(lastName, uPerson.lastName) &&
                Objects.equals(firstName, uPerson.firstName) &&
                Objects.equals(middleName, uPerson.middleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lastName, firstName, middleName);
    }

    @Override
    public String toString() {
        return "UPerson{" +
                "id=" + id +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", text='" + text + '\'' +
                ", isOutput=" + isOutput +
                ", isPublic=" + isPublic +
                ", status='" + status + '\'' +
                ", cisId='" + cisId + '\'' +
                ", www='" + www + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", post='" + post + '\'' +
                ", rank='" + rank + '\'' +
                ", degree='" + degree + '\'' +
                ", year='" + year + '\'' +
                ", isPubArt=" + isPubArt +
                ", lastUp='" + lastUp + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
