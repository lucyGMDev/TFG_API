package com.tfg.api.utils;

import java.util.Date;

public class HistorialMessages {
  public enum operations {
    UploadFile, RemoveFile, UpdateFile, CreateProject, UpdateProject, CoauthorAdded, CoauthorRemoved
  }

  public static String createProject(String userEmail) {
    return String.format("CreateProject: %s create project:%s", userEmail, new Date().toString());
  }

  public static String updateProject(String userEmail) {
    return String.format("UpdateProject: %s update project:%s", userEmail, new Date().toString());
  }

  public static String userAddFile(String userEmail, String fileName, String folderName) {
    return String.format("UploadFile: %s upload %s on %s,%s", userEmail, fileName, folderName, new Date().toString());
  }

  public static String userRemoveFile(String userEmail, String fileName, String folderName) {
    return String.format("RemoveFile: %s remove %s on %s,%s", userEmail, fileName, folderName, new Date().toString());
  }

  public static String userUpdateFile(String userEmail, String fileName, String folderName) {
    return String.format("UpdateFile: %s update %s on %s,%s", userEmail, fileName, folderName, new Date().toString());
  }


  public static String addCoauthor(String userEmail, String newCoauthor) {
    return String.format("CoauthorAdded: %s add %s;%s", userEmail, newCoauthor, new Date().toString());
  }

  public static String removeCoauthor(String userEmail, String oldCoauthor) {
    return String.format("CoauthorRemoved: %s remove %s;%s", userEmail, oldCoauthor, new Date().toString());
  }

  public static String createVersion(String userEmail, String versionName){
    return String.format("VersionName: %s create version %s;%s",userEmail,versionName,new Date().toString());
  }
}
