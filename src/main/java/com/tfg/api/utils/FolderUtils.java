package com.tfg.api.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.FileReader;

import com.google.gson.Gson;
import com.tfg.api.data.FileData;
import com.tfg.api.data.FileList;
import com.tfg.api.data.FolderMetadata;

import io.github.cdimascio.dotenv.Dotenv;

public class FolderUtils {
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

  public static Boolean userCanAccessFolder(Long projectId, String folderName, String username) throws Exception {
    DBManager database = new DBManager();
    FolderMetadata folderMetadata = getMetadataFolder(projectId, folderName);
    if (!database.userIsCoauthor(projectId, username) && !folderMetadata.getIsPublic())
      return false;

    return true;
  }

  public static Boolean userCanAccessFolder(Long projectId, String folderName) throws Exception {
    FolderMetadata folderMetadata = getMetadataFolder(projectId, folderName);
    if (!folderMetadata.getIsPublic())
      return false;

    return true;
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

  public static ArrayList<FolderMetadata> getListPublicFoldersMetadata(Long projectId) throws Exception {
    Dotenv environmentVariablesManager = Dotenv.load();

    ArrayList<FolderMetadata> publicFoldersMetadata = new ArrayList<FolderMetadata>();
    try {
      publicFoldersMetadata = Arrays
          .stream(environmentVariablesManager.get("PROJECT_SUBDIRS").split(",")).map((String folderName) -> {
            try {
              return FolderUtils.getMetadataFolder(projectId, folderName);
            } catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          }).filter(folderMetadata -> folderMetadata.getIsPublic())
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (RuntimeException e) {
      throw new Exception(e);
    }
    return publicFoldersMetadata;
  }

  public static Long getNumberFilesPublic(Long projectId, String folderName) throws Exception {

    return getFilesFromFolder(projectId, folderName).getFiles().stream().filter(file -> file.getIsPublic()).count();
  }

  public static Long getNumberFiles(Long projectId, String folderName) throws Exception {

    return getFilesFromFolder(projectId, folderName, true).getFiles().stream().count();
  }

  /**
   * Get a list of all items in the project, but all private items are censured
   * (dont seen any data)
   * 
   * @param projectId Identifier of the project
   * @return A list with all items in the project censured
   * @throws Exception
   */
  public static ArrayList<FolderMetadata> getListItemsCensured(Long projectId) throws Exception {
    Dotenv environmentVariablesManager = Dotenv.load();

    ArrayList<FolderMetadata> publicFoldersMetadata = new ArrayList<FolderMetadata>();
    try {
      publicFoldersMetadata = Arrays
          .stream(environmentVariablesManager.get("PROJECT_SUBDIRS").split(",")).map((String folderName) -> {
            try {
              return FolderUtils.getMetadataFolder(projectId, folderName);
            } catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          }).map((FolderMetadata folderMetadata) -> {
            if (folderMetadata.getIsPublic()) {
              try {
                folderMetadata.setNumberFiles(getNumberFilesPublic(projectId, folderMetadata.getFolderName()));
              } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
              }
              return folderMetadata;
            } else {
              folderMetadata.resestMetadata();
              return folderMetadata;
            }
          })
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (RuntimeException e) {
      throw new Exception(e);
    }
    return publicFoldersMetadata;
  }

  /**
   * Get a list of all items in the project
   * 
   * @param projectId Identifier of the project
   * @return A list with all items in the project
   * @throws Exception
   */
  public static ArrayList<FolderMetadata> getListItems(Long projectId) throws Exception {
    Dotenv environmentVariablesManager = Dotenv.load();

    ArrayList<FolderMetadata> publicFoldersMetadata = new ArrayList<FolderMetadata>();
    try {
      publicFoldersMetadata = Arrays
          .stream(environmentVariablesManager.get("PROJECT_SUBDIRS").split(",")).map((String folderName) -> {
            try {
              return FolderUtils.getMetadataFolder(projectId, folderName);
            } catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          }).map((FolderMetadata folderMetadata) -> {
            try {
              folderMetadata.setNumberFiles(getNumberFiles(projectId, folderMetadata.getFolderName()));
            } catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
            return folderMetadata;
          })
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (RuntimeException e) {
      throw new Exception(e);
    }
    return publicFoldersMetadata;
  }
}
