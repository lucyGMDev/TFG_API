package com.tfg.api.data;

import java.util.ArrayList;

public class VersionList {
  private ArrayList<Version> versionList;

  public VersionList() {
    this.versionList = new ArrayList<Version>();
  }

  public VersionList(ArrayList<Version> versionList) {
    this.versionList = versionList;
  }

  public ArrayList<Version> getVersionList() {
    return versionList;
  }

  public void setVersionList(ArrayList<Version> versionList) {
    this.versionList = versionList;
  }
  
}
