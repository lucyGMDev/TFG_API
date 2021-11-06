package com.tfg.api.controllers;

import java.io.File;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.Project;
import com.tfg.api.data.bodies.ProjectBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.JwtUtils;
import com.tfg.api.utils.ProjectRepository;

import io.github.cdimascio.dotenv.Dotenv;

public class ProjectController {
  public static Response createProject(final ProjectBody project, final String token) {
    //TODO: Validación de correos
    Gson gson = new Gson();
    Dotenv dotenv = Dotenv.load();
    JwtUtils jwtUtils = new JwtUtils();
    DBManager dbManager = new DBManager();
    String owner = jwtUtils.getUserEmailFromJwt(token);
    if(project.getOwner() == null) {
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

    if(project.getCoauthors() != null)
    {
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
    try{
      ProjectRepository repository = new ProjectRepository(projectPath);
    }catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error creating repository\"}").type(MediaType.APPLICATION_JSON).build();
    }

    String[] projectsSubdir = dotenv.get("PROJECT_SUBDIRS").split(",");
    for (String subDir : projectsSubdir) {
      String subFolderPath = projectPath + "/" + subDir;
      File subFolder = new File(subFolderPath);
      subFolder.mkdirs();
    }
    if(project.getCoauthors()!=null)
    {
      for (String coauthor : project.getCoauthors()) {
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

  public static Response updateProject(final Long projectId,final ProjectBody projectBody, final String token)
  {
    JwtUtils jwtUtils = new JwtUtils();
    DBManager dbManager = new DBManager();
    Gson gson = new Gson();
    String email = jwtUtils.getUserEmailFromJwt(token);
    String projectOwner = dbManager.getProjectOwner(projectId);

    Boolean canEdit = false;
    if(!email.equals(projectOwner)) {
      String[] projectCoauthors = dbManager.getProjectCoauthors(projectId);
      for(String coauthor : projectCoauthors)
      {
        if(coauthor.equals(email))
        {
          canEdit = true;
          break;
        }
      }
    }
    else{
      canEdit=true;
    }
    if(!canEdit)
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"You do not have permission to update the project \"}").type(MediaType.APPLICATION_JSON).build();
    }

    Project project = dbManager.getProjectById(projectId);
    if(projectBody.getName()!=null) {
      project.setName(projectBody.getName());
    }
    if(projectBody.getDescription()!=null) {
      project.setDescription(projectBody.getDescription());
    }
    if(projectBody.getIsPublic()!=null)
    {
      project.setIsPublic(projectBody.getIsPublic());
    }
    Project projectUpdated = dbManager.updateProject(projectId, project);

    return Response.status(Response.Status.OK).entity(gson.toJson(projectUpdated)).type(MediaType.APPLICATION_JSON).build();
  }
}
