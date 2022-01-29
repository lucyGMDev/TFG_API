package com.tfg.api.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.FileData;
import com.tfg.api.data.MetadataFile;
import com.tfg.api.data.Project;
import com.tfg.api.data.bodies.ProjectBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.FileUtil;
import com.tfg.api.utils.JwtUtils;
import com.tfg.api.utils.ProjectRepository;
import com.tfg.api.utils.ProjectsUtil;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectController {
  public static Response getProject(String token, Long projectId) {
    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"User can not access this project\"}").build();
    }

    Project userProject = database.getProjectById(projectId);

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(userProject))
        .build();
  }

  public static Response CreateProject(final ProjectBody project, final String token) {
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    String owner = null;
    try {
      owner = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    if (project.getOwner() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Owner is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    if (!project.getOwner().equals(owner)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"Project owner does not match with login user\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (!database.userExistsByEmail(owner)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"There are any user register with that email\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (project.getName() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Project name is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    if (project.getIsPublic() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Project privacity is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (project.getCoauthors() != null) {
      for (String coauthor : project.getCoauthors()) {
        if (!database.userExistsByEmail(coauthor)) {
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("{\"message\":\"You are trying to add a coauthor that already not exists\"}")
              .type(MediaType.APPLICATION_JSON).build();
        }
      }
    }

    Project projectCreated = database.createProject(project);
    if (projectCreated == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error creating project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    Long projectId = projectCreated.getProject_id();
    String rootProjects = environmentVariablesManager.get("PROJECTS_ROOT");
    String projectPath = rootProjects + "/" + projectId;
    final File projectFolder = new File(projectPath);
    projectFolder.mkdirs();

    String[] projectsSubdir = environmentVariablesManager.get("PROJECT_SUBDIRS").split(",");
    for (String subDir : projectsSubdir) {
      String subFolderPath = projectPath + "/" + subDir.trim();
      File subFolder = new File(subFolderPath);
      subFolder.mkdirs();
    }

    try {
      new ProjectRepository(projectPath);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error creating repository\"}").type(MediaType.APPLICATION_JSON).build();
    }

    if (project.getCoauthors() != null) {
      for (String coauthor : project.getCoauthors()) {
        if (database.userIsCoauthor(projectId, coauthor))
          continue;
        int result = database.addCoauthorToProject(projectId, coauthor);
        if (result == -1) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\": \"Error while adding coauthor to project\"}").type(MediaType.APPLICATION_JSON)
              .build();
        }
      }
    }

    projectCreated.setCoauthors(project.getCoauthors());
    return Response.status(Response.Status.OK).entity(jsonManager.toJson(projectCreated))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response updateProject(final Long projectId, final ProjectBody projectBody, final String token) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    String email;
    try {
      email = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (email == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error getting user email\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, email)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to update the project \"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    Project project = database.getProjectById(projectId);
    if (projectBody.getName() != null) {
      project.setName(projectBody.getName());
    }
    if (projectBody.getDescription() != null) {
      project.setDescription(projectBody.getDescription());
    }
    if (projectBody.getIsPublic() != null) {
      project.setIsPublic(projectBody.getIsPublic());
    }
    Project projectUpdated = null;

    if (projectBody.getName() != null || projectBody.getDescription() != null || projectBody.getIsPublic() != null) {
      projectUpdated = database.updateProject(projectId, project);
    } else {
      projectUpdated = project;
    }

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(projectUpdated))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response addCoauthorFromProject(final Long projectId, String token, String[] coauthors) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();

    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (coauthors == null || coauthors.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Coauthors list to add is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to add coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    for (String coauthor : coauthors) {
      if (database.userExistsByEmail(coauthor) && !database.userIsCoauthor(projectId, coauthor)) {
        if (database.addCoauthor(projectId, coauthor) == -1) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\":\"An error occurred while adding coauthor\"}").type(MediaType.APPLICATION_JSON)
              .build();
        }
      }
    }
    Project project = database.getProjectById(projectId);

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(project)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response removeCoauthorsFromProject(final Long projectId, final String token,
      final String[] coauthors) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (coauthors == null || coauthors.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Coauthors list to add is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to remove coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    for (String coauthor : coauthors) {
      if (!database.userExistsByEmail(coauthor)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You are trying to remove an user who does not exist\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
      if (!database.userIsCoauthor(projectId, coauthor)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You are trying to remove an user who does not coauthor of this project\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
    }

    for (String coauthor : coauthors) {
      if (database.removeCoauthor(projectId, coauthor) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"message\":\"An error occurred while removing coauthor\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
    }

    Project project = database.getProjectById(projectId);

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(project)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response addFileToProject(final Long projectId, final String token, final String folderName,
      final String description, final Boolean isPublic, final InputStream uploadedInputStream,
      final FormDataContentDisposition fileDetail) {

    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (folderName == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Folder name is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (uploadedInputStream == null || fileDetail == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"File is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Does not exist that folder\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to add files on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String filename = fileDetail.getFileName();
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId;
    ProjectRepository projectRepository;
    try {
      projectRepository = new ProjectRepository(projectPath);
    } catch (IllegalStateException | GitAPIException | IOException e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while adding file to project\"}").build();
    }
    String lastProjectVersion = database.getLastCommitProject(projectId);
    if (lastProjectVersion != null) {
      try {
        String projectVersion = projectRepository.getCurrentVersion();
        if (!lastProjectVersion.equals(projectVersion)) {
          try {
            projectRepository.changeVersion(lastProjectVersion);
          } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Error while adding fileto project\"}").build();
          }
        }

      } catch (GitAPIException e1) {
        e1.printStackTrace();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while adding file to project\"}").build();
      }
    }
    if (database.fileExists(projectId, folderName, filename)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"This file current exists on this folder\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String commit = "";
    try {
      projectRepository.addFile(uploadedInputStream, folderName, filename);

    } catch (Exception e) {
      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Server error was produced while uploading file\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }
    try {
      MetadataFile metadata = new MetadataFile(description, isPublic);
      commit = projectRepository.createMetadataFile(jsonManager.toJson(metadata), folderName, filename);
    } catch (IOException | GitAPIException e) {
      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Server error was produced while uploading file\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }

    if (database.insertFile(projectId, folderName, filename, isPublic, description, userEmail) == -1) {
      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e) {
        e.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while uploading file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    if (database.addCommitProject(projectId, commit) == -1) {

      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e) {
        e.printStackTrace();
      }
      database.removeProject(projectId);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while uploading file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    FileData file = database.getFile(projectId, folderName, filename);
    if (file == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while getting file\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    return Response.status(Response.Status.OK).entity(jsonManager.toJson(file)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response getFile(String token, Long projectId, String folderName, String filename) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    FileData file = database.getFile(projectId, folderName, filename);
    if (file == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current file does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You have not permission to access this project\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }
    try {
      if (!FileUtil.userCanAccessFile(projectId, folderName, filename, userEmail)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You have not permission to access this file\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while getting file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    MetadataFile metadata = FileUtil.getMetadataFile(projectId, folderName, filename);
    if (metadata != null) {
      if (metadata.getDescription() != null)
        file.setDescription(metadata.getDescription());
      if (metadata.getIsPublic() != null)
        file.setIsPublic(metadata.getIsPublic());
      else
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"message\":\"Error while getting file\"}").type(MediaType.APPLICATION_JSON).build();
    } else {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while getting file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(file)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response getFileFromVersion(String token, Long projectId, String folderName, String filename,
      String versionId) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId;
    ProjectRepository project = null;

    try {
      project = new ProjectRepository(path);
      project.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    }

    Response fileResponse = getFile(token, projectId, folderName, filename);

    String lastVersionId = database.getLastCommitProject(projectId);

    try {
      project.changeVersion(lastVersionId);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }

    return fileResponse;
  }

  public static Response updateFile(String token, final Long projectId, final String folderName, final String filename,
      final String description, final Boolean isPublic, final InputStream uploadedInputStream,
      final FormDataContentDisposition fileDetail) {
    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to update this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String lastProjectVersion = database.getLastCommitProject(projectId);
    FileData file = database.getFile(projectId, folderName, filename);
    MetadataFile metadata = FileUtil.getMetadataFile(projectId, folderName, filename);

    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + "/" + projectId;
    ProjectRepository projectRepository;
    try {
      projectRepository = new ProjectRepository(projectPath);
    } catch (IllegalStateException | GitAPIException | IOException e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while adding file to project\"}").build();
    }
    try {
      if (lastProjectVersion != null) {
        String projectVersion = projectRepository.getCurrentVersion();
        if (!lastProjectVersion.equals(projectVersion)) {
          try {
            projectRepository.changeVersion(lastProjectVersion);
          } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Error while adding file to project\"}").build();
          }
        }
      }
    } catch (GitAPIException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating file\"}").build();
    }
    if (file == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Current file does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (metadata == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while updating file\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (uploadedInputStream != null && fileDetail != null) {
      if (!fileDetail.getFileName().equals(filename)) {
        if (FileUtil.renameFile(projectId, folderName, filename, fileDetail.getFileName(), true) == -1) {
          try {
            projectRepository.changeVersion(lastProjectVersion);
          } catch (GitAPIException e) {
            e.printStackTrace();
          }
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\":\"Error while updating file\"}").type(MediaType.APPLICATION_JSON).build();
        }

        String oldMetadataFilename = "/metadata/" + FileUtil.getMetadataFilename(filename);
        String newMetadataFilename = "/metadata/" + FileUtil.getMetadataFilename(fileDetail.getFileName());

        if (FileUtil.renameFile(projectId, folderName, oldMetadataFilename, newMetadataFilename, false) == -1) {
          try {
            projectRepository.changeVersion(lastProjectVersion);
          } catch (GitAPIException e) {
            e.printStackTrace();
          }
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\":\"Error while updating file\"}").type(MediaType.APPLICATION_JSON).build();
        }
        file.setFileName(fileDetail.getFileName());
      }

      try {
        projectRepository.addFile(uploadedInputStream, folderName, fileDetail.getFileName());
        if (description != null)
          metadata.setDescription(description);
        if (isPublic != null)
          metadata.setIsPublic(isPublic);
        String commitId = projectRepository.createMetadataFile(jsonManager.toJson(metadata), folderName,
            fileDetail.getFileName());
        database.addCommitProject(projectId, commitId);
      } catch (Exception e) {
        e.printStackTrace();
        try {
          projectRepository.changeVersion(lastProjectVersion);
          database.addCommitProject(projectId, lastProjectVersion);
        } catch (GitAPIException e1) {
          e1.printStackTrace();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error updating file\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
    }

    FileData fileSaveState = database.getFile(projectId, folderName, filename);
    FileData fileUpdated = database.updateFile(projectId, folderName, file.getFileName());
    if (fileUpdated == null) {
      database.updateFile(fileSaveState.getProjectId(), fileSaveState.getDirectoryName(), fileSaveState.getFileName());
      try {
        projectRepository.changeVersion(lastProjectVersion);
        database.addCommitProject(projectId, lastProjectVersion);
      } catch (GitAPIException e) {
        e.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error updating file\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    fileUpdated.setIsPublic(metadata.getIsPublic());
    fileUpdated.setDescription(metadata.getDescription());
    return Response.status(Response.Status.OK).entity(jsonManager.toJson(fileUpdated))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response getFileLink(final String token, final Long projectId, final String folderName,
      final String filename) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error loading user email\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    try {

      if (!FileUtil.userCanAccessFile(projectId, folderName, filename, userEmail)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You have not permission to access this file\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while getting file link\"}").type(MediaType.APPLICATION_JSON).build();
    }
    String shortUrl = database.getShortUrlFile(projectId, folderName, filename);
    if (shortUrl == null) {
      UUID fileUUID = UUID.randomUUID();
      String[] uuidSplited = fileUUID.toString().split("-");
      shortUrl = "";
      for (String uuidElement : uuidSplited) {
        shortUrl += uuidElement;
      }
      if (database.saveShortUrlFile(projectId, folderName, filename, shortUrl) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error saving short url\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
    }

    return Response.status(Response.Status.OK).entity("{\"shortUrl\":\"" + shortUrl + "\"}")
        .type(MediaType.APPLICATION_JSON).build();
  }

  public static Response getFolderLink(final String token, final Long projectId) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error getting user email\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You have not permission to access this project\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }

    String projectUrl = database.getShortUrlProject(projectId);
    if (projectUrl == null) {
      UUID uuid = UUID.randomUUID();
      String[] urlSplited = uuid.toString().split("-");
      projectUrl = "";
      for (String split : urlSplited) {
        projectUrl += split;
      }

      if (database.saveShortUrlProject(projectId, projectUrl) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"message\":\"Error creating short url\"}").type(MediaType.APPLICATION_JSON).build();
      }

    }

    String response = "{\"shortUrl\":\"" + projectUrl + "\"}";
    return Response.status(Response.Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
  }

  public static Response createVersion(final String token, final Long projectId, final String name,
      final Boolean isPublic) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();

    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to create a version of this project\"}").build();
    }

    String commitId = database.getLastCommitProject(projectId);

    if (commitId == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while creating a version of this project\"}").build();
    }

    if (database.createVersion(projectId, commitId, name, isPublic) == -1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while creating a version of this project\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"message\":\"Version created successfully\"}").build();
  }
}
