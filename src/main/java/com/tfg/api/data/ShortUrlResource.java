package com.tfg.api.data;

public class ShortUrlResource {
  Long projectId;
  String folderName;
  String fileName;
  String versionName;

  public ShortUrlResource() {
  }

  public ShortUrlResource(Long projectId, String folderName, String fileName, String versionName) {
    this.projectId = projectId;
    this.folderName = folderName;
    this.fileName = fileName;
    this.versionName = versionName;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getFolderName() {
    return folderName;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getVersionName() {
    return versionName;
  }

  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

}
