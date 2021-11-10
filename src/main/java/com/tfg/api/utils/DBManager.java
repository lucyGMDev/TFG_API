package com.tfg.api.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.tfg.api.data.FileData;
import com.tfg.api.data.Project;
import com.tfg.api.data.User;
import com.tfg.api.data.bodies.ProjectBody;

import io.github.cdimascio.dotenv.Dotenv;

public class DBManager {
  private String url;
  private String username;
  private String password;

  public DBManager() {
    Dotenv dotenv = Dotenv.load();
    url = dotenv.get("DB_URL");
    username = dotenv.get("DB_USERNAME");
    password = dotenv.get("DB_PASSWORD");
  }

  public Boolean UserExistsByEmail(String email) {
    String query = "SELECT * FROM users WHERE email = ?";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, email);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return true;
      }

    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public User insertUser(String email) {
    String query = "INSERT INTO users (email, created_date) VALUES (?,CURRENT_DATE)";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setString(1, email);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        ResultSet result = statement.getGeneratedKeys();
        if (result.next()) {
          User user = new User(result.getString("email"), result.getString("username"), result.getString("name"),
              result.getString("last_name"), result.getDate("created_date"));
          return user;
        }
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public User getUserByEmail(String email) {
    String query = "SELECT * FROM users WHERE email = ?";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, email);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        User user = new User(result.getString("email"), result.getString("username"), result.getString("name"),
            result.getString("last_name"), result.getDate("created_date"));
        return user;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Long getLastId() {
    String query = "SELECT project_id FROM project ORDER BY project_id DESC LIMIT 1";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      ResultSet result = statement.executeQuery();
      while (result.next()) {
        return result.getLong("project_id");
      }
      return (long) 0;

    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Project createProject(ProjectBody projectBody) {
    String query = "INSERT INTO project (name,description,created_date,last_update_date,public,owner) VALUES(?,?,CURRENT_DATE,CURRENT_DATE,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setString(1, projectBody.getName());
      statement.setString(2, projectBody.getDescription());
      statement.setBoolean(3, projectBody.getIsPublic());
      statement.setString(4, projectBody.getOwner());
      int rowsInserted = statement.executeUpdate();
      if (rowsInserted == 0) {
        return null;
      }
      ResultSet result = statement.getGeneratedKeys();
      if (result.next()) {
        Project project = new Project(result.getLong("project_id"), result.getString("name"),
            result.getString("description"), result.getDate("created_date"), result.getDate("last_update_date"),
            result.getString("last_commit_id"), result.getBoolean("public"), result.getString("owner"), null);
        return project;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Boolean projectExitsById(Long projectId) {
    String query = "SELECT * FROM project WHERE project_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return true;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public int addCoauthorToProject(Long projectId, String coauthorEmail) {
    String query = "INSERT INTO coauthor_project VALUES (?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, coauthorEmail);
      statement.setLong(2, projectId);
      int rowInserted = statement.executeUpdate();
      if (rowInserted > 0) {
        return rowInserted;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public String getProjectOwner(Long projectId) {
    String query = "SELECT owner FROM project WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      statement.setLong(1, projectId);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public String[] getProjectCoauthors(Long projectId) {
    String query = "SELECT coauthor_email FROM coauthor_project WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);) {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if (result != null) {
        int numRows = 0;
        result.last();
        numRows = result.getRow();
        result.beforeFirst();
        String[] coauthors = new String[numRows];
        int cont = 0;
        while (result.next()) {
          coauthors[cont++] = result.getString(1);
        }
        return coauthors;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Boolean projectIsPublic(Long projectId)
  {
    String query = "SELECT public FROM project WHERE project_id=?;";
    try(Connection conn = DriverManager.getConnection(url, username, password);
    PreparedStatement statement = conn.prepareStatement(query);)
    {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if(result.next()) {
        return result.getBoolean("public");
      }
    }
    catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Project getProjectById(Long projectId) {
    String query = "SELECT * FROM project WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
            result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
            result.getBoolean("public"), result.getString("owner"), getProjectCoauthors(projectId));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Project updateProject(Long projectId, Project project) {
    String query = "UPDATE project SET name=?,description=?,last_update_date=CURRENT_DATE,public=? WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setString(1, project.getName());
      statement.setString(2, project.getDescription());
      statement.setBoolean(3, project.getIsPublic());
      statement.setLong(4, projectId);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        ResultSet result = statement.getGeneratedKeys();
        if (result.next()) {
          return new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
              result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
              result.getBoolean("public"), result.getString("owner"), getProjectCoauthors(projectId));
        }
      }

    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public int addCommitProject(Long projectId, String commitId) {
    String query = "UPDATE project SET last_commit_id = ?, last_update_date = CURRENT_DATE WHERE project_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1,commitId);
      statement.setLong(2, projectId);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        return 1;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public Boolean userIsCoauthor(Long projectId, String coauthor) {
    String query = "SELECT * FROM coauthor_project WHERE project_id = ? AND coauthor_email = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, coauthor);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return true;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  public int addCoauthor(Long projectId, String coauthor) {
    String query = "INSERT INTO coauthor_project (coauthor_email,project_id) VALUES (?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, coauthor);
      statement.setLong(2, projectId);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        return 1;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return -1;
  }

  public int removeCoauthor(Long projectId, String coauthor) {
    String query = "DELETE FROM coauthor_project WHERE project_id=? AND coauthor_email=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, coauthor);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        return 1;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public Boolean fileExists(Long projectId, String folderName, String fileName) {
    String query = "SELECT * FROM file WHERE project_id=? AND directory_name=? AND file_name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, folderName);
      statement.setString(3, fileName);
      ResultSet resultSet = statement.executeQuery();
      if (resultSet.next()) {
        return true;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public int insertFile(Long projectId, String folderName, String fileName, Boolean isPublic, String description,
      String author) {
    String query = "INSERT INTO file (file_name,directory_name,project_id,uploaded_date,last_updated_date,is_public,description,author) VALUES(?,?,?,CURRENT_DATE,CURRENT_DATE,?,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, fileName);
      statement.setString(2, folderName);
      statement.setLong(3, projectId);
      statement.setBoolean(4, isPublic);
      statement.setString(5, description);
      statement.setString(6, author);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        return 1;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public FileData getFile(Long projectId, String folderName, String fileName) {
    String query = "SELECT * FROM file WHERE project_id=? AND file_name=? AND directory_name=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, fileName);
      statement.setString(3, folderName);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return new FileData(rs.getString("file_name"), rs.getString("directory_name"), rs.getLong("project_id"),
            rs.getDate("uploaded_date"), rs.getDate("last_updated_date"), rs.getBoolean("is_public"),
            rs.getString("description"), rs.getString("author"));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Boolean fileIsPublic(String fileName, String directoryName, Long projectId)
  {
    String query = "SELECT is_public FROM file WHERE project_id=? AND file_name=? AND directory_name=?;";
    try(Connection conn = DriverManager.getConnection(url, username, password);
    PreparedStatement statement = conn.prepareStatement(query)){
      statement.setLong(1, projectId);
      statement.setString(2,fileName);
      statement.setString(3,directoryName);
      ResultSet rs =statement.executeQuery();
      if(rs.next()) {
        return rs.getBoolean("is_public");
      }
    }catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public FileData updateFile(Long projectId, String folderName, String fileName, FileData file)
  {
    String query = "UPDATE file SET last_updated_date = CURRENT_DATE, is_public=?, description=? WHERE project_id=? AND file_name=? AND directory_name=?;";
    try(Connection conn = DriverManager.getConnection(url, username, password);
    PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);)
    {
      statement.setBoolean(1,file.getIsPublic());
      statement.setString(2,file.getDescription());
      statement.setLong(3,projectId);
      statement.setString(4,fileName);
      statement.setString(5,folderName);
      int numRows = statement.executeUpdate();
      if(numRows > 0) {
        ResultSet rs = statement.getGeneratedKeys();
        if(rs.next()) {
          return new FileData(rs.getString("file_name"),rs.getString("directory_name"),rs.getLong("project_id"),rs.getDate("uploaded_date"),rs.getDate("last_updated_date"),rs.getBoolean("is_public"),rs.getString("description"),rs.getString("author"));
        }
      }
    }catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public int updateFileName(Long projectId, String directoryName, String oldFileName, String newFileName)
  {
    String query = "UPDATE file SET file_name = ? WHERE project_id=? AND directory_name=? AND file_name = ?";
    try(Connection conn = DriverManager.getConnection(url, username, password);
    PreparedStatement statement = conn.prepareStatement(query))
    {
      statement.setString(1,newFileName);
      statement.setLong(2,projectId);
      statement.setString(3,directoryName);
      statement.setString(4,oldFileName);
      int numRows = statement.executeUpdate();
      if(numRows > 0)
      {
        return 1;
      }
    }catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }
}

