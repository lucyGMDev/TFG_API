package com.tfg.api.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import java.io.FileReader;

import com.google.gson.Gson;
import com.tfg.api.data.FileData;
import com.tfg.api.data.FileList;
import com.tfg.api.data.FolderMetadata;
import com.tfg.api.data.OrderFilter;
import com.tfg.api.data.ProjectType;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectUtils {
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
    if (dbManager.projectIsPublic(projectId) || userIsAuthor(projectId, userEmail))
      return true;
    return false;
  }

  public static FileList getFilesFromFolder(Long projectId, String folderName, Boolean isAuthor) throws Exception {
    Dotenv dotenv = Dotenv.load();
    String folderPath = dotenv.get("PROJECTS_ROOT") + File.separator + projectId + File.separator + folderName;
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

  public static FileList getFilesFromFolder(Long projectId, String folderName) throws Exception {
    Dotenv dotenv = Dotenv.load();
    String folderPath = dotenv.get("PROJECTS_ROOT") + File.separator + projectId + File.separator + folderName;
    File folder = new File(folderPath);
    File[] files = folder.listFiles();
    FileList fileList = new FileList();
    
    for (File file : files) {
      if (file.isDirectory())
        continue;
      FileData metadataFile = FileUtils.getMetadataFile(projectId, folderName, file.getName());
      if (metadataFile.getIsPublic()) {
        fileList.getFiles().add(metadataFile);
      }
    }
    return fileList;
  }

  public static Boolean projectTypesAreValid(String[] projectTypes) {
    for (String projectType : projectTypes) {
      try {
        ProjectType.valueOf(projectType);
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }

  public static Boolean orderFilterIsValid(String orderFilter) {
    try {
      OrderFilter.valueOf(orderFilter);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public static FolderMetadata getMetadataFolder(Long projectId, String folderName) throws Exception {
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator
        + folderName + ".json";
    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String currentLine;
      String metadataFolderContent = "";
      while ((currentLine = reader.readLine()) != null) {
        metadataFolderContent += currentLine;
      }
      if (reader != null) {
        reader.close();
      }
      FolderMetadata metadataFolder = jsonManager.fromJson(metadataFolderContent, FolderMetadata.class);
      return metadataFolder;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    throw new Exception("Error while getting metadata");
  }

  public static String getCommitIdVersion(final Long projectId, final String versionName, String userEmail)
      throws NullPointerException, AccessControlException, NotFoundException {
    String commitIdVersion;
    DBManager database = new DBManager();
    if (versionName.equals("")) {
      commitIdVersion = database.getLastCommitProject(projectId);
      if (commitIdVersion == null) {
        throw new NullPointerException();
      }
    } else {
      if (!database.versionExistsOnProject(projectId, versionName)) {
        throw new NotFoundException();
      }
      if (!database.versionIsPublic(projectId, versionName)
          && !ProjectUtils.userIsAuthor(projectId, userEmail)) {
        throw new AccessControlException("You have not permission to access this version");
      }
      commitIdVersion = database.getCommitIdFromVersion(projectId, versionName);
      if (commitIdVersion == null) {
        throw new NullPointerException();
      }
    }
    return commitIdVersion;
  }

  public static String getCommitIdVersion(final Long projectId, final String versionName)
      throws NullPointerException, AccessControlException, NotFoundException {
    String commitIdVersion;
    DBManager database = new DBManager();
    if (versionName.equals("")) {
      commitIdVersion = database.getLastCommitProject(projectId);
      if (commitIdVersion == null) {
        throw new NullPointerException();
      }
    } else {
      if (!database.versionExistsOnProject(projectId, versionName)) {
        throw new NotFoundException();
      }
      if (!database.versionIsPublic(projectId, versionName)) {
        throw new AccessControlException("You have not permission to access this version");
      }
      commitIdVersion = database.getCommitIdFromVersion(projectId, versionName);
      if (commitIdVersion == null) {
        throw new NullPointerException();
      }
    }
    return commitIdVersion;
  }

}
