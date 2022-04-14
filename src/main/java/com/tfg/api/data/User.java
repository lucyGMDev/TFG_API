package com.tfg.api.data;

import java.util.Date;

public class User {
  private String email;
  private String username;
  private String name;
  private String lastName;
  private Date createdDate;
  private String pictureUrl;

  public User() {
  }

  public User(String email, String username, String name, String lastName, Date createdDate, String pictureUrl) {
    this.email = email;
    this.username = username;
    this.name = name;
    this.lastName = lastName;
    this.createdDate = createdDate;
    this.pictureUrl = pictureUrl;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getPictureUrl() {
    return pictureUrl;
  }

  public void setPictureUrl(String pictureUrl) {
    this.pictureUrl = pictureUrl;
  }

}
