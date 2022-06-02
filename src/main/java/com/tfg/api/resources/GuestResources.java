package com.tfg.api.resources;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.GuestController;

@Path("/guest")
public class GuestResources {

  /**
   * Get the information about a project
   * 
   * @param projectId Identifier of the project
   * @return
   */
  @GET
  @Path("/project/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProject(@PathParam("projectId") final Long projectId) {
    return GuestController.getProject(projectId);
  }

  /**
   * Download a project on a determinated version
   * 
   * @param projectId   Identifier of the project to download
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadProject(@PathParam("projectId") final Long projectId,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    return GuestController.downloadProject(projectId, versionName);
  }

  /**
   * Get all public projects from a user
   * 
   * @param username
   * @return
   */
  @GET
  @Path("/project/user/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUsersProjects(@PathParam("username") final String username) {
    return GuestController.getUserProjects(username);
  }

  /**
   * Search public projects
   * 
   * @param keyword        Keyword to search for
   * @param offset         Offset of the project to start to search from
   * @param numberProjects Number of projects to load
   * @param type           Filter search of a determinated type
   * @param order          Order the results of the search
   * @return
   */
  @GET
  @Path("/project/search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchProjects(@QueryParam("keyword") @DefaultValue("") final String keyword,
      @QueryParam("offset") final Long offset, @QueryParam("numberProjects") final Long numberProjects,
      @QueryParam("type") @DefaultValue("") final String type,
      @QueryParam("order") @DefaultValue("") final String order) {

    String[] typesArray = type.equals("") ? null
        : Arrays.stream(type.split(",")).map(singleType -> singleType.trim()).toArray(String[]::new);
    return GuestController.searchProjects(keyword, offset, numberProjects, typesArray, order);
  }

  /**
   * Get items from a project
   * 
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/getItems")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getItems(@PathParam("projectId") final Long projectId,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    return GuestController.getItems(projectId, versionName);
  }

  /**
   * Get files from a item
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/folder/{folderName}/getFiles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFilesFromFolder(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    return GuestController.getFilesFromFolder(projectId, folderName, versionName);
  }

  /**
   * Download all files from a item
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/folder/{folderName}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFolderFromProject(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @QueryParam("version") final String versionName) {
    return GuestController.downloadFolderFromProjet(projectId, folderName, versionName);
  }

  /**
   * Download a group of files from a item
   * 
   * @param projectId          Identifier of the project
   * @param folderName         Name of the item
   * @param versionName        Name of the version
   * @param filesSelectedNames Name of the files selected to download
   * @return
   */
  @POST
  @Path("/project/{projectId}/folder/{folderName}/downloadSelected")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFilesSelectedFromFolder(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName,
      @QueryParam("version") @DefaultValue("") final String versionName, List<String> filesSelectedNames) {

    return GuestController.downloadFilesSelectedFromFolder(projectId, folderName,
        versionName, filesSelectedNames);
  }

  /**
   * Get a file from a item
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param fileName    Name of the file
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/folder/{folderName}/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFileFromFolder(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @PathParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    return GuestController.getFileFromFolder(projectId, folderName, fileName, versionName);
  }

  /**
   * Download a file from a project on a determinated version
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param filename    Name of the file
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/folder/{folderName}/file/{filename}/downloadFile")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFile(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @PathParam("filename") final String filename,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    return GuestController.downloadFile(projectId, folderName, filename, versionName);
  }

  /**
   * Get all versions from a project
   * 
   * @param projectId Identifier of the project
   * @return
   */
  @GET
  @Path("/project/{projectId}/getVersions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersions(@PathParam("projectId") final Long projectId) {
    return GuestController.getVersions(projectId);
  }

  /**
   * Get a determinated file froma version
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param fileName    Name of the file
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/folder/{folderName}/file/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFileFromVersion(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") String folderName,
      @PathParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {

    return GuestController.getFileFromVersion(projectId, folderName, fileName, versionName);
  }

  /**
   * Download te content from a file
   * 
   * @param projectId   Identifier of the project
   * @param folderName  Name of the item
   * @param filename    Name of the file
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/folder/{folderName}/file/{filename}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFileContent(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") String folderName,
      @PathParam("filename") final String filename,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    return GuestController.downloadFileFromVersion(projectId, folderName, filename, versionName);
  }

  /**
   * Get all public historial messages from a project
   * 
   * @param projectId   Identifier of the project
   * @param versionName Name of the version
   * @return
   */
  @GET
  @Path("/project/{projectId}/getHistorial")
  public Response getHistorialProject(@PathParam("projectId") final Long projectId,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    return GuestController.getHistorialMessages(projectId, versionName);
  }
}
