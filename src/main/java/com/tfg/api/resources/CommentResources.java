package com.tfg.api.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.CommentController;
import com.tfg.api.data.bodies.CommentBody;

@Path("/comment")
public class CommentResources {
  
  @POST
  @Path("/postComment")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postComment(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, final CommentBody comment)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return CommentController.postComment(token, comment);
  }
}
