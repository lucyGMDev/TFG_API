package com.tfg.api.utils;

public class ProjectsUtil {
  public static Boolean userCanEditProject(Long projectId, String userEmail)
  {
    DBManager dbManager = new DBManager();
    String projectOwner = dbManager.getProjectOwner(projectId);
    Boolean canEdit = false;
    if(!userEmail.equals(projectOwner)) {
      String[] projectCoauthors = dbManager.getProjectCoauthors(projectId);
      for(String coauthor : projectCoauthors)
      {
        if(coauthor.equals(userEmail))
        {
          canEdit = true;
          break;
        }
      }
    }
    else{
      canEdit=true;
    }
    return canEdit;
  }
}
