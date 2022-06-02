package com.tfg.api.utils;

import java.util.Date;

public class HistorialMessages {
  public enum Operations {
    UploadFile, RemoveFile, UpdateFile, CreateProject, UpdateProject, CoauthorAdded, CoauthorRemoved, CreateVerion
  }

  public static final String ACTOR = "ACTOR";
  public static final String OBJECT = "OBJECT";
  public static final String DATE = "DATE";
  public static final String FOLDER = "FOLDER";

  private Operations operation;
  private String actor;
  private String object;
  private Date changeDate;
  private Long projectId;
  private String folder;
  private String file;
  private String versionName;

  public HistorialMessages() {
  }

  public HistorialMessages(Operations operation, String actor, String objetc, Date changeDate) {
    this.operation = operation;
    this.actor = actor;
    this.object = objetc;
    this.changeDate = changeDate;
  }

  public HistorialMessages(Operations operation, String actor, Date changeDate) {
    this.operation = operation;
    this.actor = actor;
    this.changeDate = changeDate;
  }

  public Operations getOperation() {
    return operation;
  }

  public void setOperation(Operations operation) {
    this.operation = operation;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
  }

  public Date getChangeDate() {
    return changeDate;
  }

  public void setChangeDate(Date changeDate) {
    this.changeDate = changeDate;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public String getVersionName() {
    return versionName;
  }

  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  @Override
  public String toString() {
    String historialMessage = "";
    if (operation.equals(Operations.CreateProject)) {
      historialMessage = String.format("%s has created the project", this.actor);
    }
    if (operation.equals(Operations.CoauthorAdded)) {
      historialMessage = String.format("%s has added %s to the project", this.actor, this.object);
    }
    if (operation.equals(Operations.UpdateProject)) {
      historialMessage = String.format("%s has update the project", this.actor);
    }
    if (operation.equals(Operations.CoauthorRemoved)) {
      historialMessage = String.format("%s has removed %s from the project", this.actor, this.object);
    }
    if (operation.equals(Operations.UploadFile)) {
      historialMessage = String.format("%s has added the file %s to the project, on the item %s", this.actor,
          this.object, this.folder);
    }
    if (operation.equals(Operations.UpdateFile)) {
      historialMessage = String.format("%s has updated file %s on the item %s", this.actor, this.object,
          this.folder);
    }
    if (operation.equals(Operations.RemoveFile)) {
      historialMessage = String.format("%s has removed the file %s from the item %s", this.actor, this.object,
          this.folder);
    }
    if (operation.equals(Operations.CreateVerion)) {
      historialMessage = String.format("%s has created the version %s on this project", this.actor, this.object);
    }

    return historialMessage;
  }

}
