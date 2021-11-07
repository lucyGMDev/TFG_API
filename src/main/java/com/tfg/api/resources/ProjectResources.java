package com.tfg.api.resources;

import javax.ws.rs.Consumes;
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
    return ProjectController.addCoauthorToProject(projectId,token,coauthors.getCoauthors());
  }
}
