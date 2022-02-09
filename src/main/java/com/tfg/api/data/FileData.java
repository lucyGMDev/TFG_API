package com.tfg.api.data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

public class FileData {
  String fileName;
  String directoryName;
  Long projectId;
  Date uploadedDate;
  Date lastUpdatedDate;
  String author;
  String description;
  Boolean isPublic;
  HashMap<String, Integer> scores;
  float avgScore;

  public FileData() {
    scores = new HashMap<String, Integer>();
  }

  public FileData(String fileName, String directoryName, Long projectId, Date uploadedDate, Date lastUpdatedDate,
      String author, String description, Boolean isPublic, HashMap<String, Integer> scores, float avgScore) {
    this.fileName = fileName;
    this.directoryName = directoryName;
    this.projectId = projectId;
    this.uploadedDate = uploadedDate;
    this.lastUpdatedDate = lastUpdatedDate;
    this.author = author;
    this.description = description;
    this.isPublic = isPublic;
    this.scores = scores;
    this.avgScore = avgScore;
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

  public Date getLastUpdatedDate() {
    return lastUpdatedDate;
  }

  public void setLastUpdatedDate(Date lastUpdatedDate) {
    this.lastUpdatedDate = lastUpdatedDate;
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

  public HashMap<String, Integer> getScores() {
    return scores;
  }

  public void setScores(HashMap<String, Integer> scores) {
    this.scores = scores;
  }

  
  public float getAvgScore() {
    calculateAvgScore();
    return this.avgScore;
  }


  public void calculateAvgScore() {
    float sumScore = 0;
    for (String user : scores.keySet()) {
      sumScore += scores.get(user);
    }
    float avg = sumScore / this.scores.size();
    this.avgScore = BigDecimal.valueOf(avg).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
  }

  public void addScore(String user, Integer score){
    if(this.scores == null){
      this.scores = new HashMap<String, Integer>();
    }
    this.scores.put(user, score);
    calculateAvgScore();
  }
}
