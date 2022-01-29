package com.tfg.api.data;

import java.util.Date;

public class FileData {
  String fileName;
  String directoryName;
  Long projectId;
  Date uploadedDate;
  Date lastUpdatedName;
  String author;
  String description;
  Boolean isPublic;
  
  public FileData() {
  }
  
  public FileData(String fileName, String directoryName, Long projectId, Date uploadedDate, Date lastUpdatedName, String author) {
    this.fileName = fileName;
    this.directoryName = directoryName;
    this.projectId = projectId;
    this.uploadedDate = uploadedDate;
    this.lastUpdatedName = lastUpdatedName;
    this.author = author;
  }

  public String getFileName() {
    return fileName;
  }
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
  public String getDirectoryName() {
    return directoryName;
  }
  public void setDirectoryName(String directoryName) {
    this.directoryName = directoryName;
  }
  public Long getProjectId() {
    return projectId;
  }
  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
  public Date getUploadedDate() {
    return uploadedDate;
  }
  public void setUploadedDate(Date uploadedDate) {
    this.uploadedDate = uploadedDate;
  }
  public Date getLastUpdatedName() {
    return lastUpdatedName;
  }
  public void setLastUpdatedName(Date lastUpdatedName) {
    this.lastUpdatedName = lastUpdatedName;
  }
  public String getAuthor() {
    return author;
  }
  public void setAuthor(String author) {
    this.author = author;
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
  

  
}
