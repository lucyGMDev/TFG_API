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
  /**
   * Get all information about the project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @return
   */
  @GET
  @Path("/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getProject(token, projectId);
  }

  /**
   * Download a project on a determinated version
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("/{projectId}/download")
  @Produces(MediaType.APPLICATION_JSON)
  public Response downloadProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.downloadProject(token, projectId, versionName);
  }

  /**
   * Get all items in the project on a determinated version
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("{projectId}/getItems")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getItems(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getItems(token, projectId, versionName);
  }

  /**
   * Get all projects of a given user
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param username
   * @return
   */
  @GET
  @Path("/user/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProjects(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("username") final String username) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getUserProjects(token, username);
  }

  /**
   * Search project on the application
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param offset              Offset to start searching projects
   * @param numberProjectsLoad  Number of projects to load
   * @param keyword             Keyword to search for projects
   * @param type                Filter the search for types
   * @param order               Order the search results
   * @return
   */
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

  /**
   * Search between the user projects
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param usename
   * @param keyword             Keyword to search for projects
   * @return
   */
  @GET
  @Path("/user/{usename}/search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchInUserProjects(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("usename") final String usename,
      @QueryParam("keyword") @DefaultValue("") final String keyword) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.searchInUserProjects(token, keyword, usename);
  }

  /**
   * Create a new project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param project             Project information
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createProject(@HeaderParam("Authorization") final String authorizationHeader,
      final ProjectBody project) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.createProject(project, token);
  }

  /**
   * Update a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param project             Project information
   * @return
   */
  @PUT
  @Path("/{projectId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, final ProjectBody project) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateProject(projectId, project, token);
  }

  /**
   * Add coauthors to the project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param coauthors           List with coauthors username to add
   * @return
   */
  @PUT
  @Path("/{projectId}/addCoauthors")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addCoauthors(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, final ProjectBody coauthors) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addCoauthorToProject(projectId, token, coauthors.getCoauthors());
  }

  /**
   * Remove coauthors from the project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param coauthors           List with coauthors username to remove
   * @return
   */
  @PUT
  @Path("/{projectId}/removeCoauthors")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response removeCoauthors(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, final ProjectBody coauthors) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.removeCoauthorsFromProject(projectId, token, coauthors.getCoauthors());
  }

  /**
   * Delete a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @return
   */
  @DELETE
  @Path("/{projectId}")
  public Response deleteProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.deleteProject(token, projectId);
  }

  /**
   * Get information about a item
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("/{projectId}/folder/{folderName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getItem(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getItem(token, projectId, folderName, versionName);
  }

  /**
   * Add a file on a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param description         Description of the file
   * @param isPublic            Indicates if the file is public
   * @param showHistory         Indicates if the history of the file is shown
   * @param uploadedInputStream File to upload
   * @param fileDetail          Detail from the file
   * @return
   */
  @POST
  @Path("/{projectId}/folder/{folderName}/addFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @FormDataParam("description") final String description, @FormDataParam("isPublic") final Boolean isPublic,
      @FormDataParam("showHistory") final Boolean showHistory,
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.addFileToProject(projectId, token, folderName, description, isPublic, showHistory,
        uploadedInputStream,
        fileDetail);
  }

  /**
   * Download a item from a project on a determinated version
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("{projectId}/folder/{folderName}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFolderFromProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @QueryParam("version") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.downloadFolderFromProjet(token, projectId, folderName, versionName);
  }

  /**
   * Download a group of files from a item
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param versionName         Name of the version
   * @param filesSelectedNames  Name of the files selected to download
   * @return
   */
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

  /**
   * Update the visibility of a item
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param isPublic            Indicate if the item is public
   * @param versionName         Name of the version
   * @return
   */
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

  /**
   * Update the item history visibility on a determinated version
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param showHistory         Idicate if the item history is shown
   * @param versionName         Name of the version
   * @return
   */
  @PUT
  @Path("/{projectId}/folder/{folderName}/{showHistory}/changeShowHistory")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFolderHistoryVisibility(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("showHistory") final Boolean showHistory,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateShowHistoryFolder(token, projectId, folderName, showHistory, versionName);
  }

  /**
   * Get files from a item
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("/{projectId}/folder/{folderName}/getFiles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFilesFromFolder(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getFilesFromFolder(token, projectId, folderName, versionName);
  }

  /**
   * Get a file on a determinated version
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param versionName         Name of the version
   * @return
   */
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

  /**
   * Download a file on a determinated version
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param filename            Name of the file
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("/{projectId}/folder/{folderName}/file/{filename}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") String folderName,
      @PathParam("filename") final String filename,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.downloadFileFromVersion(token, projectId, folderName, filename, versionName);
  }

  /**
   * Update a file in the project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param description         Description of the file
   * @param isPublic            Indicates if the file is public
   * @param uploadedInputStream File to upload
   * @param fileDetail          Detail of the file
   * @return
   */
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

  /**
   * Update the visibility of the file
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param isPublic            Indicates if the file is public
   * @param versionName         Name of the version
   * @return
   */
  @PUT
  @Path("/{projectId}/folder/{folderName}/file/{fileName}/changeVisibility/{isPublic}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFileVisibility(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName,
      @PathParam("isPublic") final Boolean isPublic,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateFileVisibility(token, projectId, folderName, fileName, isPublic, versionName);
  }

  /**
   * Update the visibility of history of the file
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param showHistory         Indicates if the file history will be shown
   * @param versionName         Name of the version
   * @return
   */
  @PUT
  @Path("/{projectId}/folder/{folderName}/file/{fileName}/changeShowHistory/{showHistory}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFileHistoryVisibility(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName,
      @PathParam("showHistory") final Boolean showHistory,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.updateFileShowHistory(token, projectId, folderName, fileName, showHistory, versionName);
  }

  /**
   * Delete a file from the project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @return
   */
  @DELETE
  @Path("/{projectId}/folder/{folderName}/file/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteFile(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @PathParam("folderName") final String folderName,
      @PathParam("fileName") final String fileName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.removeFile(token, projectId, folderName, fileName);
  }

  /**
   * Rate a file from a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param versionName         Name of the version
   * @param score               Score for the file
   * @return
   */
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

  /**
   * Get a user rating from a file
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param filename            Name of the file
   * @param versionName         Name of the version
   * @return
   */
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

  /**
   * Get versions for a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @return
   */
  @GET
  @Path("{projectId}/getVersions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersions(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getVersions(token, projectId);
  }

  /**
   * Indicate if a version exists on a project
   * 
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("{projectId}/versionExist")
  @Produces(MediaType.APPLICATION_JSON)
  public Response projectVersionExits(@PathParam("projectId") final Long projectId,
      @QueryParam("versionName") final String versionName) {
    return ProjectController.projectVersionExists(projectId, versionName);
  }

  /**
   * Create a version on a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param name                Name of the version
   * @param isPublic            Indicates if a file is public
   * @return
   */
  @POST
  @Path("{projectId}/createVersion")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createVersion(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @FormDataParam("name") final String name,
      @FormDataParam("isPublic") final Boolean isPublic) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.createVersion(token, projectId, name, isPublic);
  }

  /**
   * Delente a version on a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param versionName         Name of the version
   * @return
   */
  @DELETE
  @Path("{projectId}/deleteVersion")
  public Response deleteVersion(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("name") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.deleteVersion(token, projectId, versionName);
  }

  /**
   * Get a version from a project by his name
   * 
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("{projectId}/getVersionFromName/{versionName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersonFromName(@PathParam("projectId") final Long projectId,
      @PathParam("versionName") final String versionName) {
    return ProjectController.getVersionFromName(projectId, versionName);
  }

  /**
   * Rate a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param score               Score for the projects
   * @return
   */
  @POST
  @Path("{projectId}/rateProject")
  @Produces(MediaType.APPLICATION_JSON)
  public Response rateProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId, @QueryParam("score") final Float score) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.rateProject(token, projectId, score);
  }

  /**
   * Get historial from a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param versionName         Name of the version
   * @return
   */
  @GET
  @Path("{projectId}/getHistorial")
  public Response getHistorialProject(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.getHistorialMessages(token, projectId, versionName);
  }

  /**
   * Get a short url for a project resource
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param versionName         Name of the version
   * @return
   */
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

  /**
   * Create a short url with a determinated url
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @param folderName          Name of the item
   * @param fileName            Name of the file
   * @param versionName         Name of the version
   * @param shortUrl            Short url
   * @return
   */
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

  /**
   * Get the element shared of a short url
   * 
   * @param shortUrl
   * @return
   * @throws Exception
   */
  @GET
  @Path("/shortUrl/{shortUrl}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getElementFromUrl(@PathParam("shortUrl") final String shortUrl) throws Exception {
    return ProjectController.getElementByShortUrl(shortUrl);
  }

  /**
   * Determinated if a user is author of a project
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param projectId           Identifier of the project
   * @return
   */
  @GET
  @Path("/{projectId}/isAuthor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response isAuthor(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("projectId") final Long projectId) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return ProjectController.isAuthor(token, projectId);
  }

}
