package com.tfg.api.data;

import java.util.Date;

public class Project {
  private Long project_id;
  private String name;
  private String description;
  private Date createdDate;
  private Date lastUpdateName;
  private String lastCommitId;
  private Boolean isPublic;
  private String owner;
  private String[] coauthors = null;

  public Project() {
  }
  public Project(Long project_id, String name, String description, Date createdDate, Date lastUpdateName,
      String lastCommitId, Boolean isPublic, String owner, String[] coauthors) {
    this.project_id = project_id;
    this.name = name;
    this.description = description;
    this.createdDate = createdDate;
    this.lastUpdateName = lastUpdateName;
    this.lastCommitId = lastCommitId;
    this.isPublic = isPublic;
    this.owner = owner;
    this.coauthors = coauthors;
  }

  public Long getProject_id() {
    return project_id;
  }

  public void setProject_id(Long project_id) {
    this.project_id = project_id;
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

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getLastUpdateName() {
    return lastUpdateName;
  }

  public void setLastUpdateName(Date lastUpdateName) {
    this.lastUpdateName = lastUpdateName;
  }

  public String getLastCommitId() {
    return lastCommitId;
  }

  public void setLastCommitId(String lastCommitId) {
    this.lastCommitId = lastCommitId;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public String getOwner() {
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
