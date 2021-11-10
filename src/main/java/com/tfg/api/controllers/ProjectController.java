package com.tfg.api.controllers;

import java.io.File;
import java.io.InputStream;

import javax.print.attribute.standard.Media;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.FileData;
import com.tfg.api.data.Project;
import com.tfg.api.data.bodies.ProjectBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.FileUtil;
import com.tfg.api.utils.JwtUtils;
import com.tfg.api.utils.ProjectRepository;
import com.tfg.api.utils.ProjectsUtil;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectController {
  public static Response createProject(final ProjectBody project, final String token) {
    // TODO: Validación de correos
    Gson gson = new Gson();
    Dotenv dotenv = Dotenv.load();
    JwtUtils jwtUtils = new JwtUtils();
    DBManager dbManager = new DBManager();
    String owner=null;
    try{
      owner = jwtUtils.getUserEmailFromJwt(token);
    }
    catch(Exception e)
    {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error with JWT\"}").type(MediaType.APPLICATION_JSON).build();
    }
    // TODO: Es posible que esta comprobación no sea necesaria y coger directamente
    // el email del token
    if (project.getOwner() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Owner is required\"}")
      .type(MediaType.APPLICATION_JSON).build();
    }
    if (!project.getOwner().equals(owner)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"Project owner does not match with login user\"}").type(MediaType.APPLICATION_JSON)
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
        if (!dbManager.UserExistsByEmail(coauthor)) {
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("{\"message\":\"You are trying to add a coauthor that already not exists\"}")
              .type(MediaType.APPLICATION_JSON).build();
        }
      }
    }

    Project projectCreated = dbManager.createProject(project);
    if (projectCreated == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error creating project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    Long projectId = projectCreated.getProject_id();
    String rootProjects = dotenv.get("PROJECTS_ROOT");
    String projectPath = rootProjects + "/" + projectId;
    final File projectFolder = new File(projectPath);
    projectFolder.mkdirs();

    String[] projectsSubdir = dotenv.get("PROJECT_SUBDIRS").split(",");
    for (String subDir : projectsSubdir) {
      String subFolderPath = projectPath + "/" + subDir.trim();
      File subFolder = new File(subFolderPath);
      subFolder.mkdirs();
    }

    try {
      ProjectRepository repository = new ProjectRepository(projectPath);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error creating repository\"}").type(MediaType.APPLICATION_JSON).build();
    }

    if (project.getCoauthors() != null) {
      for (String coauthor : project.getCoauthors()) {
        if (dbManager.userIsCoauthor(projectId, coauthor))
          continue;
        int result = dbManager.addCoauthorToProject(projectId, coauthor);
        if (result == -1) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\": \"Error while adding coauthor to project\"}").type(MediaType.APPLICATION_JSON)
              .build();
        }
      }
    }

    projectCreated.setCoauthors(project.getCoauthors());
    return Response.status(Response.Status.OK).entity(gson.toJson(projectCreated)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response updateProject(final Long projectId, final ProjectBody projectBody, final String token) {
    JwtUtils jwtUtils = new JwtUtils();
    DBManager dbManager = new DBManager();
    Gson gson = new Gson();
    String email = jwtUtils.getUserEmailFromJwt(token);

    if (!dbManager.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, email)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to update the project \"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    Project project = dbManager.getProjectById(projectId);
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
      projectUpdated = dbManager.updateProject(projectId, project);
    } else {
      projectUpdated = project;
    }

    return Response.status(Response.Status.OK).entity(gson.toJson(projectUpdated)).type(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Response addCoauthorFromProject(final Long projectId, String token, String[] coauthors) {
    JwtUtils jwtUtils = new JwtUtils();
    DBManager dbManager = new DBManager();
    String userEmail = jwtUtils.getUserEmailFromJwt(token);
    Gson gson = new Gson();

    if (coauthors == null || coauthors.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Coauthors list to add is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!dbManager.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to add coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    for (String coauthor : coauthors) {
      if (dbManager.UserExistsByEmail(coauthor) && !dbManager.userIsCoauthor(projectId, coauthor)) {
        if (dbManager.addCoauthor(projectId, coauthor) == -1) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("{\"message\":\"An error occurred while adding coauthor\"}").type(MediaType.APPLICATION_JSON)
              .build();
        }
      }
    }
    Project project = dbManager.getProjectById(projectId);

    return Response.status(Response.Status.OK).entity(gson.toJson(project)).type(MediaType.APPLICATION_JSON).build();
  }

  public static Response removeCoauthorsFromProject(final Long projectId, final String token,
      final String[] coauthors) {
    DBManager dbManager = new DBManager();
    Gson gson = new Gson();
    JwtUtils jwtUtils = new JwtUtils();
    String userEmail = jwtUtils.getUserEmailFromJwt(token);

    if (coauthors == null || coauthors.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Coauthors list to add is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!dbManager.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to remove coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    for (String coauthor : coauthors) {
      if (!dbManager.UserExistsByEmail(coauthor)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You are trying to remove an user who does not exist\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
      if (!dbManager.userIsCoauthor(projectId, coauthor)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"message\":\"You are trying to remove an user who does not coauthor of this project\"}")
            .type(MediaType.APPLICATION_JSON).build();
      }
    }

    for (String coauthor : coauthors) {
      if (dbManager.removeCoauthor(projectId, coauthor) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"message\":\"An error occurred while removing coauthor\"}").type(MediaType.APPLICATION_JSON)
            .build();
      }
    }

    Project project = dbManager.getProjectById(projectId);

    return Response.status(Response.Status.OK).entity(gson.toJson(project)).type(MediaType.APPLICATION_JSON).build();
  }

  public static Response addFileToProject(final Long projectId, final String token, final String folderName,
      final String description, final Boolean isPublic, final InputStream uploadedInputStream, final FormDataContentDisposition fileDetail) {

    DBManager dbManager = new DBManager();
    Dotenv dotenv = Dotenv.load();
    Gson gson = new Gson();
    JwtUtils jwtUtils = new JwtUtils();
    String userEmail = jwtUtils.getUserEmailFromJwt(token);
    String fileName = fileDetail.getFileName();

    if (!dbManager.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current project does not exist\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    
    if (dbManager.fileExists(projectId, folderName, fileName))
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"This file current exists on this folder\"}")
      .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to remove coauthors on this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if (folderName == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Folder name is required\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    if(uploadedInputStream == null || fileDetail == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"File is required\"}")
      .type(MediaType.APPLICATION_JSON).build();
    }

    if (!ProjectsUtil.folderNameIsValid(folderName)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Does not exist that folder\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }

    String projectPath = dotenv.get("PROJECTS_ROOT")+"/"+projectId;
    String commit = "";
    try{
      ProjectRepository projectRepository = new ProjectRepository(projectPath);
      commit = projectRepository.addFile(uploadedInputStream, folderName,fileName);
    }
    catch(Exception e)
    {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Server error was produced while uploading file\"}").type(MediaType.APPLICATION_JSON).build();
    }
    
    if(dbManager.insertFile(projectId, folderName, fileName, isPublic, description, userEmail)==-1)
    {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while uploading file\"}").type(MediaType.APPLICATION_JSON).build();
    }

    if(dbManager.addCommitProject(projectId, commit)==-1)
    {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while uploading file\"}").type(MediaType.APPLICATION_JSON).build();
    }
    
    FileData file = dbManager.getFile(projectId, folderName, fileName);
    if(file == null)
    {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while getting file\"}").type(MediaType.APPLICATION_JSON).build();
    }
    return Response.status(Response.Status.OK).entity(gson.toJson(file)).type(MediaType.APPLICATION_JSON).build();
  }

  public static Response getFile(String token,Long projectId, String folderName, String fileName)
  {
    DBManager dbManager = new DBManager();
    Gson gson = new Gson();
    JwtUtils jwtUtils = new JwtUtils();
    String userEmail = jwtUtils.getUserEmailFromJwt(token);

    FileData file = dbManager.getFile(projectId, folderName, fileName);
    if(file == null)
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"The current file does not exist\"}").type(MediaType.APPLICATION_JSON).build();
    }

    if(!ProjectsUtil.userCanAccessProject(projectId, userEmail))
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"You have not permission to access this project\"}").type(MediaType.APPLICATION_JSON).build();
    }
    
    if(!FileUtil.userCanAccessFile(projectId, folderName, fileName, userEmail))
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"You have not permission to access this file\"}").type(MediaType.APPLICATION_JSON).build();
    }
    
    
    return Response.status(Response.Status.OK).entity(gson.toJson(file)).type(MediaType.APPLICATION_JSON).build();
  }

  public static Response updateFile(String token, final Long projectId, final String folderName, final String fileName, final String description, final Boolean isPublic, final InputStream uploadedInputStream, final FormDataContentDisposition fileDetail)
  {
    DBManager dbManager = new DBManager();
    JwtUtils jwtUtils = new JwtUtils();
    Gson gson = new Gson();
    Dotenv dotenv = Dotenv.load();
    String userEmail = jwtUtils.getUserEmailFromJwt(token);


    FileData file = dbManager.getFile(projectId, folderName, fileName);
    if(file == null)
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Current file does not exist\"}").type(MediaType.APPLICATION_JSON).build();
    }

    if(!ProjectsUtil.userIsAuthor(projectId, userEmail))
    {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"message\":\"You do not have permission to update this project\"}")
          .type(MediaType.APPLICATION_JSON).build();
    }
    if(isPublic!=null)
    {
      file.setIsPublic(isPublic);
    }

    if(description!=null) 
    {
      file.setDescription(description);
    }
    if(uploadedInputStream!=null)
    {
      if(fileDetail.getName() != fileName)
      {
        if(FileUtil.renameFile(projectId,folderName,fileName,fileDetail.getFileName())==-1)
        {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while updating file\"}").type(MediaType.APPLICATION_JSON).build();
        }
        file.setFileName(fileDetail.getFileName());
      }
      String projectPath = dotenv.get("PROJECTS_ROOT");
      try
      {
        ProjectRepository repository = new ProjectRepository(projectPath+"/"+projectId);
        String commitId = repository.addFile(uploadedInputStream, folderName, fileDetail.getFileName());
        dbManager.addCommitProject(projectId, commitId);
      }
      catch(Exception e)
      {
        e.printStackTrace();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error updating file\"}").type(MediaType.APPLICATION_JSON).build();
      }
    }
    FileData fileUpdated = dbManager.updateFile(projectId, folderName, file.getFileName(), file);
    
    return Response.status(Response.Status.OK).entity(gson.toJson(fileUpdated)).type(MediaType.APPLICATION_JSON).build();
  }
}
