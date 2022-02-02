package com.tfg.api.data;

import java.util.ArrayList;

public class FileList {
  
  ArrayList<FileData> files;

  public FileList() {
    files = new ArrayList<FileData>();
  }

  public FileList(ArrayList<FileData> files) {
    this.files = files;
  }

  public ArrayList<FileData> getFiles() {
    return files;
  }

  public void setFiles(ArrayList<FileData> files) {
    this.files = files;
  }

  
}
