package com.tfg.api.data;

import java.math.BigDecimal;
import java.util.Date;

public class Project {
  private Long project_id;
  private String name;
  private String description;
  private Date createdDate;
  private Date lastUpdateName;
  private String lastCommitId;
  private Boolean isPublic;
  private Boolean showHistory;
  private String[] coauthors = null;
  private String[] type;
  private Float avgScore;

  public Project() {
  }

  public Project(Long project_id, String name, String description, Date createdDate, Date lastUpdateName,
      String lastCommitId, Boolean isPublic, Boolean showHistory, String[] coauthors, String[] type, Float avgScore) {
    this.project_id = project_id;
    this.name = name;
    this.description = description;
    this.createdDate = createdDate;
    this.lastUpdateName = lastUpdateName;
    this.lastCommitId = lastCommitId;
    this.isPublic = isPublic;
    this.showHistory = showHistory;
    this.coauthors = coauthors;
    this.type = type;
    if (avgScore != null) {
      this.avgScore = BigDecimal.valueOf(avgScore).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
    }
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

  public Boolean getShowHistory() {
    return showHistory;
  }

  public void setShowHistory(Boolean showHistory) {
    this.showHistory = showHistory;
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

  public Float getAvgScore() {
    return avgScore;
  }

  public void setAvgScore(Float avgScore) {
    avgScore = avgScore != null ? BigDecimal.valueOf(avgScore).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue()
        : null;
    this.avgScore = avgScore;
  }

}
