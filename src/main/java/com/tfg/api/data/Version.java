package com.tfg.api.data;

public class Version {
  private Long projectId;
  private String versionId;
  private String name;
  private Boolean isPublic;
  public Version() {
  }
  public Version(Long projectId, String versionId, String name, Boolean isPublic) {
    this.projectId = projectId;
    this.versionId = versionId;
    this.name = name;
    this.isPublic = isPublic;
  }
  public Long getProjectId() {
    return projectId;
  }
  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
  public String getVersionId() {
    return versionId;
  }
  public void setVersionId(String versionId) {
    this.versionId = versionId;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public Boolean getIsPublic() {
    return isPublic;
  }
  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  
}
