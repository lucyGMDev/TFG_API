package com.tfg.api.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

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

  public Git getGit() {
    return git;
  }
  public void setGit(Git git) {
    this.git = git;
  }
  public Repository getRepository() {
    return repository;
  }
  public void setRepository(Repository repository) {
    this.repository = repository;
  }


  private File writeInfo(InputStream dStream, String path, String filename)
  {
    BufferedInputStream bis = null;
    BufferedOutputStream bos = null;
    try{
      bis = new BufferedInputStream(dStream);
      bos = new BufferedOutputStream(new FileOutputStream(path+"/"+filename));
      int i;
      while((i=bis.read()) != -1)
      {
        bos.write(i);
      }

    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if(bis!=null)
      {
        try{
          bis.close();
        }catch(Exception e)
        {
          e.printStackTrace();
        }
      }
      if(bos!=null)
      {
        try{
          bos.close();
        }catch(Exception e)
        {
          e.printStackTrace();
        }
      }
    }
    File file = new File(path+"/"+filename);
    return file;
  }

  public String addFile(InputStream dStream, String folderName,String filename) throws IOException, GitAPIException
  {
    String path = getRepository().getDirectory().getParentFile().getAbsolutePath()+"/"+folderName;
    File fileUpdated = writeInfo(dStream,path,filename);
    getGit().add().addFilepattern(fileUpdated.getName()).call();
    String commitMessage = "Add file: "+filename+" to folder "+folderName;
    RevCommit commit = getGit().commit().setMessage(commitMessage).call();
    return commit.getName();
  }

  
}
