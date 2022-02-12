package com.tfg.api.utils;

import java.io.File;

import com.tfg.api.data.FileData;
import com.tfg.api.data.FileList;
import com.tfg.api.data.OrderFilter;
import com.tfg.api.data.ProjectType;

import io.github.cdimascio.dotenv.Dotenv;


public class ProjectsUtil {
  public static Boolean userIsAuthor(Long projectId, String userEmail) {
    DBManager dbManager = new DBManager();
    String[] projectCoauthors = dbManager.getProjectCoauthors(projectId);
    for (String coauthor : projectCoauthors) {
      if (coauthor.equals(userEmail)) {
        return true;
      }
    }
    return false;

  }

  public static Boolean folderNameIsValid(String folderName) {
    Dotenv dotenv = Dotenv.load();
    String[] folders = dotenv.get("PROJECT_SUBDIRS").split(",");
    for (String folder : folders) {
      if (folderName.equals(folder.trim())) {
        return true;
      }
    }
    return false;
  }

  public static Boolean typesAreValid(String[] types) {
    Dotenv dotenv = Dotenv.load();
    String[] validTypes = dotenv.get("PROJECT_TYPES").split(",");
    for (String type : types) {
      Boolean valid = false;
      for (String validType : validTypes) {
        if (type.equals(validType)) {
          valid = true;
          break;
        }
      }
      if (!valid)
        return false;
    }

    return true;
  }

  public static Boolean userCanAccessProject(Long projectId, String userEmail) {
    DBManager dbManager = new DBManager();
    if (dbManager.projectIsPublic(projectId))
      return true;
    if (userIsAuthor(projectId, userEmail))
      return true;
    return false;
  }

  public static FileList getFilesFromFolder(Long projectId, String folderName, Boolean isAuthor) throws Exception {
    Dotenv dotenv = Dotenv.load();
    String folderPath = dotenv.get("PROJECTS_ROOT") + "/" + projectId + "/" + folderName;
    File folder = new File(folderPath);
    File[] files = folder.listFiles();
    FileList fileList = new FileList();
    for (File file : files) {
      if (file.isDirectory())
        continue;
      FileData metadataFile = FileUtils.getMetadataFile(projectId, folderName, file.getName());
      if (isAuthor || metadataFile.getIsPublic()) {
        fileList.getFiles().add(metadataFile);
      }
    }
    return fileList;
  }

  public static Boolean projectTypesAreValid(String[] projectTypes){
    for (String projectType : projectTypes) {
      try {
        ProjectType.valueOf(projectType);
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }

  public static Boolean orderFilterIsValid(String orderFilter){
    try{
      OrderFilter.valueOf(orderFilter);
    }catch(Exception e){
      return false;
    }
    return true;
  }

}
