package com.tfg.api.data.bodies;

public class CommentBody {
  private Long projectId;
  private String username;
  private String commentText;
  private String responseCommentId;

  public CommentBody() {
  }

  public CommentBody(Long projectId, String username, String commentText, String responseCommentId) {
    this.projectId = projectId;
    this.username = username;
    this.commentText = commentText;
    this.responseCommentId = responseCommentId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projcetId) {
    this.projectId = projcetId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getCommentText() {
    return commentText;
  }

  public void setCommentText(String commentText) {
    this.commentText = commentText;
  }

  public String getResponseCommentId() {
    return responseCommentId;
  }

  public void setResponseCommentId(String responseCommentId) {
    this.responseCommentId = responseCommentId;
  }

}
