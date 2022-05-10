package com.tfg.api.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import com.tfg.api.data.VersionList;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.FileUtils;
import com.tfg.api.utils.FolderUtils;
import com.tfg.api.utils.ProjectRepository;
import com.tfg.api.utils.ProjectUtils;
import com.tfg.api.utils.VersionsUtils;

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
      return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
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
   * Esta funcion descarga todos los elementos publicos de un proyecto determinado
   * para una version dada si esta es publica
   * 
   * @param projectId   Identificador del proyecto a descargar
   * @param versionName Version del proyecto a descargar
   * @return Devuelve una response con el proyecto a descargar
   */
  public static Response downloadProject(final Long projectId, final String versionName) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to download this project\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && !database.versionIsPublic(projectId, versionName)) {
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
          if (folderMetadata.getIsPublic()) {
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
            FileList files = FolderUtils.getPublicFilesFromFolder(projectId, directory.getName());
            final String directoryName = directory.getName();
            files.getFiles().stream().filter(file -> file.getIsPublic()).forEach(
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

    if (offset == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Offset is required\"}").build();
    }
    if (numberProjectsGet == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Number Projects Get is required\"}").build();
    }

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
   * Get a list with all items of a proyect on a determinated version, if an item
   * is private then dont send information about it
   * 
   * @param projectId   Id of the project
   * @param versionName Name of the project version to retrieve items
   * @return Response with a list with items to get
   */
  public static Response getItems(final Long projectId, final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to download this project\"}").build();
    }

    if (!(versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME")))
        && !database.versionIsPublic(projectId, versionName)) {
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
      itemsList = FolderUtils.getListItemsCensured(projectId);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting items from repository\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(itemsList))
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
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
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

    if (!FolderUtils.folderNameIsValid(folderName)) {
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

    try {
      if (!FolderUtils.userCanAccessFolder(projectId, folderName)) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this folder\"}").build();
      }
    } catch (Exception e2) {
      e2.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting files\"}").build();
    }

    FileList files;
    try {
      files = FolderUtils.getPublicFilesFromFolder(projectId, folderName);
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
      if (!versionName.equals("") || versionName.equals(environmentVariablesManager.get("CURRENT_VERSION_NAME"))) {
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
   * Download all public files from a folder
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the folder
   * @param versionName Name of the version to download
   * @return Response with a zip with all public files
   */
  public static Response downloadFolderFromProjet(final Long projectId, final String folderName,
      final String versionName) {
    Gson jsonManager = new Gson();
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
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
      if (!FolderUtils.userCanAccessFolder(projectId, folderName)) {
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
          if (!FileUtils.getMetadataFile(projectId, folderName, singleFile.getName()).getIsPublic()) {
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

  public static Response downloadFilesSelectedFromFolder(final Long projectId, final String folderName,
      final String versionName, final List<String> filesSelectedNames) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
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
      if (!FolderUtils.userCanAccessFolder(projectId, folderName)) {
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
                  && FileUtils.fileIsPublic(projectId, folderName, fileName);
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
   * Get a public file from a folder, from a public project with a determinated
   * version, if this version is public
   * 
   * @param projectId   of the project whose contains the file to get
   * @param folderName  of the folder whose contains the file to get
   * @param fileName    of the file to get
   * @param versionName of the version wich is the file to get
   * @return Response with the file to get
   */
  public static Response getFileFromFolder(final Long projectId, final String folderName, final String fileName,
      final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
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
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName);
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
      if (!FolderUtils.userCanAccessFolder(projectId, folderName)) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this project\"}").build();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file from folder\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, fileName)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current file does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    try {
      if (!FileUtils.fileIsPublic(projectId, folderName, fileName)) {
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
      metadataFile = FileUtils.getMetadataFile(projectId, folderName, fileName);
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
   * Download a file from a project on a determinated version wich is public
   * 
   * @param projectId   of the project whose contains the file to download
   * @param folderName  of the folder whose contains the file to download
   * @param fileName    of the file to download
   * @param versionName of the file to download
   * @return Response with the file to download
   */
  public static Response downloadFile(final Long projectId, final String folderName, final String fileName,
      final String versionName) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to access this project\"}").build();
    }

    if (!FolderUtils.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"The folder name is not valid\"}").build();
    }

    String commitIdVersion;
    try {
      commitIdVersion = ProjectUtils.getCommitIdVersion(projectId, versionName);
    } catch (NullPointerException npe) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    } catch (NotFoundException nfe) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any version with this name on this project\"}").build();
    } catch (AccessControlException ace) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
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
      if (!FolderUtils.userCanAccessFolder(projectId, folderName)) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permissions to download this file\"}").build();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file to download\"}").build();
    }

    if (!FileUtils.fileExists(projectId, folderName, fileName)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity(
              "{\"message\":\"There are not any file with this name on this project and this folder on this version\"}")
          .build();
    }

    try {
      if (!FileUtils.fileIsPublic(projectId, folderName, fileName)) {
        return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You have not permission to access this file\"}").build();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting file\"}").build();
    }

    String filePath = projectPath + File.separator + folderName + File.separator + fileName;
    File file = new File(filePath);

    return Response.status(Response.Status.OK).type(MediaType.MULTIPART_FORM_DATA).entity((Object) file)
        .header("Content-Disposition", "attachment; filename=" + file.getName()).build();
  }

  /**
   * Get all publics version of a project
   * 
   * @param projectId Id of the project to get versions
   * @return Response with public versions
   */
  public static Response getVersions(final Long projectId) {
    Gson jsonManager = new Gson();
    DBManager database = new DBManager();

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!database.projectIsPublic(projectId)) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"message\":\"You have not permission to access this project\"}").type(MediaType.APPLICATION_JSON)
          .build();
    }

    VersionList versions;
    try {
      versions = VersionsUtils.getVersionsProject(projectId);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while getting versions\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(versions))
        .build();
  }
}
