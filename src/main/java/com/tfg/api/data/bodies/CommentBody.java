package com.tfg.api.data.bodies;

public class CommentBody {
  private Long projectId;
  private String writterEmail;
  private String commentText;
  private String responseCommentId;
  public CommentBody() {
  }
  public CommentBody(Long projectId, String writterEmail, String commentText, String responseCommentId) {
    this.projectId = projectId;
    this.writterEmail = writterEmail;
    this.commentText = commentText;
    this.responseCommentId = responseCommentId;
  }
  public Long getProjectId() {
    return projectId;
  }
  public void setProjectId(Long projcetId) {
    this.projectId = projcetId;
  }
  public String getWritterEmail() {
    return writterEmail;
  }
  public void setWritterEmail(String writterEmail) {
    this.writterEmail = writterEmail;
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
