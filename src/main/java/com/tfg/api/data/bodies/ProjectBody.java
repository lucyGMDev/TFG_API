package com.tfg.api.data.bodies;

public class ProjectBody {
  private String name;
  private String description;
  private Boolean isPublic;
  private String[] coauthors;
  private String[] type;

  public ProjectBody() {
  }

  public ProjectBody(String name, String description, Boolean isPublic, String[] coauthors,
      String[] type) {
    this.name = name;
    this.description = description;
    this.isPublic = isPublic;
    this.coauthors = coauthors;
    this.type = type;
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

  public String[] getCoauthors() {
    return coauthors;
  }

  public void setCoauthors(String[] coauthors) {
    this.coauthors = coauthors;
  }

  public String[] getType() {
    return type;
  }

  public void setType(String[] type) {
    this.type = type;
  }

}
