package com.tfg.api.data;

public class FolderMetadata {
  private Long numberViews;

  public FolderMetadata() {
  }

  
  public FolderMetadata(Long numberViews) {
    this.numberViews = numberViews;
  }


  public Long getNumberViews() {
    return numberViews;
  }

  public void setNumberViews(Long numberViews) {
    this.numberViews = numberViews;
  }

}
