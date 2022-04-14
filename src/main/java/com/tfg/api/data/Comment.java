package com.tfg.api.data;

import java.util.Date;

public class Comment {
  String commentId;
  String writterEmail;
  String responseCommentId;
  Long projectId;
  Date postDate;
  String commentText;
  Long numberResponses;

  public Comment() {
  }

  public Comment(String commentId, String writterEmail, String responseCommentId, Long projectId, Date postDate,
      String commentText, Long numberResponses) {
    this.commentId = commentId;
    this.writterEmail = writterEmail;
    this.responseCommentId = responseCommentId;
    this.projectId = projectId;
    this.postDate = postDate;
    this.commentText = commentText;
    this.numberResponses = numberResponses;
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

  public String getResponseCommentId() {
    return responseCommentId;
  }

  public void setResponseCommentId(String responseCommentId) {
    this.responseCommentId = responseCommentId;
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

  public Long getNumberResponses() {
    return numberResponses;
  }

  public void setNumberResponses(Long numberResponses) {
    this.numberResponses = numberResponses;
  }

}
