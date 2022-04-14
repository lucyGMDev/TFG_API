package com.tfg.api.utils;

import java.security.AccessControlException;

import javax.ws.rs.NotFoundException;

import com.tfg.api.data.OrderFilter;
import com.tfg.api.data.ProjectType;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectUtils {
  public static Boolean userIsAuthor(Long projectId, String username) {
    DBManager dbManager = new DBManager();
    String[] projectCoauthors = dbManager.getProjectCoauthors(projectId);
    for (String coauthor : projectCoauthors) {
      if (coauthor.equals(username)) {
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

  public static Boolean userCanAccessProject(Long projectId, String username) {
    DBManager dbManager = new DBManager();
    if (dbManager.projectIsPublic(projectId) || userIsAuthor(projectId, username))
      return true;
    return false;
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

  public static String getCommitIdVersion(final Long projectId, final String versionName, String username)
      throws NullPointerException, AccessControlException, NotFoundException {
    String commitIdVersion;
    DBManager database = new DBManager();
    Boolean userIsAuthor = !ProjectUtils.userIsAuthor(projectId, username);
    if (versionName.equals("")) {
      commitIdVersion = database.getLastCommitProject(projectId);
      if (commitIdVersion == null) {
        throw new NullPointerException();
      }
      if (!database.projectIsPublic(projectId) && !userIsAuthor) {
        throw new AccessControlException("You have not permission to access this version");
      }
    } else {
      if (!database.versionExistsOnProject(projectId, versionName)) {
        throw new NotFoundException();
      }
      if (!database.versionIsPublic(projectId, versionName)
          && userIsAuthor) {
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
    Dotenv environmentVariablesManager = Dotenv.load();
    DBManager database = new DBManager();
    if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
      commitIdVersion = database.getLastCommitProject(projectId);
      if (commitIdVersion == null) {
        throw new NullPointerException();
      }
      if (!database.projectIsPublic(projectId)) {
        throw new AccessControlException("You have not permission to access this version");
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
