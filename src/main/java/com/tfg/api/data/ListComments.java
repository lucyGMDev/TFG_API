package com.tfg.api.data;

import java.util.ArrayList;

public class ListComments {
  private ArrayList<Comment> comments;

  public ListComments() {
    comments = new ArrayList<Comment>();
  }

  public ListComments(ArrayList<Comment> comments) {
    this.comments = comments;
  }

  public ArrayList<Comment> getComments() {
    return comments;
  }

  public void setComments(ArrayList<Comment> comments) {
    this.comments = comments;
  }
  
  
}
