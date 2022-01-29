package com.tfg.api.data.bodies;

public class ProjectBody {
  private String name;
  private String description;
  private Boolean isPublic;
  private String owner;
  private String[] coauthors;
  public ProjectBody() {
  }

  public ProjectBody(String name, String description, Boolean isPublic, String owner, String[] coauthors) {
    this.name = name;
    this.description = description;
    this.isPublic = isPublic;
    this.owner = owner;
    this.coauthors = coauthors;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public String getOwner() {
    if(owner == null) return "";
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String[] getCoauthors() {
    return coauthors;
  }

  public void setCoauthors(String[] coauthors) {
    this.coauthors = coauthors;
  }

}
