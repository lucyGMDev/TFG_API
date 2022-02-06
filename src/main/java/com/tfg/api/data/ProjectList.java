package com.tfg.api.data;

import java.util.ArrayList;

public class ProjectList {
  ArrayList<Project> projectList;

  public ProjectList() {
    projectList = new ArrayList<Project>();
  }

  public ProjectList(ArrayList<Project> projectList) {
    this.projectList = projectList;
  }

  public ArrayList<Project> getProjectList() {
    return projectList;
  }

  public void setProjectList(ArrayList<Project> projectList) {
    this.projectList = projectList;
  }

  
}
