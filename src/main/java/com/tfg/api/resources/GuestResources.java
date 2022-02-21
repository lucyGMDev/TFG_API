package com.tfg.api.resources;

import java.util.Arrays;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
  public Response getProject(@PathParam("projectId") final Long projectId){
    return GuestController.getProject(projectId);
  }

  @GET
  @Path("/projects/user/{userEmail}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUsersProjects(@PathParam("userEmail") final String userEmail){
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
    return GuestController.searchProjects(keyword,offset,numberProjects,typesArray,order);
  }

  @GET
  @Path("/project/{projectId}/folder/{folderName}/getFiles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFilesFromFolder(@PathParam("projectId")final Long projectId, @PathParam("folder")final String folderName,@QueryParam("versionName")@DefaultValue("") final String versionName)
  {
    return GuestController.getFilesFromFolder(projectId, folderName, versionName);
  }
}