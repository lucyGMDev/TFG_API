package com.tfg.api.utils;

import com.tfg.api.data.Version;
import com.tfg.api.data.VersionList;

public class VersionsUtils {
  public static VersionList getVersionsProject(final Long projectId, Boolean isAuthor) throws Exception {
    DBManager database = new DBManager();
    VersionList versions = new VersionList();


    VersionList allVersions = database.getVersionsProject(projectId);
    if (allVersions == null)
      throw new Exception("Error while getting versions for project " + projectId);
    
    String lastVersionId = database.getLastCommitProject(projectId);
    if (lastVersionId == null)
      throw new Exception("Error while getting versions for project " + projectId);

    Boolean projectIsPublic = database.projectIsPublic(projectId);
    if (projectIsPublic == null)
      throw new Exception("Error while getting versions for project " + projectId);

    Version currentVersion = new Version(projectId, lastVersionId, "current version", projectIsPublic);
    if (isAuthor || currentVersion.getIsPublic())
      versions.getVersionList().add(currentVersion);


    for (Version version : allVersions.getVersionList()) {
      if (version.getIsPublic() || isAuthor)
        versions.getVersionList().add(version);
    }

    return versions;
  }
}
