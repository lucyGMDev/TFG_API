package com.tfg.api.data.bodies;

public class CommentBody {
  private String writterEmail;
  private String commentText;
  private String responseCommentId;
  public CommentBody() {
  }
  public CommentBody(String writterEmail, String commentText, String responseCommentId) {
    this.writterEmail = writterEmail;
    this.commentText = commentText;
    this.responseCommentId = responseCommentId;
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
