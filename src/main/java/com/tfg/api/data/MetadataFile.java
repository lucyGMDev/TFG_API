package com.tfg.api.data;

public class MetadataFile {
  private String description;
  private Boolean isPublic;
  public MetadataFile() {
  }
  public MetadataFile(String description, Boolean isPublic) {
    this.description = description;
    this.isPublic = isPublic;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public Boolean getIsPublic() {
    return isPublic;
  }
  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  
}
