package com.tfg.api.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.FileData;
import com.tfg.api.data.FileList;
import com.tfg.api.data.FolderMetadata;
import com.tfg.api.data.OrderFilter;
import com.tfg.api.data.Project;
import com.tfg.api.data.ProjectList;
import com.tfg.api.data.ShortUrlResource;
import com.tfg.api.data.User;
import com.tfg.api.data.Version;
import com.tfg.api.data.VersionList;
import com.tfg.api.data.bodies.ProjectBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.FileUtils;
import com.tfg.api.utils.FolderUtils;
import com.tfg.api.utils.HistorialMessages;
import com.tfg.api.utils.JwtUtils;
import com.tfg.api.utils.ProjectRepository;
import com.tfg.api.utils.ProjectUtils;
import com.tfg.api.utils.VersionsUtils;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectController {

  /**
   * Get all information about a project
   * 
   * @param token     Token with email of the user
   * @param projectId Identifier of the project
   * @return
   */
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
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"User can not access this project\"}").build();
    }

    Project userProject = database.getProjectById(projectId);
    if (userProject == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting project\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(userProject))
        .build();
  }

  /**
   * Download a poject on a determinated version
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  public static Response downloadProject(final String token, final long projectId, final String versionName) {

    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String userName;
    try {
      userName = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}")
          .build();
    }
    User user = database.getUserByEmail(userName);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to download this project\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && (!database.versionIsPublic(projectId, versionName)
            && !ProjectUtils.userIsAuthor(projectId, user.getUsername()))) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to download this version of the project\"}").build();
    }

    String folderPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    File projectFolder = new File(folderPath);
    ProjectRepository repository;
    String versionId = versionName.equals("")
        || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))
            ? database.getLastCommitProject(projectId)
            : database.getCommitIdFromVersion(projectId, versionName);
    try {
      repository = new ProjectRepository(folderPath);
      repository.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading project\"}").build();
    }
    Arrays.stream(projectFolder.listFiles()).filter(file -> file.getName().matches(".*\\.zip$"))
        .forEach((File file) -> {
          if (!file.delete()) {
            throw new RuntimeException();
          }
        });
    Project project = database.getProjectById(projectId);
    String projectZipPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId
        + File.separator + project.getName() + ".zip";

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(projectZipPath))) {
      List<File> folders = Arrays.stream(projectFolder.listFiles())
          .filter(file -> FolderUtils.folderNameIsValid(file.getName()) && file.isDirectory())
          .collect(Collectors.toList());
      try {
        folders.stream().filter((File file) -> {
          FolderMetadata folderMetadata;
          try {
            folderMetadata = FolderUtils.getMetadataFolder(projectId, file.getName());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          if (ProjectUtils.userIsAuthor(projectId, user.getUsername()) || folderMetadata.getIsPublic()) {
            folderMetadata.incrementNumberDownloads();
            try {
              String version = repository.createMetadataFolder(jsonManager.toJson(folderMetadata), file.getName());
              if (versionName.equals("")
                  || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
                database.addCommitProject(projectId, version);
              } else {
                database.updateVersionCommit(projectId, versionName, version);
              }
            } catch (IOException | GitAPIException e) {
              throw new RuntimeException(e);
            }
            return true;
          }
          return false;
        }).forEach((File directory) -> {
          try {
            FileList files = FolderUtils.getFilesFromFolder(projectId, directory.getName(),
                ProjectUtils.userIsAuthor(projectId, user.getUsername()));
            final String directoryName = directory.getName();
            files.getFiles().stream()
                .filter(file -> file.getIsPublic() || ProjectUtils.userIsAuthor(projectId, user.getUsername())).forEach(
                    (FileData fileData) -> {
                      File file = new File(
                          environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator
                              + fileData.getDirectoryName() + File.separator + fileData.getFileName());
                      try {
                        zipOutputStream.putNextEntry(new ZipEntry(directoryName + File.separator + file.getName()));
                        FileInputStream fis = new FileInputStream(file);
                        byte[] fileBytes = new byte[1024];
                        int length;
                        while ((length = fis.read(fileBytes)) >= 0) {
                          zipOutputStream.write(fileBytes, 0, length);
                        }
                        fis.close();
                        zipOutputStream.closeEntry();
                      } catch (IOException e) {
                        throw new RuntimeException(e);
                      }
                    });
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      } catch (RuntimeException e) {
        try {
          repository.changeVersion(versionId);
          if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
            database.addCommitProject(projectId, versionId);
          } else {
            database.updateVersionCommit(projectId, versionName, versionId);
          }
        } catch (GitAPIException e1) {
          e1.printStackTrace();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while downloading project\"}").build();
      }
    } catch (IOException e) {
      try {
        repository.changeVersion(versionId);
        if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
          database.addCommitProject(projectId, versionId);
        } else {
          database.updateVersionCommit(projectId, versionName, versionId);
        }
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading project\"}").build();
    }

    File zipFile = new File(projectZipPath);

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", project.getName()))
        .entity((Object) zipFile).build();

  }

  /**
   * Get projects from a user
   * 
   * @param token         Token with user email
   * @param ownerProjects Username of the projects owner
   * @return
   */
  public static Response getUserProjects(String token, String ownerProjects) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.userExistsByUsername(ownerProjects)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any user with this username\"}").build();
    }

    ProjectList projects = database.getProjectsFromUser(ownerProjects);
    if (projects == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are some problem while loading projects\"}").build();
    }

    projects.setProjectList(projects.getProjectList().stream()
        .filter(project -> project.getIsPublic() || Arrays.asList(project.getCoauthors()).contains(user.getUsername()))
        .collect(Collectors.toCollection(ArrayList::new)));

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(projects))
        .build();
  }

  /**
   * Search projects on the application
   * 
   * @param token             Token with user email
   * @param offset            Offset to start searching projects
   * @param numberProjectsGet number of projects to get
   * @param keyword           Keyword to search projects
   * @param typesArray        Array with projects types to filter search
   * @param order             Order of the search results
   * @return
   */
  public static Response searchProjects(final String token, final Long offset, final Long numberProjectsGet,
      final String keyword, final String[] typesArray, final String order) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (offset < 0 || numberProjectsGet < 0) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Offset and number of comments can not been negative\"}").build();
    }

    if (typesArray != null && !ProjectUtils.projectTypesAreValid(typesArray)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are some project filter invalid\"}").build();
    }

    ProjectList projects;
    if (!order.equals("")) {
      OrderFilter orderFilter = OrderFilter.valueOf(order);
      switch (orderFilter) {
        case LAST_UPDATE:
          projects = typesArray != null
              ? database.searchProjectByTypes(user.getUsername(), numberProjectsGet, offset, keyword, typesArray)
              : database.searchProject(user.getUsername(), numberProjectsGet, offset, keyword);
          break;
        case RATING:
          projects = typesArray != null
              ? database.searchProjectByTypesOrderByRate(user.getUsername(), numberProjectsGet, offset, keyword,
                  typesArray)
              : database.searchProjectOrderByRate(user.getUsername(), numberProjectsGet, offset, keyword);
          break;
        default:
          return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
              .entity("{\"message\":\"Order filter is not valid\"}").build();
      }
    } else {
      projects = typesArray != null
          ? database.searchProjectByTypes(user.getUsername(), numberProjectsGet, offset, keyword, typesArray)
          : database.searchProject(user.getUsername(), numberProjectsGet, offset, keyword);
    }

    if (projects == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting projects\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(projects))
        .build();
  }

  /**
   * Search between user projects
   * 
   * @param token   Token with user email
   * @param keyword Keyword to search projects
   * @param usename
   * @return
   */
  public static Response searchInUserProjects(final String token, final String keyword, final String usename) {
    DBManager database = new DBManager();
    JwtUtils jwtUtils = new JwtUtils();
    Gson jsonManager = new Gson();
    String email;
    try {
      email = jwtUtils.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(email);
    if (!database.userExistsByUsername(usename)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any users with that email\"}").build();
    }

    ProjectList projects = database.searchProjectsInUser(keyword, usename);
    if (projects == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting projects\"}").build();
    }

    projects.setProjectList(projects.getProjectList().stream()
        .filter(project -> ProjectUtils.userCanAccessProject(project.getProject_id(), user.getUsername()))
        .collect(Collectors.toCollection(ArrayList::new)));

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(projects))
        .build();
  }

  /**
   * Create a project
   * 
   * @param project Project information
   * @param token   Token with user email
   * @return
   */
  public static Response createProject(final ProjectBody project, final String token) {
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    String userEmail = null;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    if (!database.userExistsByEmail(userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"There are any user register with that email\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }

    User user = database.getUserByEmail(userEmail);

    if (project.getName() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Project name is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    if (project.getIsPublic() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Project privacity is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (project.getShowHistory() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Project show history is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (project.getType() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Project type is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectUtils.typesAreValid(project.getType())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"There are any type invalid for project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (project.getCoauthors() != null) {
      for (String coauthor : project.getCoauthors()) {
        if (!database.userExistsByUsername(coauthor)) {
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
    if (database.addCoauthorToProject(projectId, user.getUsername()) == -1) {
      database.deleteProject(projectId);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error creating repository\"}").type(MediaType.APPLICATION_JSON).build();
    }

    String rootProjects = environmentVariablesManager.get("PROJECTS_ROOT");
    String projectPath = rootProjects + File.separator + projectId;
    final File projectFolder = new File(projectPath);
    projectFolder.mkdirs();
    ProjectRepository projectRepository;
    try {
      projectRepository = new ProjectRepository(projectPath);
    } catch (IllegalStateException | GitAPIException | IOException e1) {
      database.deleteProject(projectId);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while creating project\"}").build();
    }

    String[] projectsSubdir = environmentVariablesManager.get("PROJECT_SUBDIRS").split(",");
    String commitId = null;
    for (String subDir : projectsSubdir) {
      String subFolderPath = projectPath + File.separator + subDir.trim();
      try {
        commitId = projectRepository.createFolder(subFolderPath, subDir);
      } catch (IOException | GitAPIException e) {
        e.printStackTrace();
        database.deleteProject(projectId);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while creating project\"}").build();
      }
    }

    if (commitId != null) {
      database.addCommitProject(projectId, commitId);
    }
    HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.CreateProject,
        user.getUsername(),
        new Date());
    String changeMessage = jsonManager.toJson(historialMessages);
    database.addChangeMessageProject(projectId, null, changeMessage, null, null);
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
        HistorialMessages addCoauthorhistorialMessage = new HistorialMessages(
            HistorialMessages.Operations.CoauthorAdded, user.getUsername(), coauthor, new Date());
        changeMessage = jsonManager.toJson(addCoauthorhistorialMessage);
        database.addChangeMessageProject(projectId, null, changeMessage, null, null);
      }
    }

    String response = String.format("{\"projectId\":%d}", projectId);
    return Response.status(Response.Status.OK).entity(response)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Update a project
   * 
   * @param projectId   Identifier of the project
   * @param projectBody Information about the project
   * @param token       Token with user email
   * @return
   */
  public static Response updateProject(final Long projectId, final ProjectBody projectBody, final String token) {
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

    if (userEmail == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error getting user email\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You do not have permission to update the project \"}")
          .build();
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
    if (projectBody.getShowHistory() != null) {
      project.setShowHistory(projectBody.getShowHistory());
    }
    if (projectBody.getType() != null) {
      project.setType(projectBody.getType());
    }

    Project projectUpdated = null;

    if (projectBody.getName() != null || projectBody.getDescription() != null || projectBody.getIsPublic() != null
        || projectBody.getType() != null || projectBody.getShowHistory()) {
      projectUpdated = database.updateProject(projectId, project);
      HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.UpdateProject,
          user.getUsername(),
          new Date());
      String changeMessage = jsonManager.toJson(historialMessages);
      database.addChangeMessageProject(projectId, null, changeMessage, null, null);
    } else {
      projectUpdated = project;
    }

    for (String coauthor : projectBody.getCoauthors()) {
      if (database.userIsCoauthor(projectId, coauthor))
        continue;
      if (database.addCoauthor(projectId, coauthor) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"message\":\"An error occurred while adding coauthor\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
      HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.CoauthorAdded,
          user.getUsername(),
          coauthor, new Date());
      String changeMessage = jsonManager.toJson(historialMessages);
      database.addChangeMessageProject(projectId, null, changeMessage, null, null);
    }
    if (projectUpdated == null) {
      projectUpdated = database.getProjectById(projectId);
    }
    projectUpdated.setCoauthors(projectBody.getCoauthors());
    return Response.status(Response.Status.OK).entity(jsonManager.toJson(projectUpdated))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Add coauthors to the project
   * 
   * @param projectId Identifier of the project
   * @param token     Token with user email
   * @param coauthors List of coauthor to add
   * @return
   */
  public static Response addCoauthorToProject(final Long projectId, String token, String[] coauthors) {
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
    User user = database.getUserByEmail(userEmail);
    if (coauthors == null || coauthors.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Coauthors list to add is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You do not have permission to add coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    for (String coauthor : coauthors) {
      if (!database.userExistsByUsername(coauthor)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You are trying to add an user who does not exist\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
      if (database.userIsCoauthor(projectId, coauthor)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You are trying to add an user who is coauthor of this project\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
    }

    for (String coauthor : coauthors) {
      if (database.addCoauthor(projectId, coauthor) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"message\":\"An error occurred while adding coauthor\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
      HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.CoauthorAdded,
          user.getUsername(),
          coauthor, new Date());
      String changeMessage = jsonManager.toJson(historialMessages);
      database.addChangeMessageProject(projectId, null, changeMessage, null, null);
    }

    Project project = database.getProjectById(projectId);

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(project)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Remove coauthor from a project
   * 
   * @param projectId Identifier of the project
   * @param token     Token with user email
   * @param coauthors List of coauthor to remove from the project
   * @return
   */
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

    User user = database.getUserByEmail(userEmail);

    if (coauthors == null || coauthors.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Coauthors list to add is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You do not have permission to remove coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    for (String coauthor : coauthors) {
      if (!database.userExistsByUsername(coauthor)) {
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
      HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.CoauthorRemoved,
          user.getUsername(), coauthor, new Date());
      String changeMessage = jsonManager.toJson(historialMessages);
      database.addChangeMessageProject(projectId, null, changeMessage, null, null);
    }

    Project project = database.getProjectById(projectId);

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(project)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Delete a project
   * 
   * @param token     Token with user email
   * @param projectId Identifier of the project
   * @return
   */
  public static Response deleteProject(final String token, final Long projectId) {
    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error whit JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You are not authorized to delete this project\"}").build();
    }
    ProjectUtils.deleteProject(projectId);
    if (database.deleteProject(projectId) == -1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while deleting project\"}").build();
    }

    return Response.status(Response.Status.OK).build();

  }

  /**
   * This function get all metadata for a given item of a project on a
   * determinated project version.
   * 
   * @param token       A json web token with useremail
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param versionName Name of the version
   * @return Response with item metadata
   */
  public static Response getItem(final String token, final Long projectId, final String folderName,
      final String versionName) {

    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You dont have permission to access this project\"}").build();
    }
    String versionId;
    try {
      versionId = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting item metadata\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    try {
      ProjectRepository project = new ProjectRepository(projectPath);
      project.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting item metadata\"}").build();
    }
    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this item\"}").build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting item metadata\"}").build();
    }
    FolderMetadata folderMetadata;
    try {
      folderMetadata = FolderUtils.getMetadataFolder(projectId, folderName);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting item metadata\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity(jsonManager.toJson(folderMetadata))
        .build();
  }

  /**
   * Update the visibility of a folder
   * 
   * @param token       JWToken with user email
   * @param projectId   Project id of project to change
   * @param folderName  Folder name whose visibility will be changed
   * @param isPublic    Change the folder to public if true or private if false
   * @param versionName Name of the version whose folder visibility will be
   *                    changed
   * @return Response with the result of the operation
   */
  public static Response updateVisibilityFolder(final String token, final Long projectId, final String folderName,
      final Boolean isPublic, final String versionName) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }
    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to update this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && database.versionExistsOnProject(projectId, versionName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    }

    String versionId = (versionName.equals("")
        || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
            ? database.getLastCommitProject(projectId)
            : database.getCommitIdFromVersion(projectId, versionName);

    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository projectRepository;
    try {
      projectRepository = new ProjectRepository(projectPath);
      projectRepository.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error updating folder permision\"}").build();
    }
    FolderMetadata folderMetadata;
    try {
      folderMetadata = FolderUtils.getMetadataFolder(projectId, folderName);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error updating folder permision\"}").build();
    }
    folderMetadata.setIsPublic(isPublic);
    String commitId;
    try {
      commitId = projectRepository.createMetadataFolder(jsonManager.toJson(folderMetadata), folderName);
    } catch (IOException | GitAPIException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error updating folder permision\"}").build();
    }
    if ((versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))) {
      database.addCommitProject(projectId, commitId);
    } else {
      database.updateVersionCommit(projectId, versionName, commitId);
    }

    String lastProjectVersion = database.getLastCommitProject(projectId);
    try {
      projectRepository.changeVersion(lastProjectVersion);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Update history visibility of a item
   * 
   * @param token       json web token with user email
   * @param projectId   Identifier of project
   * @param folderName  Name of the folder
   * @param showHistory Visibility of the history
   * @param versionName Name of the version to change
   * @return
   */
  public static Response updateShowHistoryFolder(final String token, final Long projectId, final String folderName,
      final Boolean showHistory, final String versionName) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }
    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to update this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && database.versionExistsOnProject(projectId, versionName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    }

    String versionId = (versionName.equals("")
        || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
            ? database.getLastCommitProject(projectId)
            : database.getCommitIdFromVersion(projectId, versionName);

    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository projectRepository;
    try {
      projectRepository = new ProjectRepository(projectPath);
      projectRepository.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error updating folder permision\"}").build();
    }
    FolderMetadata folderMetadata;
    try {
      folderMetadata = FolderUtils.getMetadataFolder(projectId, folderName);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error updating folder permision\"}").build();
    }
    folderMetadata.setShowHistory(showHistory);
    String commitId;
    try {
      commitId = projectRepository.createMetadataFolder(jsonManager.toJson(folderMetadata), folderName);
    } catch (IOException | GitAPIException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error updating folder permision\"}").build();
    }
    if ((versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))) {
      database.addCommitProject(projectId, commitId);
    } else {
      database.updateVersionCommit(projectId, versionName, commitId);
    }

    String lastProjectVersion = database.getLastCommitProject(projectId);
    try {
      projectRepository.changeVersion(lastProjectVersion);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Add a file to a project
   * 
   * @param projectId           Identifier of the project
   * @param token               Token with user email
   * @param folderName          Name of the item
   * @param description         Description of the file
   * @param isPublic            Indicates if the file is public
   * @param showHistorial       Indicates if the history of the file will be shown
   * @param uploadedInputStream File to upload
   * @param fileDetail          File to upload
   * @return
   */
  public static Response addFileToProject(final Long projectId, final String token, final String folderName,
      final String description, final Boolean isPublic, final Boolean showHistorial,
      final InputStream uploadedInputStream,
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

    User user = database.getUserByEmail(userEmail);

    if (folderName == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Folder name is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (uploadedInputStream == null || fileDetail == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"File is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (isPublic == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Privacity is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Does not exist that folder\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You do not have permission to add files on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String filename = fileDetail.getFileName();
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
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
    if (FileUtils.fileExists(projectId, folderName, filename)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"This file current exists on this folder\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String commit = "";
    try {
      HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.UploadFile,
          user.getUsername(),
          filename, new Date());
      historialMessages.setFolder(folderName);
      String commitMessage = jsonManager.toJson(historialMessages);
      projectRepository.addFile(uploadedInputStream, folderName, filename, commitMessage);
      database.addChangeMessageProject(projectId, null, commitMessage, folderName, null);
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
    FileData metadata;
    try {
      metadata = new FileData(filename, folderName, projectId, new Date(), new Date(), userEmail, description,
          isPublic, showHistorial, new HashMap<String, Integer>(), 0, 0L);
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
    FolderMetadata folderMetadata;
    try {
      folderMetadata = FolderUtils.getMetadataFolder(projectId, folderName);
      folderMetadata.setLastUpdated(new Date());
      commit = projectRepository.createMetadataFolder(jsonManager.toJson(folderMetadata), folderName);
    } catch (Exception e) {
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

    if (database.addCommitProject(projectId, commit) == -1) {
      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e) {
        e.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while uploading file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(metadata)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Download a item from a project
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param versionName Name of the version
   * @return
   */
  public static Response downloadFolderFromProjet(final String token, final Long projectId, final String folderName,
      final String versionName) {
    Gson jsonManager = new Gson();
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permissions to access this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"This folde name is not a valid folder name\"}").build();
    }

    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    String commitId = versionName.equals("")
        || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))
            ? database.getLastCommitProject(projectId)
            : database.getCommitIdFromVersion(projectId, versionName);
    try {
      project = new ProjectRepository(projectPath);
      project.changeVersion(commitId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading folder\"}").build();
    }
    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have no permissions to access this folder\"}").build();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading folder\"}").build();
    }
    String folderZipPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId
        + File.separator + folderName + ".zip";

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(folderZipPath))) {
      File folder = new File(projectPath + File.separator + folderName);
      List<File> filesDownload = Arrays.asList(folder.listFiles()).stream().filter(singleFile -> {
        if (singleFile.isDirectory()) {
          return false;
        }
        try {
          if (!FileUtils.userCanAccessFile(projectId, folderName, singleFile.getName(), user.getUsername())) {
            return false;
          }
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
        return true;
      }).collect(Collectors.toList());
      filesDownload.forEach(fileDownload -> {
        try {
          zipOutputStream.putNextEntry(new ZipEntry(fileDownload.getName()));
          FileInputStream fis = new FileInputStream(fileDownload);
          byte[] fileBytes = new byte[1024];
          int length;
          while ((length = fis.read(fileBytes)) >= 0) {
            zipOutputStream.write(fileBytes, 0, length);
          }
          fis.close();
          zipOutputStream.closeEntry();
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
        FileData fileMetadata;
        try {
          fileMetadata = FileUtils.getMetadataFile(projectId, folderName, fileDownload.getName());
          fileMetadata.incrementNumberDownloads();
          project.createMetadataFile(jsonManager.toJson(fileMetadata), folderName, fileDownload.getName());
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      });
      FolderMetadata folderMetadata;
      try {
        folderMetadata = FolderUtils.getMetadataFolder(projectId, folderName);
        folderMetadata.incrementNumberDownloads();
        String newCommitId = project.createMetadataFolder(jsonManager.toJson(folderMetadata), folderName);

        if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
          database.addCommitProject(projectId, newCommitId);
        } else {
          database.updateVersionCommit(projectId, versionName, newCommitId);
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }

    } catch (IOException | RuntimeException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading folder\"}").build();
    }

    File zipFile = new File(folderZipPath);

    return Response.status(Response.Status.OK)
        .header("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", folderName))
        .type(MediaType.APPLICATION_OCTET_STREAM).entity((Object) zipFile)
        .build();
  }

  /**
   * Download a group of files from a item
   * 
   * @param token              Token with user email
   * @param projectId          Identifier of the project
   * @param folderName         Name of the item
   * @param versionName        Name of the version
   * @param filesSelectedNames Name of the files selected to download
   * @return
   */
  public static Response downloadFilesSelectedFromFolder(final String token, final Long projectId,
      final String folderName,
      final String versionName, final List<String> filesSelectedNames) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error whit JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"This folder is not valid\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && !database.versionExistsOnProject(projectId, versionName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();

    }

    String commitId = versionName.equals("")
        || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))
            ? database.getLastCommitProject(projectId)
            : database.getCommitIdFromVersion(projectId, versionName);

    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository repository;
    try {
      repository = new ProjectRepository(projectPath);
      repository.changeVersion(commitId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading files\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON).entity("{\"message\": You have not permission to access this folder}")
            .build();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\": \"Error while downloading files\"}").build();
    }
    String folderZipPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId
        + File.separator + folderName + ".zip";
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(folderZipPath))) {
      filesSelectedNames.stream()
          .filter(fileName -> {
            try {
              return FileUtils.fileExists(projectId, folderName, fileName)
                  && FileUtils.userCanAccessFile(projectId, folderName, fileName, user.getUsername());
            } catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          }).map(fileName -> new File(projectPath + File.separator + folderName + File.separator + fileName))
          .forEach(fileDownload -> {
            try {
              zipOutputStream.putNextEntry(new ZipEntry(fileDownload.getName()));
              FileInputStream fis = new FileInputStream(fileDownload);
              byte[] fileBytes = new byte[1024];
              int length;
              while ((length = fis.read(fileBytes)) >= 0) {
                zipOutputStream.write(fileBytes, 0, length);
              }
              fis.close();
              zipOutputStream.closeEntry();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }

            FileData fileMetadata;
            try {
              fileMetadata = FileUtils.getMetadataFile(projectId, folderName, fileDownload.getName());
              fileMetadata.incrementNumberDownloads();
              String newVersionId = repository.createMetadataFile(jsonManager.toJson(fileMetadata), folderName,
                  fileDownload.getName());
              if (versionName.equals("")
                  || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
                database.addCommitProject(projectId, newVersionId);
              } else {
                database.updateVersionCommit(projectId, versionName, newVersionId);
              }
            } catch (Exception e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          });
    } catch (IOException | RuntimeException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading file\"}").build();
    }

    File zipFile = new File(folderZipPath);
    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition",
        String.format("attachment; filename=\"%s.zip\"", folderName)).entity((Object) zipFile).build();
  }

  /**
   * Get files frfom a item
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the folder
   * @param versionName Name of the version
   * @return
   */
  public static Response getFilesFromFolder(final String token, final Long projectId, final String folderName,
      final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
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
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting files from folder\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any folder with this name on this project\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this project\"}").build();
      }
    } catch (Exception e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting files from folder\"}").build();
    }

    String lastCommitVersion = database.getLastCommitProject(projectId);

    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(path);
      project.changeVersion(commitIdVersion);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting files\"}").build();
    }
    FileList files;
    try {
      files = FolderUtils.getFilesFromFolder(projectId, folderName,
          ProjectUtils.userIsAuthor(projectId, user.getUsername()));
    } catch (Exception e) {
      e.printStackTrace();
      try {
        project.changeVersion(lastCommitVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting files\"}").build();
    }

    try {// Una vez que he obtenido los archivos, añado la visita
      FolderMetadata metadata = FolderUtils.getMetadataFolder(projectId, folderName);
      metadata.incrementNumberViews();
      String commitId = project.createMetadataFolder(jsonManager.toJson(metadata), folderName);
      if (!versionName.equals("") && !versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
        database.updateVersionCommit(projectId, versionName, commitId);
      } else {
        database.addCommitProject(projectId, commitId);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      project.changeVersion(lastCommitVersion);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(files))
        .build();
  }

  /**
   * Get a file of a determinated version
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param filename    Name of the file
   * @param versionName Name of the version
   * @return
   */
  public static Response getFileFromVersion(String token, Long projectId, String folderName, String filename,
      String versionName) {
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
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You have not permission to access this project\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any folder with this name on this project\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this version\"}").build();
    }

    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project = null;

    try {
      project = new ProjectRepository(path);
      project.changeVersion(commitIdVersion);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this file\"}").build();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, filename)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current file does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    try {
      if (!FileUtils.userCanAccessFile(projectId, folderName, filename, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity("{\"message\":\"You have not permission to access this file\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while getting file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    FileData metadataFile;
    try {
      metadataFile = FileUtils.getMetadataFile(projectId, folderName, filename);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error getting file \"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String lastVersionId = database.getLastCommitProject(projectId);
    try {
      project.changeVersion(lastVersionId);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }
    return Response.status(Response.Status.OK).entity(jsonManager.toJson(metadataFile)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Change the file visibility on a determinated vesion of the project
   * 
   * @param token       Json Web Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the folder
   * @param fileName    Name of the file
   * @param isPublic    Set if file is public or not
   * @param versionName Name of the version to the project
   * @return
   */
  public static Response updateFileVisibility(final String token, final Long projectId, final String folderName,
      final String fileName, final Boolean isPublic, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to update this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }

    String commitId;
    try {
      commitId = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating visibility of file\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this version\"}").build();
    }
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository repository;

    try {
      repository = new ProjectRepository(projectPath);
      repository.changeVersion(commitId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating visibility of file\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, fileName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"The file do not exists\"}").build();
    }
    FileData fileMetadata;
    try {
      fileMetadata = FileUtils.getMetadataFile(projectId, folderName, fileName);
      fileMetadata.setIsPublic(isPublic);
      String newCommitId = repository.createMetadataFile(jsonManager.toJson(fileMetadata), folderName, fileName);
      if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
        database.addCommitProject(projectId, newCommitId);
      } else {
        database.updateVersionCommit(projectId, versionName, newCommitId);

      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating visibility\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).build();

  }

  /**
   * Changue the permissions to see the history on users who are not authors
   * 
   * @param token       Json Web Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the folder
   * @param fileName    Name of the file
   * @param showHistory Set if an user who is not an author can see the history
   * @param versionName Name of the version
   * @return
   */
  public static Response updateFileShowHistory(final String token, final Long projectId, final String folderName,
      final String fileName, final Boolean showHistory, final String versionName) {

    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to update this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }

    String commitId;
    try {
      commitId = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating visibility of file\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this version\"}").build();
    }
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository repository;

    try {
      repository = new ProjectRepository(projectPath);
      repository.changeVersion(commitId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating visibility of file\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, fileName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"The file do not exists\"}").build();
    }
    FileData fileMetadata;
    try {
      fileMetadata = FileUtils.getMetadataFile(projectId, folderName, fileName);
      fileMetadata.setShowHistorial(showHistory);
      String newCommitId = repository.createMetadataFile(jsonManager.toJson(fileMetadata), folderName, fileName);
      if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
        database.addCommitProject(projectId, newCommitId);
      } else {
        database.updateVersionCommit(projectId, versionName, newCommitId);

      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating visibility\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Download a fil on a determinated version
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the project
   * @param filename    Name of the file
   * @param versionName Name of the version
   * @return
   */
  public static Response downloadFileFromVersion(final String token, final Long projectId, final String folderName,
      final String filename, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"The folder name is not valid\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You hve not permission to access this version\"}").build();
    }

    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(projectPath);
      project.changeVersion(commitIdVersion);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error whule getting file\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission download this file\"}").build();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while downloading this file\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, filename)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity(
              "{\"message\":\"There are not any file with this name on this project and this folder on this version\"}")
          .build();
    }

    try {
      if (!FileUtils.userCanAccessFile(projectId, folderName, filename, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this file\"}").build();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    }

    String filePath = projectPath + File.separator + folderName + File.separator + filename;
    File file = new File(filePath);

    FileData fileMetadata;
    try {
      fileMetadata = FileUtils.getMetadataFile(projectId, folderName, filename);
      fileMetadata.incrementNumberDownloads();
      String commitId = project.createMetadataFile(jsonManager.toJson(fileMetadata), folderName, filename);
      if (versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
        database.addCommitProject(projectId, commitId);
      } else {
        database.updateVersionCommit(projectId, versionName, commitId);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return Response.status(Response.Status.OK).type(MediaType.MULTIPART_FORM_DATA).entity((Object) file)
        .header("Content-Disposition", "attachment; filename=" + file.getName()).build();
  }

  /**
   * Update a file on the project
   * 
   * @param token               Token with user email
   * @param projectId           Identifier of the project
   * @param folderName          Name of the folder
   * @param filename            Name of the file
   * @param description         Description of the file
   * @param isPublic            Indicates if the file is public
   * @param uploadedInputStream File to upload
   * @param fileDetail          File to upload
   * @return
   */
  public static Response updateFile(final String token, final Long projectId, final String folderName,
      final String filename,
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

    User user = database.getUserByEmail(userEmail);

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You do not have permission to update this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String lastProjectVersion = database.getLastCommitProject(projectId);
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository projectRepository;
    try {
      projectRepository = new ProjectRepository(projectPath);
      projectRepository.changeVersion(lastProjectVersion);
    } catch (IllegalStateException | GitAPIException | IOException e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while adding file to project\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this project\"}").build();
      }
    } catch (Exception e2) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while updating file\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, filename)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Current file does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    FileData fileMetadata;
    try {
      fileMetadata = FileUtils.getMetadataFile(projectId, folderName, filename);
    } catch (Exception e3) {
      e3.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while adding file to project\"}").build();
    }

    if (uploadedInputStream != null && fileDetail != null) {
      if (!fileDetail.getFileName().equals(filename)) {
        if (FileUtils.renameFile(projectId, folderName, filename, fileDetail.getFileName()) == -1) {
          try {
            projectRepository.changeVersion(lastProjectVersion);
          } catch (GitAPIException e) {
            e.printStackTrace();
          }
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\":\"Error while updating file\"}").type(MediaType.APPLICATION_JSON).build();
        }

        String oldMetadataFilename = File.separator + "metadata" + File.separator
            + FileUtils.getMetadataFilename(filename);
        String newMetadataFilename = File.separator + "metadata" + File.separator
            + FileUtils.getMetadataFilename(fileDetail.getFileName());

        if (FileUtils.renameFile(projectId, folderName, oldMetadataFilename, newMetadataFilename) == -1) {
          try {
            projectRepository.changeVersion(lastProjectVersion);
          } catch (GitAPIException e) {
            e.printStackTrace();
          }
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\":\"Error while updating file\"}").type(MediaType.APPLICATION_JSON).build();
        }
        fileMetadata.setFileName(fileDetail.getFileName());
      }

      try {
        HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.UpdateFile,
            user.getUsername(),
            filename, new Date());
        historialMessages.setFolder(folderName);
        String changeMessage = jsonManager.toJson(historialMessages);
        projectRepository.addFile(uploadedInputStream, folderName, fileDetail.getFileName(), changeMessage);
        database.addChangeMessageProject(projectId, null, changeMessage, folderName, fileDetail.getFileName());
      } catch (Exception e) {
        e.printStackTrace();
        try {
          projectRepository.changeVersion(lastProjectVersion);
          // database.addCommitProject(projectId, lastProjectVersion);
        } catch (GitAPIException e1) {
          e1.printStackTrace();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error updating file\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
    }

    if (description != null)
      fileMetadata.setDescription(description);
    if (isPublic != null)
      fileMetadata.setIsPublic(isPublic);
    fileMetadata.setLastUpdatedDate(new Date());

    String commitId;
    try {
      commitId = projectRepository.createMetadataFile(jsonManager.toJson(fileMetadata), folderName,
          fileMetadata.getFileName());
    } catch (IOException | GitAPIException e) {
      e.printStackTrace();
      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error updating file\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (database.addCommitProject(projectId, commitId) == -1) {
      try {
        projectRepository.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error updating file\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.OK).entity(jsonManager.toJson(fileMetadata))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  /**
   * Remove a file from the project
   * 
   * @param token      Token with user email
   * @param projectId  Identifier of the project
   * @param folderName Name of the folder
   * @param fileName   Name of the file
   * @return
   */
  public static Response removeFile(final String token, final Long projectId, final String folderName,
      final String fileName) {

    Dotenv environmentVariablesManager = Dotenv.load();
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);
    System.out.println("Tengo usuario");
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }
    System.out.println("Proyecto existe");

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to remove files on this project\"}").build();
    }
    System.out.println("Eres author");

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }
    System.out.println("Nombre carpeta valido");

    String lastVersionId = database.getLastCommitProject(projectId);
    System.out.println("Commit obtenido");
    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(path);
      project.changeVersion(lastVersionId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while deleting version\"}").build();
    }
    if (!FileUtils.fileExists(projectId, folderName, fileName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"This file does not exist\"}").build();
    }
    System.out.println("Archivo existe");

    try {
      path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId + File.separator + folderName
          + File.separator + fileName;
      HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.RemoveFile,
          user.getUsername(),
          fileName, new Date());
      String changeMessage = jsonManager.toJson(historialMessages);
      project.removerFileFromProject(path, changeMessage);
      System.out.println("Archivo eliminado");
      String metadataPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId
          + File.separator + folderName
          + File.separator + "metadata" + File.separator + FileUtils.getMetadataFilename(fileName);
      String commitId = project.removerFileFromProject(metadataPath, "Remove metadafile from " + fileName);
      System.out.println("Metadata eliminado");
      database.addChangeMessageProject(projectId, null, changeMessage, folderName, null);
      System.out.println("Guardado mensaje");
      database.addCommitProject(projectId, commitId);
      System.out.println("Guardado commit");
    } catch (GitAPIException e) {
      e.printStackTrace();
      try {
        project.changeVersion(lastVersionId);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while deleting version\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"message\":\"File deleted successfully\"}").build();
  }

  /**
   * Rate a file on a project
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the folder
   * @param filename    Name of the file
   * @param versionName Name of the version
   * @param score       Score for the file
   * @return
   */
  public static Response rateFile(final String token, final Long projectId, final String folderName,
      final String filename, final String versionName, final Integer score) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String lastProjectVersion = database.getLastCommitProject(projectId);
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);
    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project Id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .encoding("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (folderName == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is required\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }

    if (score == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Score is required\"}").build();
    }

    if (score < 0 || score > 5) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Score have to been between 0 and 5\"}").build();
    }

    if (filename == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Filename is required\"}").build();
    }

    if (versionName == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Version id is required\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while rating file\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this version\"}").build();
    }

    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(path);
      project.changeVersion(commitIdVersion);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while rating file\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to rate this file\"}").build();
      }
    } catch (Exception e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while rating file\"}").build();
    }

    try {
      if (!FileUtils.userCanAccessFile(projectId, folderName, filename, user.getUsername())) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have no permission to rate this file\"}").build();
      }
    } catch (Exception e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error while rating file\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, filename)) {
      try {
        project.changeVersion(lastProjectVersion);
      } catch (GitAPIException e) {
        e.printStackTrace();
      }
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"This file does not exist\"}").build();
    }

    FileData fileData;
    try {
      fileData = FileUtils.getMetadataFile(projectId, folderName, filename);
    } catch (Exception e) {
      e.printStackTrace();
      try {
        project.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while rating file\"}").build();
    }

    fileData.addScore(userEmail, score);
    String commitId;
    try {
      commitId = project.createMetadataFile(jsonManager.toJson(fileData, FileData.class), folderName, filename);
    } catch (IOException | GitAPIException e) {
      e.printStackTrace();
      try {
        project.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while rating file\"}").build();
    }

    if (versionName.equals("")) {
      if (database.addCommitProject(projectId, commitId) == -1) {
        try {
          project.changeVersion(lastProjectVersion);
        } catch (GitAPIException e) {
          e.printStackTrace();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error while rating file \"}").build();
      }
      lastProjectVersion = commitId;
    } else {
      if (database.updateVersionCommit(projectId, versionName, commitId) == -1) {
        try {
          project.changeVersion(lastProjectVersion);
        } catch (GitAPIException e) {
          e.printStackTrace();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error while rating file \"}").build();
      }
    }
    try {
      project.changeVersion(lastProjectVersion);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"message\":\"Rating done successfully\"}").build();
  }

  /**
   * Get the file rating for a file from a user
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the folder
   * @param filename    Name of the file
   * @param versionName Name of the version
   * @return
   */
  public static Response getFileRatingUser(final String token, final Long projectId, final String folderName,
      final String filename, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String lastProjectVersion = database.getLastCommitProject(projectId);
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project Id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (folderName == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is required\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Folder name is not valid\"}").build();
    }

    if (filename == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Filename is required\"}").build();
    }

    if (versionName == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Version id is required\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName, userEmail);
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting ratting\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You hve not permission to access this project\"}").build();
    }

    String path = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(path);
      project.changeVersion(commitIdVersion);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting rate\"}").build();
    }

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName, userEmail)) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this file\"}").build();
      }
    } catch (Exception e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting rating\"}").build();
    }

    try {
      if (!FileUtils.userCanAccessFile(projectId, folderName, filename, userEmail)) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have no permission to access to this file\"}").build();
      }
    } catch (Exception e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error while getting rating\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, filename)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"This file does not exist\"}").build();
    }

    FileData fileData;
    Integer score;
    try {
      fileData = FileUtils.getMetadataFile(projectId, folderName, filename);
      HashMap<String, Integer> scores = fileData.getScores();
      score = scores.get(userEmail);
    } catch (Exception e) {
      e.printStackTrace();
      try {
        project.changeVersion(lastProjectVersion);
      } catch (GitAPIException e1) {
        e1.printStackTrace();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting rate\"}").build();
    }
    try {
      project.changeVersion(lastProjectVersion);
    } catch (GitAPIException e) {
      e.printStackTrace();
    }

    String responseJson = String.format("{\"score\":%d}", score);
    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(responseJson).build();
  }

  /**
   * Get the short url for a project resource
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param fileName    Name of the file
   * @param versionName Name of the version
   * @return
   */
  public static Response getShortUrl(final String token, final Long projectId, final String folderName,
      final String fileName, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Yo have not permissions get a shortUrl\"}").build();
    }

    String versionId;
    try {
      versionId = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this version\"}").build();
    }
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(projectPath);
      project.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    }

    if (folderName != null) {
      if (!FolderUtils.folderNameIsValid(folderName)) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"This folder name is invalid\"}").build();
      }

    }

    if (fileName != null) {
      if (folderName == null) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Folder name is required to access to a file\"}").build();
      }
      if (!FileUtils.fileExists(projectId, folderName, fileName)) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"This file does not exist for this project\"}").build();
      }
    }
    String shortUrl = "";
    try {
      if (database.resourcesHasUrl(projectId, folderName, fileName, versionName)) {
        shortUrl = database.getShortUrl(projectId, folderName, fileName, versionName);
      }
    } catch (Exception e1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity(String.format("{\"shorUrl\":\"%s\"}", shortUrl)).build();

  }

  /**
   * Set a short url for a project resource
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param fileName    Name of the file
   * @param versionName Name of the version
   * @param shortUrl
   * @return
   */
  public static Response getShortUrl(final String token, final Long projectId, final String folderName,
      final String fileName, final String versionName, String shortUrl) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Yo have not permissions get a shortUrl\"}").build();
    }

    try {
      if (shortUrl != null && database.shortUrlExist(shortUrl)) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"This short url is already in use\"}").build();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    }

    String versionId;
    try {
      versionId = ProjectUtils.getCommitIdVersion(projectId, versionName, user.getUsername());
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this version\"}").build();
    }
    String projectPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    ProjectRepository project;
    try {
      project = new ProjectRepository(projectPath);
      project.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    }

    if (folderName != null) {
      if (!FolderUtils.folderNameIsValid(folderName)) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"This folder name is invalid\"}").build();
      }

    }

    if (fileName != null) {
      if (folderName == null) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Folder name is required to access to a file\"}").build();
      }
      if (!FileUtils.fileExists(projectId, folderName, fileName)) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"This file does not exist for this project\"}").build();
      }
    }
    try {
      if (database.resourcesHasUrl(projectId, folderName, fileName, versionName)) {
        shortUrl = database.getShortUrl(projectId, folderName, fileName, versionName);
      } else {
        if (shortUrl == null) {
          int maxIteration = 30;
          do {
            if (maxIteration <= 0) {
              return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                  .entity("{\"message\":\"Error while getting short url\"}").build();
            }
            shortUrl = "";
            UUID uuid = UUID.randomUUID();
            String[] uuidFields = uuid.toString().split("-");
            for (String field : uuidFields) {
              shortUrl += field;
            }
            shortUrl = shortUrl.substring(0, 16);
            maxIteration--;
          } while (database.shortUrlExist(shortUrl));
          if (database.insertShortUrl(shortUrl, projectId, folderName, fileName, versionName) == -1) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Error while getting short url\"}").build();
          }
        } else {
          if (database.insertShortUrl(shortUrl, projectId, folderName, fileName, versionName) == -1) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Error while getting short url\"}").build();
          }
        }
      }
    } catch (Exception e1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting short url\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity(String.format("{\"shorUrl\":\"%s\"}", shortUrl)).build();

  }

  /**
   * Create a version for a project
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @param isPublic    Indicates if the version is public
   * @return
   */
  public static Response createVersion(final String token, final Long projectId, final String versionName,
      final Boolean isPublic) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to create a version of this project\"}").build();
    }

    String commitId = database.getLastCommitProject(projectId);

    if (commitId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You can not create a version on an empty project\"}").build();
    }

    if (versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
      String responseMessage = String.format("{\"message\":\"You can not create a version with name %s\"}",
          environmentVariablesManager.get("CURRENT_VERSION_NAME"));
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity(responseMessage).build();
    }
    if (database.versionExistsOnProject(projectId, versionName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are a current version on this project with this name\"}").build();
    }

    if (database.createVersion(projectId, commitId, versionName, isPublic) == -1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while creating a version of this project\"}").build();
    }
    Version versionCreated = new Version(projectId, database.getCommitIdFromVersion(projectId, versionName),
        versionName, isPublic);
    HistorialMessages historialMessages = new HistorialMessages(HistorialMessages.Operations.CreateVerion,
        user.getUsername(),
        versionName, new Date());
    String changeMessage = jsonManager.toJson(historialMessages);
    database.addChangeMessageProject(projectId, null, changeMessage, null, null);
    database.addChangeMessageProject(projectId, versionName, changeMessage, null, null);

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity(jsonManager.toJson(versionCreated)).build();
  }

  /**
   * Get all versions for a project
   * 
   * @param token     Token with user email
   * @param projectId Identifier of the project
   * @return
   */
  public static Response getVersions(final String token, final Long projectId) {
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    DBManager database = new DBManager();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error validating JWT\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    User user = database.getUserByEmail(userEmail);

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You have not permission to access this project\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }

    VersionList versions;
    try {
      versions = VersionsUtils.getVersionsProject(projectId, ProjectUtils.userIsAuthor(projectId, user.getUsername()));
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while getting versions\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(versions))
        .build();
  }

  /**
   * Indicates if a version exists on a project
   * 
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  public static Response projectVersionExists(final Long projectId, final String versionName) {
    DBManager database = new DBManager();
    Boolean versionExists = database.versionExistsOnProject(projectId, versionName);
    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"exists\":" + versionExists + "}").build();
  }

  /**
   * Delete a version on a project
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  public static Response deleteVersion(final String token, final Long projectId, final String versionName) {

    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.versionExistsOnProject(projectId, versionName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (database.removeVersion(projectId, versionName) == -1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while deleting version\"}").build();
    }

    database.removeChangeMessagesFromVersion(projectId, versionName);

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"message\":\"Version removed successfully\"}").build();
  }

  /**
   * Get a version on a project by his name
   * 
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  public static Response getVersionFromName(final Long projectId, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    Version version;
    if (!versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
      if (!database.versionExistsOnProject(projectId, versionName)) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
      }
      version = database.getVersionFromName(projectId, versionName);
      if (version == null) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while getting version\"}").build();
      }
    } else {
      version = new Version(projectId, database.getCommitIdFromVersion(projectId, versionName), versionName,
          database.projectIsPublic(projectId));
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(version))
        .build();
  }

  /**
   * Rate a project
   * 
   * @param token     Token with user email
   * @param projectId Identifier of the project
   * @param score     Score for the project
   * @return
   */
  public static Response rateProject(final String token, final Long projectId, final Float score) {
    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername()) && !database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have no permissions to access this project\"}").build();
    }

    if (score == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Score is required\"}").build();
    }

    if (score < 0 || score > 5) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Score is invalid, must be between 0 and 5\"}").build();
    }

    if (!database.userHasRateProject(projectId, userEmail)) {
      if (database.rateProject(projectId, userEmail, score) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while rating project\"}").build();
      }
    } else {
      if (database.updateRateProject(projectId, userEmail, score) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while rating project\"}").build();
      }
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"message\":\"Project rate successfully\"}").build();
  }

  /**
   * Get history messages for a project
   * 
   * @param token       Token with user email
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  public static Response getHistorialMessages(final String token, final Long projectId, final String versionName) {
    JwtUtils jwtManager = new JwtUtils();
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to see the historial on this project\"}").build();
    }

    if (!versionName.equals("") && !versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))
        && !database.versionExistsOnProject(projectId, versionName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    }

    ArrayList<HistorialMessages> historial = database.getHistorialProject(projectId);
    if (historial == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting historial changes\"}").build();
    }
    ArrayList<HistorialMessages> historialFiltered = historial.stream().filter(historialMessage -> {
      if (ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
        return true;
      }
      if (historialMessage.getFile() != null && historialMessage.getFolder() != null
          && historialMessage.getProjectId() != null) {
        try {
          FileData fileMetadata = FileUtils.getMetadataFile(historialMessage.getProjectId(),
              historialMessage.getFolder(), historialMessage.getFile());
          return fileMetadata.getShowHistorial();
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
      if (historialMessage.getFile() == null && historialMessage.getFolder() != null
          && historialMessage.getProjectId() != null) {
        try {
          FolderMetadata folderMetadata = FolderUtils.getMetadataFolder(historialMessage.getProjectId(),
              historialMessage.getFolder());
          return folderMetadata.getShowHistory();
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
      if (historialMessage.getFile() == null && historialMessage.getFolder() == null
          && historialMessage.getProjectId() != null) {
        try {
          Project project = database.getProjectById(historialMessage.getProjectId());
          return project.getShowHistory();
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
      return false;
    }).collect(Collectors.toCollection(ArrayList::new));

    ArrayList<String> result = historialFiltered.stream().map(singleHistorial -> singleHistorial.toString())
        .collect(Collectors.toCollection(ArrayList::new));

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity(String.format("{\"historial\": %s, \"historialMetaData\":%s}", jsonManager.toJson(result),
            jsonManager.toJson(historialFiltered)))
        .build();
  }

  /**
   * Function which get the items of a project from a version
   * 
   * @param token       JWToken with user email
   * @param projectId   Identifier of project to get items
   * @param versionName Version of the project to get items
   * @return Response with the items
   */
  public static Response getItems(final String token, final Long projectId, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to download this project\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && !database.versionIsPublic(projectId, versionName)
        && !ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to download this version of the project\"}").build();
    }

    String versionId = versionName.equals("")
        || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))
            ? database.getLastCommitProject(projectId)
            : database.getCommitIdFromVersion(projectId, versionName);
    String repositoryPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId;
    try {
      ProjectRepository repository = new ProjectRepository(repositoryPath);
      repository.changeVersion(versionId);
    } catch (IllegalStateException | GitAPIException | IOException e) {
      e.printStackTrace();
    }

    ArrayList<FolderMetadata> itemsList = new ArrayList<FolderMetadata>();
    try {
      if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
        itemsList = FolderUtils.getListItemsCensured(projectId);
      } else {
        itemsList = FolderUtils.getListItems(projectId);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting items from repository\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(itemsList))
        .build();
  }

  /**
   * Indicate if a user is author of a project
   * 
   * @param token     Token with user email
   * @param projectId Identifier of the project
   * @return
   */
  public static Response isAuthor(String token, Long projectId) {
    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    Gson jsonManager = new Gson();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error whit JWT\"}").build();
    }
    User user = database.getUserByEmail(userEmail);
    Boolean isAuthor = ProjectUtils.userIsAuthor(projectId, user.getUsername());

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(isAuthor))
        .build();
  }

  /**
   * Get a resource of a project by his short url
   * 
   * @param shortUrl
   * @return
   * @throws Exception
   */
  public static Response getElementByShortUrl(final String shortUrl) throws Exception {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    if (!database.shortUrlExist(shortUrl)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any resource with this url\"}").build();
    }
    ShortUrlResource resource = database.getShortUrlResource(shortUrl);

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(resource))
        .build();
  }

}
