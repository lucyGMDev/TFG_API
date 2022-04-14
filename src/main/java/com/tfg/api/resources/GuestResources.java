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

  @GET
  @Path("/project/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProject(@PathParam("projectId") final Long projectId) {
    return GuestController.getProject(projectId);
  }

  @GET
  @Path("/project/{projectId}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadProject(@PathParam("projectId") final Long projectId,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    return GuestController.downloadProject(projectId, versionName);
  }

  @GET
  @Path("/projects/user/{userEmail}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUsersProjects(@PathParam("userEmail") final String userEmail) {
    return GuestController.getUserProjects(userEmail);
  }

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

  @GET
  @Path("/project/{projectId}/getItems")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getItems(@PathParam("projectId") final Long projectId,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    return GuestController.getItems(projectId, versionName);
  }

  @GET
  @Path("/project/{projectId}/folder/{folderName}/getFiles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFilesFromFolder(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName,
      @QueryParam("version") @DefaultValue("") final String versionName) {
    return GuestController.getFilesFromFolder(projectId, folderName, versionName);
  }

  @GET
  @Path("/project/{projectId}/folder/{folderName}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFolderFromProject(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @QueryParam("version") final String versionName) {
    return GuestController.downloadFolderFromProjet(projectId, folderName, versionName);
  }

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

  @GET
  @Path("/project/{projectId}/folder/{folderName}/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFileFromFolder(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @PathParam("fileName") final String fileName,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    return GuestController.getFileFromFolder(projectId, folderName, fileName, versionName);
  }

  @GET
  @Path("/project/{projectId}/folder/{folderName}/file/{filename}/downloadFile")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadFile(@PathParam("projectId") final Long projectId,
      @PathParam("folderName") final String folderName, @PathParam("filename") final String filename,
      @QueryParam("versionName") @DefaultValue("") final String versionName) {
    return GuestController.downloadFile(projectId, folderName, filename, versionName);
  }

  @GET
  @Path("/project/{projectId}/getVersions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersions(@PathParam("projectId") final Long projectId) {
    return GuestController.getVersions(projectId);
  }
}
