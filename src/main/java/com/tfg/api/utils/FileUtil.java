package com.tfg.api.utils;

import java.io.File;
import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;

public class FileUtil {
  public static Boolean userCanAccessFile(Long projectId, String directoryName, String fileName, String userEmail) {
    DBManager dbManager = new DBManager();
    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail))
      return false;

    if (!dbManager.fileIsPublic(fileName, directoryName, projectId) && !ProjectsUtil.userIsAuthor(projectId, userEmail))
      return false;

    return true;
  }

  public static int renameFile(Long projectId, String directoryName, String oldFileName, String newFileName)
  {
    DBManager dbManager = new DBManager();
    Dotenv dotenv = Dotenv.load();
    if(!dbManager.fileExists(projectId, directoryName, newFileName))
    {
      
      File oldFile = new File(dotenv.get("PROJECTS_ROOT")+"/"+projectId+"/"+directoryName+"/"+oldFileName);
      File newFile = new File(dotenv.get("PROJECTS_ROOT")+"/"+projectId+"/"+directoryName+"/"+newFileName);
      try
      {
        oldFile.renameTo(newFile);
        if(dbManager.updateFileName(projectId, directoryName, oldFileName, newFileName)==-1)
        {
          newFile = new File(dotenv.get("PROJECTS_ROOT")+"/"+projectId+"/"+directoryName+"/"+oldFileName);
          oldFile = new File(dotenv.get("PROJECTS_ROOT")+"/"+projectId+"/"+directoryName+"/"+newFileName);
          oldFile.renameTo(newFile);
          return -1;
        }
        return 1;
      }
      catch (Exception e)
      {
        e.printStackTrace();
        return -1;
      }
    }
    return -1;
  }
}
