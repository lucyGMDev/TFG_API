package com.tfg.api.controllers;

import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.Comment;
import com.tfg.api.data.ListComments;
import com.tfg.api.data.User;
import com.tfg.api.data.bodies.CommentBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.JwtUtils;
import com.tfg.api.utils.ProjectUtils;

public class CommentController {

  /***
   * A function to get a list of comments from a project
   * 
   * @param token              JSON Web Token with user email for authentication
   *                           or empty, if want to get comments from a public
   *                           project
   * @param projectId          The id of the project
   * @param offset             The number of comments since want to get comments.
   *                           Have to be more or equals to 0
   * @param numberCommentsLoad Number of comments to load. Have to be more or
   *                           equals 0
   * @return A response with a list of comments on JSON format
   */
  public static Response getComments(final String token, final Long projectId, final Long offset,
      final Long numberCommentsLoad) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = token.equals("") ? token : jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (projectId == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id is required\"}").build();
    }
    if (offset == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Offset is required\"}").build();
    }

    if (offset < 0) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Offset have to be more or equals than 0\"}").build();
    }

    if (numberCommentsLoad == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Number comments to load is required\"}").build();
    }

    if (numberCommentsLoad < 0) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Number comments to load have to be more or equals than 0\"}").build();
    }

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this id\"}").build();
    }
    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
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

  /**
   * A function to get a response list of a comment
   * 
   * @param token              JSONWebToken with user email
   * @param projectId          Identifier of a project
   * @param commentId          Identifier of a comment
   * @param offset             Number of comments to start to retrieve comments
   * @param numberCommentsLoad Number of comments to retrieve
   * @return A response with a list of response on JSON format
   */
  public static Response getCommentResponses(String token, final Long projectId, final String commentId,
      final Long offset, final Long numberCommentsLoad) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;
    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }

    User user = database.getUserByEmail(userEmail);

    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any project with this ID\"}").build();
    }

    if (!database.commentExistsInProject(commentId, projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any comment on this project with this ID\"}").build();
    }

    if (!ProjectUtils.userCanAccessProject(projectId, user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You can not access this project\"}").build();
    }

    ListComments comments = database.getCommentResponses(projectId, commentId, offset, numberCommentsLoad);
    if (comments == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error while getting comments\"}").build();
    }

    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(comments))
        .build();
  }

  /**
   * A function to post a comment on a project, or response to a comment
   * 
   * @param token   JWToken with user email who post the comment
   * @param comment A json with comment body
   * @return A response with the comment posted
   */
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
    User user = database.getUserByEmail(userEmail);
    if (comment.getCommentText() == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Body of comment is required\"}").build();
    }
    if (comment.getUsername() == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Owner email of comment is required\"}").build();
    }
    if (comment.getProjectId() == null) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Project id of comment is required\"}").build();
    }
    if (!comment.getUsername().equals(user.getUsername())) {
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
    if (comment.getResponseCommentId() != null) {
      if (database.getCommentById(comment.getResponseCommentId()).getResponseCommentId() != null) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"You can no response to a comment wich is a response\"}").build();
      }
    }

    if (!ProjectUtils.userCanAccessProject(comment.getProjectId(), user.getUsername())) {
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You have not permission to post a comment on this project\"}").build();
    }

    String commentId = UUID.randomUUID().toString();
    if (database.addCommentToProject(comment.getProjectId(), commentId, comment.getCommentText(),
        comment.getUsername(), comment.getResponseCommentId()) == -1) {
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

  /**
   * A functon to delete a comment from a project (only owners of projects can
   * delete comments)
   * 
   * @param token     JSONWebToken with user email who want to delete a comment
   * @param projectId Identifier of the project
   * @param commentId Identifier of the comment
   * @return
   */
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
    User user = database.getUserByEmail(userEmail);
    if (!database.projectExitsById(projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any project with this id\"}").build();
    }

    if (!database.commentExistsInProject(commentId, projectId)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are any comment with this id on this project\"}").build();
    }

    if (!ProjectUtils.userIsAuthor(projectId, user.getUsername())) {
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
