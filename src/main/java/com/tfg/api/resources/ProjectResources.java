package com.tfg.api.resources;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.ProjectController;
import com.tfg.api.data.bodies.ProjectBody;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Path("/project")
public class ProjectResources {
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createProject(@HeaderParam("Authorization") final String authorizationHeader,final ProjectBody project)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.createProject(project, token);
  }

  @PUT
  @Path("/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateProject(@HeaderParam("Authorization") final String authorizationHeader,@PathParam("projectId") final Long projectId ,final ProjectBody project)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateProject(projectId, project, token);
  }

  @PUT
  @Path("/{projectId}/addCoauthors")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addCoauthors(@HeaderParam("Authorization") final String authorizationHeader,@PathParam("projectId") final Long projectId, final ProjectBody coauthors)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addCoauthorFromProject(projectId,token,coauthors.getCoauthors());
  }

  @PUT
  @Path("/{projectId}/removeCoauthors")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeCoauthors(@HeaderParam("Authorization") final String authorizationHeader,@PathParam("projectId") final Long projectId, final ProjectBody coauthors)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.removeCoauthorsFromProject(projectId,token,coauthors.getCoauthors());
  }

  @POST
  @Path("/{projectId}/addFile")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response addFile(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, @FormDataParam("folder") final String folderName,@FormDataParam("description") final String description,@FormDataParam("isPublic") final Boolean isPublic, @FormDataParam("file") final InputStream uploadedInputStream, @FormDataParam("file") final FormDataContentDisposition fileDetail)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addFileToProject(projectId,token,folderName,description,isPublic,uploadedInputStream,fileDetail);
  }

  @GET
  @Path("/{projectId}/{folderName}/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFile(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, @PathParam("folderName")String folderName, @PathParam("fileName") final String fileName)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFile(token,projectId,folderName,fileName);
  }

  @PUT
  @Path("/{projectId}/{folderName}/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFile(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId")final Long projectId, @PathParam("folderName")final String folderName, @PathParam("fileName") final String fileName, @FormDataParam("description")final String description, @FormDataParam("isPublic") final Boolean isPublic, @FormDataParam("file") final InputStream uploadedInputStream, @FormDataParam("file") final FormDataContentDisposition fileDetail)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateFile(token, projectId, folderName, fileName, description, isPublic, uploadedInputStream, fileDetail);
  }
}
