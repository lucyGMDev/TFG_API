package com.tfg.api.resources;

import java.io.InputStream;
import java.util.List;

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
  // TODO: Hacer methodo para descargar un solo proyecto
  @GET
  @Path("/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getProject(token, projectId);
  }

  @GET
  @Path("{projectId}/getItems")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getItems(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getItems(token, projectId, versionName);
  }

  @GET
  @Path("/user/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProjects(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("username") final String username) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getUserProjects(token, username);
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

  @GET
  @Path("/user/{usename}/search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchInUserProjects(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("usename") final String usename,
      @QueryParam("keyword") @DefaultValue("") final String keyword) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.searchInUserProjects(token, keyword, usename);
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

  @DELETE
  @Path("/{projectId}")
  public Response deleteProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.deleteProject(token, projectId);
  }

  @POST
  @Path("/{projectId}/folder/{folderName}/addFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @FormDataParam("description") final String description, @FormDataParam("isPublic") final Boolean isPublic,
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addFileToProject(projectId, token, folderName, description, isPublic, uploadedInputStream,
        fileDetail);
  }

  @GET
  @Path("{projectId}/folder/{folderName}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFolderFromProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @QueryParam("version") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.downloadFolderFromProjet(token, projectId, folderName, versionName);
  }

  @POST
  @Path("{projectId}/folder/{folderName}/downloadSelected")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFilesSelectedFromFolder(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName,
      @QueryParam("version") @DefaultValue("") final String versionName, List<String> filesSelectedNames) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.downloadFilesSelectedFromFolder(token, projectId, folderName,
        versionName, filesSelectedNames);
  }

  @PUT
  @Path("/{projectId}/folder/{folderName}/{isPublic}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFolderVisibility(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("isPublic") final Boolean isPublic,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateVisibilityFolder(token, projectId, folderName, isPublic, versionName);
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
  public Response deleteFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName) {
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

  @GET
  @Path("{projectId}/getVersions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersions(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getVersions(token, projectId);
  }

  @GET
  @Path("{projectId}/versionExist")
  @Produces(MediaType.APPLICATION_JSON)
  public Response projectVersionExits(@PathParam("projectId") final Long projectId,
      @QueryParam("versionName") final String versionName) {
    return ProjectController.projectVersionExists(projectId, versionName);
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

  @GET
  @Path("{projectId}/getHistorial")
  public Response getHistorialProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getHistorialMessages(token, projectId, versionName);
  }

  @GET
  @Path("{projectId}/getShortUrl")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getShortUrl(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("folderName") final String folderName,
      @QueryParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getShortUrl(token, projectId, folderName, fileName, versionName);
  }

  @POST
  @Path("{projectId}/getShortUrl")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getShortUrl(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("folderName") final String folderName,
      @QueryParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName,
      @QueryParam("shortUrl") final String shortUrl) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getShortUrl(token, projectId, folderName, fileName, versionName, shortUrl);
  }

  @GET
  @Path("/{projectId}/isAuthor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response isAuthor(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.isAuthor(token, projectId);
  }

}
