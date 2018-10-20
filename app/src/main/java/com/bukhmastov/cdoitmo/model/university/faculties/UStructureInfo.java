package com.bukhmastov.cdoitmo.model.university.faculties;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UStructureInfo extends JsonEntity {

    @JsonProperty("short_title")
    private String titleShort;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("site")
    private String site;

    @JsonProperty("adres")
    private String address;

    @JsonProperty("dekanat_adres")
    private String deaneryAddress;

    @JsonProperty("dekanat_phone")
    private String deaneryPhone;

    @JsonProperty("dekanat_email")
    private String deaneryEmail;

    @JsonProperty("isu_otdel_id")
    private int isuOtdelId;

    @JsonProperty("isu_person_id")
    private int isuPersonId;

    @JsonProperty("ifmo_person_id")
    private int ifmoPersonId;

    @JsonProperty("lastname")
    private String lastName;

    @JsonProperty("firstname")
    private String firstName;

    @JsonProperty("middlename")
    private String middleName;

    @JsonProperty("person_rank")
    private String personRank;

    @JsonProperty("person_degree")
    private String personDegree;

    @JsonProperty("person_post")
    private String personPost;

    @JsonProperty("person_avatar")
    private String personAvatar;

    public UStructureInfo() {
        super();
    }

    public String getTitleShort() {
        return titleShort;
    }

    public void setTitleShort(String titleShort) {
        this.titleShort = titleShort;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDeaneryAddress() {
        return deaneryAddress;
    }

    public void setDeaneryAddress(String deaneryAddress) {
        this.deaneryAddress = deaneryAddress;
    }

    public String getDeaneryPhone() {
        return deaneryPhone;
    }

    public void setDeaneryPhone(String deaneryPhone) {
        this.deaneryPhone = deaneryPhone;
    }

    public String getDeaneryEmail() {
        return deaneryEmail;
    }

    public void setDeaneryEmail(String deaneryEmail) {
        this.deaneryEmail = deaneryEmail;
    }

    public int getIsuOtdelId() {
        return isuOtdelId;
    }

    public void setIsuOtdelId(int isuOtdelId) {
        this.isuOtdelId = isuOtdelId;
    }

    public int getIsuPersonId() {
        return isuPersonId;
    }

    public void setIsuPersonId(int isuPersonId) {
        this.isuPersonId = isuPersonId;
    }

    public int getIfmoPersonId() {
        return ifmoPersonId;
    }

    public void setIfmoPersonId(int ifmoPersonId) {
        this.ifmoPersonId = ifmoPersonId;
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

    public String getPersonRank() {
        return personRank;
    }

    public void setPersonRank(String personRank) {
        this.personRank = personRank;
    }

    public String getPersonDegree() {
        return personDegree;
    }

    public void setPersonDegree(String personDegree) {
        this.personDegree = personDegree;
    }

    public String getPersonPost() {
        return personPost;
    }

    public void setPersonPost(String personPost) {
        this.personPost = personPost;
    }

    public String getPersonAvatar() {
        return personAvatar;
    }

    public void setPersonAvatar(String personAvatar) {
        this.personAvatar = personAvatar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UStructureInfo)) return false;
        UStructureInfo that = (UStructureInfo) o;
        return isuOtdelId == that.isuOtdelId &&
                isuPersonId == that.isuPersonId &&
                ifmoPersonId == that.ifmoPersonId &&
                Objects.equals(titleShort, that.titleShort) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(email, that.email) &&
                Objects.equals(site, that.site) &&
                Objects.equals(address, that.address) &&
                Objects.equals(deaneryAddress, that.deaneryAddress) &&
                Objects.equals(deaneryPhone, that.deaneryPhone) &&
                Objects.equals(deaneryEmail, that.deaneryEmail) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(middleName, that.middleName) &&
                Objects.equals(personRank, that.personRank) &&
                Objects.equals(personDegree, that.personDegree) &&
                Objects.equals(personPost, that.personPost) &&
                Objects.equals(personAvatar, that.personAvatar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleShort, phone, email, site, address, deaneryAddress, deaneryPhone, deaneryEmail, isuOtdelId, isuPersonId, ifmoPersonId, lastName, firstName, middleName, personRank, personDegree, personPost, personAvatar);
    }

    @Override
    public String toString() {
        return "UStructureInfo{" +
                "titleShort='" + titleShort + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", site='" + site + '\'' +
                ", address='" + address + '\'' +
                ", deaneryAddress='" + deaneryAddress + '\'' +
                ", deaneryPhone='" + deaneryPhone + '\'' +
                ", deaneryEmail='" + deaneryEmail + '\'' +
                ", isuOtdelId=" + isuOtdelId +
                ", isuPersonId=" + isuPersonId +
                ", ifmoPersonId=" + ifmoPersonId +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", personRank='" + personRank + '\'' +
                ", personDegree='" + personDegree + '\'' +
                ", personPost='" + personPost + '\'' +
                ", personAvatar='" + personAvatar + '\'' +
                '}';
    }
}
