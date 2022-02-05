package com.tfg.api.controllers;

import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.Comment;
import com.tfg.api.data.ListComments;
import com.tfg.api.data.bodies.CommentBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.JwtUtils;
import com.tfg.api.utils.ProjectsUtil;

public class CommentController {

  public static Response getComments(final String token, final Long projectId, final Long offset,
      final Long numberCommentsLoad) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (!ProjectsUtil.userCanAccessProject(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to post a comment on this project\"}").build();
    }

    ListComments comments = database.getComments(projectId, offset, numberCommentsLoad);
    if (comments == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any problem while loading comments\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(comments))
        .build();
  }

  public static Response getCommentResponses(String token, final Long projectId, final String commentId, final Long offset, final Long numberCommentsLoad)
  {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    }catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error with JWT\"}").build();
    }

    if(!database.projectExitsById(projectId)){
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity("{\"message\":\"There are not any project with this ID\"}").build();
    }

    if(!database.commentExistsInProject(commentId, projectId)){
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity("{\"message\":\"There are any comment on this project with this ID\"}").build();
    }

    if(!ProjectsUtil.userCanAccessProject(projectId, userEmail))
    {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity("{\"message\":\"You can not access this project\"}").build();
    }

    ListComments comments = database.getCommentResponses(projectId, commentId, offset, numberCommentsLoad);
    if(comments==null)
    {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity("{\"message\":\"Error while getting comments\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(comments)).build();
  }
  public static Response postComment(final String token, final CommentBody comment) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    if (comment.getCommentText() == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Body of comment is required\"}").build();
    }
    if (comment.getWritterEmail() == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Owner email of comment is required\"}").build();
    }
    if (comment.getProjectId() == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id of comment is required\"}").build();
    }
    if (!comment.getWritterEmail().equals(userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error on user validation\"}").build();
    }

    if (!database.projectExitsById(comment.getProjectId())) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }

    if (comment.getResponseCommentId() != null
        && !database.commentExistsInProject(comment.getResponseCommentId(), comment.getProjectId())) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You can not response to a comment who does not exist\"}").build();
    }

    if (!ProjectsUtil.userCanAccessProject(comment.getProjectId(), userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to post a comment on this project\"}").build();
    }

    String commentId = UUID.randomUUID().toString();
    if (database.addCommentToProject(comment.getProjectId(), commentId, comment.getCommentText(),
        comment.getWritterEmail(), comment.getResponseCommentId()) == -1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while post comment\"}").build();
    }

    Comment postedComment = database.getCommentById(commentId);
    if (postedComment == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting posted comment\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity(jsonManager.toJson(postedComment)).build();
  }

  public static Response deleteComment(final String token, final Long projectId, final String commentId) {
    DBManager database = new DBManager();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!database.commentExistsInProject(commentId, projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any comment with this id on this project\"}").build();
    }

    if (!ProjectsUtil.userIsAuthor(projectId, userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to edit this project\"}").build();
    }

    if (database.deleteComment(projectId, commentId) == -1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while deleting comment\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
        .entity("{\"message\":\"Comment was deleting successfully\"}").build();
  }

}
