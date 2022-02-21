package com.tfg.api.controllers;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.FileList;
import com.tfg.api.data.FolderMetadata;
import com.tfg.api.data.OrderFilter;
import com.tfg.api.data.Project;
import com.tfg.api.data.ProjectList;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.ProjectRepository;
import com.tfg.api.utils.ProjectUtils;

import org.eclipse.jgit.api.errors.GitAPIException;

import io.github.cdimascio.dotenv.Dotenv;

public class GuestController {

  /**
   * Get all data about a project who is public
   * 
   * @param projectId of the project wich you want to get
   * @return Response with the data of the project
   */
  public static Response getProject(final Long projectId) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to acces this project\"}").build();
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
   * Get all publics projects of an author
   * 
   * @param userEmail of the author
   * @return Response with all publics projects of the author with the email given
   */
  public static Response getUserProjects(final String userEmail) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();

    if (!database.userExistsByEmail(userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any user with this email\"}").build();
    }

    ProjectList projects = database.getProjectsFromUser(userEmail);
    if (projects == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are some problem while loading projects\"}").build();
    }

    projects.setProjectList(projects.getProjectList().stream()
        .filter(project -> project.getIsPublic())
        .collect(Collectors.toCollection(ArrayList::new)));

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(projects))
        .build();
  }

  /**
   * This functions search a group of public projects. This search can be
   * configured with a keyword to search, a group of type to projects to search,
   * and the order to show the results
   * 
   * @param keyword           to search projects
   * @param offset            number of projects to start to search
   * @param numberProjectsGet number of projects to load
   * @param typesArray        types of projects witch are searched
   * @param order             that we are using to show the projects
   * @return Response with a list of the projects founded
   */
  public static Response searchProjects(final String keyword, final Long offset, final Long numberProjectsGet,
      final String[] typesArray, final String order) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();

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
              ? database.searchProjectPublicsByTypes(numberProjectsGet, offset, keyword, typesArray)
              : database.searchProjectPublics(numberProjectsGet, offset, keyword);
          break;
        case RATING:
          projects = typesArray != null
              ? database.searchProjectPublicsByTypesOrderByRate(numberProjectsGet, offset, keyword,
                  typesArray)
              : database.searchProjectPublicsOrderByRate(numberProjectsGet, offset, keyword);
          break;
        default:
          return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
              .entity("{\"message\":\"Order filter is not valid\"}").build();
      }
    } else {
      projects = typesArray != null
          ? database.searchProjectPublicsByTypes(numberProjectsGet, offset, keyword, typesArray)
          : database.searchProjectPublics(numberProjectsGet, offset, keyword);
    }

    if (projects == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting projects\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(projects))
        .build();

  }

  /**
   * Get public files from a folder from a public project from a determinated
   * version wich must be public
   * 
   * @param projectId   of the project whose folder want to get
   * @param folderName  of the project whose publics files want to get
   * @param versionName of the project whose publics files want to get from a
   *                    determinated folder
   * @return Response with a list of public files from a folder from a public
   *         project in a determinated version
   */
  public static Response getFilesFromFolder(final Long projectId, final String folderName, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName);
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting files from folder\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You hve not permission to access this project\"}").build();
    }

    if (!ProjectUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any folder with this name on this project\"}").build();
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
      files = ProjectUtils.getFilesFromFolder(projectId, folderName);
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
      String folderMetadataPath = environmentVariablesManager.get("PROJECTS_ROOT") + File.separator + projectId
          + File.separator + folderName
          + ".json";
      File folderMetadataFile = new File(folderMetadataPath);
      FolderMetadata metadata;
      if (folderMetadataFile.exists()) {// TODO: Posiblemente sea mejor crear los metadatos cuando se crean las carpetas
        metadata = ProjectUtils.getMetadataFolder(projectId, folderName);
      } else {
        metadata = new FolderMetadata();
      }
      metadata.incrementNumberViews();
      String commitId = project.createMetadataFolder(jsonManager.toJson(metadata), folderName);
      if (!versionName.equals("")) {
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

}
