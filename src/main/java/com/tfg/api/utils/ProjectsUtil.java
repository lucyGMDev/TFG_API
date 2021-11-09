package com.tfg.api.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectsUtil {
  public static Boolean userIsAuthor(Long projectId, String userEmail) {
    DBManager dbManager = new DBManager();
    String projectOwner = dbManager.getProjectOwner(projectId);
    Boolean canEdit = false;
    if (!userEmail.equals(projectOwner)) {
      String[] projectCoauthors = dbManager.getProjectCoauthors(projectId);
      for (String coauthor : projectCoauthors) {
        if (coauthor.equals(userEmail)) {
          canEdit = true;
          break;
        }
      }
    } else {
      canEdit = true;
    }
    return canEdit;
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

  public static Boolean userCanAccessProject(Long projectId, String userEmail) {
    DBManager dbManager = new DBManager();
    if (dbManager.projectIsPublic(projectId))
      return true;
    if (userIsAuthor(projectId, userEmail))
      return true;
    return false;
  }

  public static Boolean userCanAccessFile(Long projectId, String directoryName, String fileName, String userEmail) {
    DBManager dbManager = new DBManager();
    if (!userCanAccessProject(projectId, userEmail))
      return false;

    if (!dbManager.fileIsPublic(fileName, directoryName, projectId) && !userIsAuthor(projectId, userEmail))
      return false;

    return true;
  }
}
