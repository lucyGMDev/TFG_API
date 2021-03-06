package com.tfg.api.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import com.google.gson.Gson;
import com.tfg.api.data.FolderMetadata;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class ProjectRepository {
  Git git;
  Repository repository;

  public ProjectRepository(String repositoryPath) throws IllegalStateException, GitAPIException, IOException {
    File gitFile = new File(repositoryPath + File.separator + ".git");
    if (!gitFile.exists()) {
      this.git = Git.init().setDirectory(new File(repositoryPath)).call();
    }
    this.repository = new FileRepository(repositoryPath + File.separator + ".git");
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

  private File writeInfo(InputStream dStream, String path, String filename) {
    BufferedInputStream bis = null;
    BufferedOutputStream bos = null;
    try {
      bis = new BufferedInputStream(dStream);
      bos = new BufferedOutputStream(new FileOutputStream(path + File.separator + filename));
      int i;
      while ((i = bis.read()) != -1) {
        bos.write(i);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (bis != null) {
        try {
          bis.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (bos != null) {
        try {
          bos.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    File file = new File(path + File.separator + filename);
    return file;
  }

  public File createFileFromString(String fileContent, String path, String filename) {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(path + File.separator + filename));
      writer.write(fileContent);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    File file = new File(path + File.separator + filename);
    return file;
  }

  public String addFile(InputStream dStream, String folderName, String filename, String commitMessage)
      throws IOException, GitAPIException {
    String path = getRepository().getDirectory().getParentFile().getAbsolutePath() + File.separator + folderName;
    File fileUpdated = writeInfo(dStream, path, filename);
    getGit().add().addFilepattern(folderName + File.separator + fileUpdated.getName()).call();
    RevCommit commit = getGit().commit().setMessage(commitMessage).call();
    return commit.getName();
  }

  public String createFolder(String subFolderPath, String folderName) throws IOException, GitAPIException {
    Gson jsonManager = new Gson();
    File subFolder = new File(subFolderPath);
    subFolder.mkdirs();
    FolderMetadata folderMetadata = new FolderMetadata(folderName.trim());
    getGit().add().addFilepattern(folderName).call();
    String folderMetadataContent = jsonManager.toJson(folderMetadata);
    createMetadataFolder(folderMetadataContent, folderName);
    getGit().add().addFilepattern(folderName + ".json").call();
    RevCommit commit = getGit().commit().setMessage(String.format("Folder %s created", folderName)).call();
    return commit.getName();
  }

  public String createMetadataFile(String metadata, String folderName, String filename)
      throws IOException, GitAPIException {
    String metadataFilename = FileUtils.getMetadataFilename(filename);
    String path = getRepository().getDirectory().getParentFile().getAbsolutePath() + File.separator + folderName
        + File.separator + "metadata";
    File metadataFolder = new File(path);
    if (!metadataFolder.exists())
      metadataFolder.mkdirs();
    File metadataFile = createFileFromString(metadata, path, metadataFilename);
    getGit().add().addFilepattern(folderName + File.separator + "metadata" + File.separator + metadataFile.getName())
        .call();
    String commitMessage = "Create metadata file: " + metadataFilename + " to folder " + folderName;
    RevCommit commit = getGit().commit().setMessage(commitMessage).call();

    return commit.getName();
  }

  public String createMetadataFolder(String metadata, String folderName)
      throws IOException, GitAPIException {
    String path = getRepository().getDirectory().getParentFile().getAbsolutePath();
    File metadataFolder = new File(path);
    if (!metadataFolder.exists())
      metadataFolder.createNewFile();
    createFileFromString(metadata, path, folderName + ".json");
    getGit().add().addFilepattern(folderName + ".json").call();
    String commitMessage = "Create folder metadata file: " + folderName + ".json";
    RevCommit commit = getGit().commit().setMessage(commitMessage).call();

    return commit.getName();
  }

  public void changeVersion(String versionId) throws RefAlreadyExistsException, RefNotFoundException,
      InvalidRefNameException, CheckoutConflictException, GitAPIException {
    getGit().checkout().setName(versionId).call();
  }

  public String getCurrentVersion() throws NoHeadException, GitAPIException {
    RevCommit currentVersion = getGit().log().setMaxCount(1).call().iterator().next();
    String currentVersionId = currentVersion.getName();
    return currentVersionId;
  }

  public String removerFileFromProject(String path, String commitMessage)
      throws NoFilepatternException, GitAPIException {
    File file = new File(path);
    if (file.exists()) {
      System.out.println("Inicio borrado");
      file.delete();
      System.out.println("Borrado completado");
      getGit().add().addFilepattern(".").call();
      RevCommit commit = getGit().commit().setMessage(commitMessage).call();
      System.out.println("Commit generado");
      return commit.getName();
    }
    return null;
  }
}
