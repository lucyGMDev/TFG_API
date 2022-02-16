package com.tfg.api.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.CommentController;
import com.tfg.api.data.bodies.CommentBody;

@Path("/comment")
public class CommentResources {
  
  @GET
  @Path("/{projectId}/getComments")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getComments(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, @QueryParam("offset") final Long offset, @QueryParam("numberCommentsLoad") final Long numberCommentsLoad)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return CommentController.getComments(token, projectId, offset,numberCommentsLoad);
  }
  @GET
  @Path("/{projectId}/getResponses/{commentId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCommentResponses(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, @PathParam("commentId") final String commentId, @QueryParam("offset") final Long offset, @QueryParam("numberCommentsLoad") final Long numberCommentsLoad){
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return CommentController.getCommentResponses(token, projectId, commentId, offset, numberCommentsLoad);
  }

  @POST
  @Path("/postComment")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postComment(@HeaderParam("Authorization") final String authorizationHeader, final CommentBody comment)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return CommentController.postComment(token, comment);
  }

  @DELETE
  @Path("/deleteComment/{projectId}/{commentId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteComment(@HeaderParam("Authorization") final String authorizationHeader, @PathParam("projectId") final Long projectId, @PathParam("commentId") final String commentId)
  {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return CommentController.deleteComment(token, projectId, commentId);
  }
}
