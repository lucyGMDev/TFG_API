package com.tfg.api.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.tfg.api.data.MetadataFile;

import io.github.cdimascio.dotenv.Dotenv;

public class FileUtil {

  public static MetadataFile getMetadataFile(Long projectId, String directoryName, String filename) {
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String metadataFileName = getMetadataFilename(filename);
    // File metadataFile = new File(environmentVariablesManager.get("PROJECTS_ROOT")
    // + "/" + projectId + "/" + directoryName + "/" + metadataFileName);
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
      MetadataFile metadataFile = jsonManager.fromJson(metadataFileContent, MetadataFile.class);
      return metadataFile;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  public static Boolean userCanAccessFile(Long projectId, String directoryName, String fileName, String userEmail)
      throws Exception {
    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail))
      return false;
    MetadataFile metadataFile = getMetadataFile(projectId, directoryName, fileName);
    if (metadataFile == null) {
      throw new Exception("Error getting metadata file");
    }

    if (!metadataFile.getIsPublic() && !ProjectsUtil.userIsAuthor(projectId, userEmail))
      return false;

    return true;
  }

  public static int renameFile(Long projectId, String directoryName, String oldFileName, String newFileName,
      Boolean fileExistsDatabase) {
    DBManager dbManager = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    if (!dbManager.fileExists(projectId, directoryName, newFileName)) {

      File oldFile = new File(
          environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName + "/" + oldFileName);
      File newFile = new File(
          environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName + "/" + newFileName);
      try {
        oldFile.renameTo(newFile);
        if (fileExistsDatabase == true) {
          if (dbManager.updateFileName(projectId, directoryName, oldFileName, newFileName) == -1) {
            newFile = new File(environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName
                + "/" + oldFileName);
            oldFile = new File(environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId + "/" + directoryName
                + "/" + newFileName);
            oldFile.renameTo(newFile);
            return -1;
          }
        }
        return 1;
      } catch (Exception e) {
        e.printStackTrace();
        return -1;
      }
    }
    return -1;
  }

  public static String getMetadataFilename(String originalFilename) {
    String originalFilenameWithoutExtension = originalFilename.replaceFirst("[.][^.]+$", "");
    originalFilenameWithoutExtension = originalFilenameWithoutExtension.equals("") ? originalFilename : originalFilenameWithoutExtension;
    String metadataFilename = originalFilenameWithoutExtension + ".json";
    return metadataFilename;
  }
}
