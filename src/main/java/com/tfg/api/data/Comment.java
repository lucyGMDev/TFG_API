package com.tfg.api.data;

import java.util.Date;

public class Comment {
  String commentId;
  String writterEmail;
  String responseCommitId;
  Long projectId;
  Date postDate;
  String commentText;
  public Comment() {
  }
  public Comment(String commentId, String writterEmail, String responseCommitId, Long projectId, Date postDate,
      String commentText) {
    this.commentId = commentId;
    this.writterEmail = writterEmail;
    this.responseCommitId = responseCommitId;
    this.projectId = projectId;
    this.postDate = postDate;
    this.commentText = commentText;
  }
  public String getCommentId() {
    return commentId;
  }
  public void setCommentId(String commentId) {
    this.commentId = commentId;
  }
  public String getWritterEmail() {
    return writterEmail;
  }
  public void setWritterEmail(String writterEmail) {
    this.writterEmail = writterEmail;
  }
  public String getResponseCommitId() {
    return responseCommitId;
  }
  public void setResponseCommitId(String responseCommitId) {
    this.responseCommitId = responseCommitId;
  }
  public Long getProjectId() {
    return projectId;
  }
  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
  public Date getPostDate() {
    return postDate;
  }
  public void setPostDate(Date postDate) {
    this.postDate = postDate;
  }
  public String getCommentText() {
    return commentText;
  }
  public void setCommentText(String commentText) {
    this.commentText = commentText;
  }

  

}
