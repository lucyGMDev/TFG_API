package com.tfg.api.resources;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.ProjectController;
import com.tfg.api.data.bodies.ProjectBody;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/project")
public class ProjectResources {

  @GET
  @Path("/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getProject(token, projectId);
  }

  @GET
  @Path("/user/{userEmail}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProjects(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("userEmail") final String userEmail) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getUserProjects(token, userEmail);
  }

  @GET
  @Path("/search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchProjects(@HeaderParam("Authorization") final String authorizationHeader,
      @QueryParam("offset") final Long offset, @QueryParam("numberProjectsLoad") final Long numberProjectsLoad,
      @QueryParam("keyword") @DefaultValue("") final String keyword,
      @QueryParam("type") @DefaultValue("") final String type,
      @QueryParam("order") @DefaultValue("") final String order) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    String[] typesArray = type.equals("") ? null : type.split(",");
    return ProjectController.searchProjects(token, offset, numberProjectsLoad, keyword, typesArray, order);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createProject(@HeaderParam("Authorization") final String authorizationHeader,
      final ProjectBody project) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.createProject(project, token);
  }

  @PUT
  @Path("/{projectId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, final ProjectBody project) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateProject(projectId, project, token);
  }

  @PUT
  @Path("/{projectId}/addCoauthors")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addCoauthors(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, final ProjectBody coauthors) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addCoauthorToProject(projectId, token, coauthors.getCoauthors());
  }

  @PUT
  @Path("/{projectId}/removeCoauthors")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response removeCoauthors(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, final ProjectBody coauthors) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.removeCoauthorsFromProject(projectId, token, coauthors.getCoauthors());
  }

  @POST
  @Path("/{projectId}/addFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @FormDataParam("folder") final String folderName,
      @FormDataParam("description") final String description, @FormDataParam("isPublic") final Boolean isPublic,
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addFileToProject(projectId, token, folderName, description, isPublic, uploadedInputStream,
        fileDetail);
  }

  @GET
  @Path("/{projectId}/folder/{folderName}/getFiles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFilesFromFolder(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFilesFromFolder(token, projectId, folderName, versionName);
  }

  @GET
  @Path("/{projectId}/folder/{folderName}/file/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFileFromVersion(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") String folderName,
      @PathParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFileFromVersion(token, projectId, folderName, fileName, versionName);
  }

  @GET
  @Path("/{projectId}/folder/{folderName}/file/{filename}/downloadFile")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") String folderName,
      @PathParam("filename") final String filename,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.downloadFileFromVersion(token, projectId, folderName, filename, versionName);
  }

  @PUT
  @Path("/{projectId}/folder/{folderName}/file/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName, @FormDataParam("description") final String description,
      @FormDataParam("isPublic") final Boolean isPublic, @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateFile(token, projectId, folderName, fileName, description, isPublic,
        uploadedInputStream, fileDetail);
  }

  @DELETE
  @Path("/{projectId}/folder/{folderName}/file/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteFile(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName, @PathParam("fileName") final String fileName){
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.removeFile(token, projectId, folderName, fileName);
  }

  @POST
  @Path("/{projectId}/folder/{folderName}/file/{fileName}/rateFile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response rateFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName,
      @QueryParam("score") final Integer score) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.rateFile(token, projectId, folderName, fileName, versionName, score);
  }

  @GET
  @Path("/{projectId}/folder/{folderName}/file/{fileName}/getUserRating")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFileRatingUser(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String filename,
      @QueryParam("vesionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFileRatingUser(token, projectId, folderName, filename, versionName);
  }

  @POST
  @Path("/{projectId}/{folderName}/{fileName}/getlink")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFileLink(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFileLink(token, projectId, folderName, fileName);
  }

  @POST
  @Path("{projectId}/getlink")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFolderLink(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFolderLink(token, projectId);
  }

  @GET
  @Path("{projectId}/getVersions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersions(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getVersions(token, projectId);
  }

  @POST
  @Path("{projectId}/createVersion")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createVersion(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @FormDataParam("name") final String name,
      @FormDataParam("isPublic") final Boolean isPublic) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.createVersion(token, projectId, name, isPublic);
  }

  @DELETE
  @Path("{projectId}/deleteVersion")
  public Response deleteVersion(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("name") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.deleteVersion(token, projectId, versionName);
  }

  @POST
  @Path("{projectId}/rateProject")
  @Produces(MediaType.APPLICATION_JSON)
  public Response rateProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("score") final Float score) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.rateProject(token, projectId, score);
  }

}
