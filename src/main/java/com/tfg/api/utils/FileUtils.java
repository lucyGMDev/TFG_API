package com.tfg.api.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.tfg.api.data.FileData;

import io.github.cdimascio.dotenv.Dotenv;

public class FileUtils {

  public static FileData getMetadataFile(Long projectId, String directoryName, String filename) throws Exception {
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String metadataFileName = getMetadataFilename(filename);
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName
        + "/metadata/" + metadataFileName;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String currentLine;
      String metadataFileContent = "";
      while ((currentLine = reader.readLine()) != null) {
        metadataFileContent += currentLine;
      }
      if (reader != null) {
        reader.close();
      }
      FileData metadataFile = jsonManager.fromJson(metadataFileContent, FileData.class);
      return metadataFile;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    throw new Exception("Error while getting metadata");
  }

  public static Boolean userCanAccessFile(Long projectId, String directoryName, String fileName, String userEmail)
      throws Exception {
    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail))
      return false;
    FileData metadataFile = getMetadataFile(projectId, directoryName, fileName);
    if (metadataFile == null) {
      throw new Exception("Error getting metadata file");
    }

    if (!metadataFile.getIsPublic() && !ProjectsUtil.userIsAuthor(projectId, userEmail))
      return false;

    return true;
  }

  public static int renameFile(Long projectId, String directoryName, String oldFileName, String newFileName) {
    Dotenv environmentVariablesManager = Dotenv.load();
    if (fileExists(projectId, directoryName, oldFileName)) {

      File oldFile = new File(
          environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName + "/" + oldFileName);
      File newFile = new File(
          environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName + "/" + newFileName);
      try {
        oldFile.renameTo(newFile);
        return 1;
      } catch (Exception e) {
        e.printStackTrace();
        return -1;
      }
    }
    return -1;
  }

  public static String getMetadataFilename(String originalFilename) {
    return originalFilename + ".json";
  }

  public static Boolean fileExists(String path) {
    File file = new File(path);
    return file.exists();
  }

  public static Boolean fileExists(Long projectId, String foldername, String filename) {
    Dotenv environmentVariablesManager = Dotenv.load();
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + foldername + "/"
        + filename;
    File file = new File(path);
    return file.exists();
  }

}
