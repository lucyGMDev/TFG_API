package com.tfg.api.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.tfg.api.data.Comment;
import com.tfg.api.data.ListComments;
import com.tfg.api.data.Project;
import com.tfg.api.data.User;
import com.tfg.api.data.Version;
import com.tfg.api.data.VersionList;
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

  public Boolean userExistsByEmail(String email) {
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
          User user = new User(result.getString("email"), result.getString("username"), result.getString("firstname"),
              result.getString("lastname"), result.getDate("created_date"));
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
        User user = new User(result.getString("email"), result.getString("username"), result.getString("firstname"),
            result.getString("lastname"), result.getDate("created_date"));
        return user;
      }
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
    String query = "INSERT INTO coauthor_project (coauthor_email, project_id) VALUES (?,?);";
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

  public Boolean projectIsPublic(Long projectId) {
    String query = "SELECT public FROM project WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return result.getBoolean("public");
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
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

  public int removeProject(Long projectId) {
    String query = "DELETE FROM project WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      int numRows = statement.executeUpdate();
      if (numRows >= 0) {
        return numRows;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public int addCommitProject(Long projectId, String commitId) {
    String query = "UPDATE project SET last_commit_id = ?, last_update_date = CURRENT_DATE WHERE project_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commitId);
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

  public String getLastCommitProject(Long projectId) {
    String query = "SELECT last_commit_id FROM project WHERE project_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return rs.getString("last_commit_id");
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
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

  public String getShortUrlFile(final Long projectId, final String directoryName, final String fileName) {
    String query = "SELECT short_url FROM file WHERE project_id=? AND directory_name=? AND file_name=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      statement.setLong(1, projectId);
      statement.setString(2, directoryName);
      statement.setString(3, fileName);
      ResultSet rs = statement.executeQuery();
      if (rs.next())
        return rs.getString("short_url");
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public int saveShortUrlFile(final Long projectId, final String directoryName, final String fileName,
      final String shortUrl) {
    String query = "UPDATE file SET short_url=? WHERE project_id=? AND directory_name=? AND file_name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, shortUrl);
      statement.setLong(2, projectId);
      statement.setString(3, directoryName);
      statement.setString(4, fileName);
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

  public String getShortUrlProject(final Long projectId) {
    String query = "SELECT short_url FROM project WHERE project_id=?;";
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

  public int saveShortUrlProject(final Long projectId, final String shortUrl) {
    String query = "UPDATE project SET short_url=? WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, shortUrl);
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

  public int createVersion(final Long projectId, final String commitId, final String name, final Boolean isPublic) {
    String query = "INSERT INTO project_version(project_id,version_commit,name,public) VALUES (?,?,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement ps = conn.prepareStatement(query);) {
      ps.setLong(1, projectId);
      ps.setString(2, commitId);
      ps.setString(3, name);
      ps.setBoolean(4, isPublic);
      int rowsUpdated = ps.executeUpdate();
      if (rowsUpdated > 0) {
        return rowsUpdated;
      }
    } catch (

    SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public boolean versionExistsOnProject(final Long projectId, final String versionId) {
    String query = "SELECT * FROM project_version WHERE project_id=? AND version_commit = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionId);
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

  public boolean versionIsPublic(final Long projectId, final String versionId) {
    String query = "SELECT public FROM project_version WHERE project_id=? AND version_commit = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return result.getBoolean("public");
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public VersionList getVersionsProject(final Long projectId) {
    String query = "SELECT * FROM project_version WHERE project_id= ?";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {

      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      VersionList versions = new VersionList();
      while (result.next()) {
        versions.getVersionList().add(new Version(result.getLong("project_id"), result.getString("version_commit"),
            result.getString("name"), result.getBoolean("public")));
      }
      if (versions.getVersionList().size() >= 0)
        return versions;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean commentExistsInProject(String commentId, Long projectId) {
    String query = "SELECT * FROM comment WHERE comment_id = ? AND project_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commentId);
      statement.setLong(2, projectId);
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

  public int addCommentToProject(Long projectId, String commentId, String commentText, String writterEmail,
      String responseCommentId) {
    String query = "INSERT INTO comment (comment_id, writter_email, comment_text, response_comment_id, project_id, post_date) VALUES(?,?,?,?,?,CURRENT_DATE);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commentId);
      statement.setString(2, writterEmail);
      statement.setString(3, commentText);
      statement.setString(4, responseCommentId);
      statement.setLong(5, projectId);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        return numRows;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public Comment getCommentById(String commentId) {
    String query = "SELECT * FROM comment WHERE comment_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commentId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return new Comment(result.getString("comment_id"), result.getString("writter_email"),
            result.getString("response_comment_id"), result.getLong("project_id"), result.getDate("post_date"),
            result.getString("comment_text"));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ListComments getComments(final Long projectId, final Long offset, final Long numberCommentsGet) {
    String query = "SELECT * FROM comment WHERE project_id = ? ORDER BY post_date DESC LIMIT ? OFFSET ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setLong(2, numberCommentsGet);
      statement.setLong(3, offset);
      ResultSet result = statement.executeQuery();
      ListComments comments = new ListComments();
      while (result.next()) {
        comments.getComments()
            .add(new Comment(result.getString("comment_id"), result.getString("writter_email"),
                result.getString("response_comment_id"), result.getLong("project_id"), result.getDate("post_date"),
                result.getString("comment_text")));
      }
      return comments;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public int deleteComment(final Long projectId, final String commentId) {
    String query = "DELETE FROM comment WHERE project_id=? AND comment_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, commentId);
      int numRows = statement.executeUpdate();
      if(numRows > 0){
        return numRows;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return -1;
  }

}
