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

  /**
   * @param projectId
   * @param directoryName
   * @param filename
   * @return FileData
   * @throws Exception
   */
  public static FileData getMetadataFile(Long projectId, String directoryName, String filename) throws Exception {
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String metadataFileName = getMetadataFilename(filename);
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator
        + directoryName
        + File.separator + "metadata" + File.separator + metadataFileName;
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

  /**
   * @param projectId
   * @param directoryName
   * @param fileName
   * @param userEmail
   * @return Boolean
   * @throws Exception
   */
  public static Boolean userCanAccessFile(Long projectId, String directoryName, String fileName, String username)
      throws Exception {
    if (!ProjectUtils.userCanAccessProject(projectId, username))
      return false;

    FileData metadataFile = getMetadataFile(projectId, directoryName, fileName);
    if (metadataFile == null) {
      throw new Exception("Error getting metadata file");
    }

    if (!metadataFile.getIsPublic() && !ProjectUtils.userIsAuthor(projectId, username))
      return false;

    return true;
  }

  public static Boolean fileIsPublic(Long projectId, String directoryName, String fileName) throws Exception {
    FileData metadataFile = getMetadataFile(projectId, directoryName, fileName);
    if (metadataFile == null) {
      throw new Exception("Error getting metadata file");
    }
    return metadataFile.getIsPublic();
  }

  /**
   * @param projectId
   * @param directoryName
   * @param oldFileName
   * @param newFileName
   * @return int
   */
  public static int renameFile(Long projectId, String directoryName, String oldFileName, String newFileName) {
    Dotenv environmentVariablesManager = Dotenv.load();
    if (fileExists(projectId, directoryName, oldFileName)) {

      File oldFile = new File(
          environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator + directoryName
              + File.separator + oldFileName);
      File newFile = new File(
          environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator + directoryName
              + File.separator + newFileName);
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

  /**
   * @param originalFilename
   * @return String
   */
  public static String getMetadataFilename(String originalFilename) {
    return originalFilename + ".json";
  }

  /**
   * @param path
   * @return Boolean
   */
  public static Boolean fileExists(String path) {
    File file = new File(path);
    return file.exists();
  }

  /**
   * @param projectId
   * @param foldername
   * @param filename
   * @return Boolean
   */
  public static Boolean fileExists(Long projectId, String foldername, String filename) {
    Dotenv environmentVariablesManager = Dotenv.load();
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator
        + foldername + File.separator
        + filename;
    File file = new File(path);
    return file.exists();
  }

}
