package com.tfg.api.utils;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

public class ProjectRepository {
  Git git;
  Repository repository;
  public ProjectRepository(String repositoryPath) throws IllegalStateException, GitAPIException, IOException
  {
    File gitFile = new File(repositoryPath+"/.git");
    if(!gitFile.exists())
    {
      this.git = Git.init().setDirectory(new File(repositoryPath)).call();
    }
    this.repository = new FileRepository(repositoryPath+"/.git");
    this.git = new Git(this.repository);
    
  }
}
