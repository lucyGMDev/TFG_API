package com.tfg.api.data;

import java.util.Date;

public class FolderMetadata {
  private String folderName;
  private Long numberViews;
  private Boolean isPublic;
  private Boolean showHistory;
  private Long numberDownloads;
  private Date lastUpdated;
  private Long numberFiles;

  public FolderMetadata(String folderName) {
    this.folderName = folderName;
    this.numberViews = 0L;
    this.isPublic = true;
    this.showHistory = true;
    this.numberDownloads = 0L;
    this.lastUpdated = new Date();
    this.numberFiles = 0L;
  }

  public FolderMetadata(String folderName, Long numberViews, Boolean isPublic, Boolean showHistory,
      Long numberDownloads,
      Date lastUpdatedDate) {
    this.folderName = folderName;
    this.numberViews = numberViews;
    this.isPublic = isPublic;
    this.numberDownloads = numberDownloads;
    this.lastUpdated = lastUpdatedDate;
    this.showHistory = showHistory;
  }

  public Long getNumberViews() {
    return numberViews;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public Boolean getShowHistory() {
    return showHistory;
  }

  public void setShowHistory(Boolean showHistory) {
    this.showHistory = showHistory;
  }

  public void setNumberViews(Long numberViews) {
    this.numberViews = numberViews;
  }

  public Long getNumberDownloads() {
    return numberDownloads;
  }

  public void setNumberDownloads(Long numberDownloads) {
    this.numberDownloads = numberDownloads;
  }

  public String getFolderName() {
    return folderName;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdatedDate) {
    this.lastUpdated = lastUpdatedDate;
  }

  public Long getNumberFile() {
    return numberFiles;
  }

  public void setNumberFiles(Long numberFiles) {
    this.numberFiles = numberFiles;
  }

  public void incrementNumberViews() {
    if (numberViews == null)
      numberViews = 1L;
    else
      this.numberViews++;
  }

  public void incrementNumberDownloads() {
    if (this.numberDownloads == null)
      this.numberDownloads = 1L;
    else
      this.numberDownloads++;
  }

  public void resestMetadata() {
    this.numberDownloads = null;
    this.lastUpdated = null;
    this.numberDownloads = null;
    this.numberViews = null;
    this.numberFiles = null;
  }
}
