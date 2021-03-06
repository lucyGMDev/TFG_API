package com.tfg.api.utils;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.tfg.api.data.Comment;
import com.tfg.api.data.ListComments;
import com.tfg.api.data.Project;
import com.tfg.api.data.ProjectList;
import com.tfg.api.data.ShortUrlResource;
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

  public Boolean userExistsByUsername(String userName) {
    String query = "SELECT * FROM users WHERE username = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userName);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return true;
      }
      return false;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public User insertUser(String email, String userName) {
    String query = "INSERT INTO users (email,username, created_date) VALUES (?,?,CURRENT_DATE)";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setString(1, email);
      statement.setString(2, username);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        ResultSet result = statement.getGeneratedKeys();
        if (result.next()) {
          User user = new User(result.getString("email"), result.getString("username"), result.getString("firstname"),
              result.getString("lastname"), result.getDate("created_date"), result.getString("picture_url"));
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

  public int updateUser(User user) {
    String query = "UPDATE users SET username = ? , firstname = ? , lastname = ?, picture_url = ? WHERE email = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, user.getUsername());
      statement.setString(2, user.getName());
      statement.setString(3, user.getLastName());
      statement.setString(4, user.getPictureUrl());
      statement.setString(5, user.getEmail());
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

  public User getUserByEmail(String email) {
    String query = "SELECT * FROM users WHERE email = ?";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, email);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        User user = new User(result.getString("email"), result.getString("username"), result.getString("firstname"),
            result.getString("lastname"), result.getDate("created_date"), result.getString("picture_url"));
        return user;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public User getUserByUsername(String userName) {
    String query = "SELECT * FROM users WHERE username = ?";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userName);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        User user = new User(result.getString("email"), result.getString("username"), result.getString("firstname"),
            result.getString("lastname"), result.getDate("created_date"), result.getString("picture_url"));
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
    String query = "INSERT INTO project (name,description,created_date,last_update_date,public,type,show_history) VALUES(?,?,CURRENT_DATE,CURRENT_DATE,?,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setString(1, projectBody.getName());
      statement.setString(2, projectBody.getDescription());
      statement.setBoolean(3, projectBody.getIsPublic());
      Array typeArray = conn.createArrayOf("text", projectBody.getType());
      statement.setArray(4, typeArray);
      statement.setBoolean(5, projectBody.getShowHistory());
      int rowsInserted = statement.executeUpdate();
      if (rowsInserted == 0) {
        return null;
      }
      ResultSet result = statement.getGeneratedKeys();
      if (result.next()) {
        typeArray = result.getArray("type");
        String[] type = (String[]) typeArray.getArray();
        Project project = new Project(result.getLong("project_id"), result.getString("name"),
            result.getString("description"), result.getDate("created_date"), result.getDate("last_update_date"),
            result.getString("last_commit_id"), result.getBoolean("public"), result.getBoolean("show_history"), null,
            type, null);
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
    String query = "INSERT INTO coauthor_project (coauthor_username, project_id) VALUES (?,?);";
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

  public String[] getProjectCoauthors(Long projectId) {
    String query = "SELECT coauthor_username FROM coauthor_project WHERE project_id=?;";
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
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE p.project_id=? GROUP BY p.project_id;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        Array typeArray = result.getArray("type");
        String[] type = typeArray == null ? null : (String[]) typeArray.getArray();
        return new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
            result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
            result.getBoolean("public"), result.getBoolean("show_history"), getProjectCoauthors(projectId), type,
            result.getFloat("score"));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ProjectList getProjectsFromUser(String userEmail) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON p.project_id = cp.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE cp.coauthor_username = ? GROUP BY p.project_id;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userEmail);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        Array typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types,
                result.getFloat("score")));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ProjectList searchProject(final String userEmail, final Long numberCommentsGet, final Long offset,
      final String keyword) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE (p.public = true OR cp.coauthor_username= ?) AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) GROUP BY p.project_id ORDER BY p.last_update_date DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userEmail);
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      statement.setString(4, '%' + keyword.toLowerCase() + '%');
      statement.setLong(5, numberCommentsGet);
      statement.setLong(6, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        Array typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types,
                result.getFloat("score")));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectByTypes(final String userEmail, final Long numberCommentsGet, final Long offset,
      final String keyword, final String[] projectTypes) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE (p.public = true OR cp.coauthor_username= ?) AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) AND (? && p.type) GROUP BY p.project_id ORDER BY p.last_update_date DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userEmail);
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      statement.setString(4, '%' + keyword.toLowerCase() + '%');
      Array typesArray = conn.createArrayOf("text", projectTypes);
      statement.setArray(5, typesArray);
      statement.setLong(6, numberCommentsGet);
      statement.setLong(7, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        Float score = result.getFloat("score");
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types, score));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectOrderByRate(final String userEmail, final Long numberCommentsGet, final Long offset,
      final String keyword) {
    String sql = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE (p.public = true OR cp.coauthor_username= ?) AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) GROUP BY p.project_id HAVING AVG(sp.score) IS NOT NULL ORDER BY score DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, userEmail);
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      statement.setString(4, '%' + keyword.toLowerCase() + '%');
      statement.setLong(5, numberCommentsGet);
      statement.setLong(6, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        Array typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types,
                result.getFloat("score")));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectByTypesOrderByRate(final String userEmail, final Long numberCommentsGet,
      final Long offset,
      final String keyword, final String[] projectTypes) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE (p.public = true OR cp.coauthor_username= ?) AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) AND (? && p.type) GROUP BY p.project_id HAVING AVG(sp.score) IS NOT NULL ORDER BY score DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userEmail);
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      statement.setString(4, '%' + keyword.toLowerCase() + '%');
      Array typesArray = conn.createArrayOf("text", projectTypes);
      statement.setArray(5, typesArray);
      statement.setLong(6, numberCommentsGet);
      statement.setLong(7, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        Float score = result.getFloat("score");
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types, score));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectPublics(final Long numberCommentsGet, final Long offset,
      final String keyword) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE p.public = true AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) GROUP BY p.project_id ORDER BY p.last_update_date DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, '%' + keyword.toLowerCase() + '%');
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      statement.setLong(4, numberCommentsGet);
      statement.setLong(5, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        Array typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types,
                result.getFloat("score")));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectPublicsByTypes(final Long numberCommentsGet, final Long offset,
      final String keyword, final String[] projectTypes) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE p.public = true AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) AND (? && p.type) GROUP BY p.project_id ORDER BY p.last_update_date DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, '%' + keyword.toLowerCase() + '%');
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      Array typesArray = conn.createArrayOf("text", projectTypes);
      statement.setArray(4, typesArray);
      statement.setLong(5, numberCommentsGet);
      statement.setLong(6, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        Float score = result.getFloat("score");
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types, score));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectPublicsOrderByRate(final Long numberCommentsGet, final Long offset,
      final String keyword) {
    String sql = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE p.public = true AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) GROUP BY p.project_id HAVING AVG(sp.score) IS NOT NULL ORDER BY score DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, '%' + keyword.toLowerCase() + '%');
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      statement.setLong(4, numberCommentsGet);
      statement.setLong(5, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        Array typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types,
                result.getFloat("score")));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectPublicsByTypesOrderByRate(final Long numberCommentsGet,
      final Long offset,
      final String keyword, final String[] projectTypes) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE p.public = true AND (LOWER(p.name) LIKE ? OR LOWER(cp.coauthor_username) LIKE ? OR LOWER(p.description) LIKE ?) AND (? && p.type) GROUP BY p.project_id HAVING AVG(sp.score) IS NOT NULL ORDER BY score DESC LIMIT ? OFFSET ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, '%' + keyword.toLowerCase() + '%');
      statement.setString(2, '%' + keyword.toLowerCase() + '%');
      statement.setString(3, '%' + keyword.toLowerCase() + '%');
      Array typesArray = conn.createArrayOf("text", projectTypes);
      statement.setArray(4, typesArray);
      statement.setLong(5, numberCommentsGet);
      statement.setLong(6, offset);
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        Float score = result.getFloat("score");
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types, score));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public ProjectList searchProjectsInUser(final String keyword, final String userToSearch) {
    String query = "SELECT p.*, AVG(sp.score) AS \"score\" FROM project p INNER JOIN coauthor_project cp ON cp.project_id = p.project_id LEFT JOIN score_project sp ON p.project_id = sp.project_id WHERE cp.coauthor_username= ? AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ?) GROUP BY p.project_id ORDER BY p.last_update_date DESC;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userToSearch);
      statement.setString(2, "%" + keyword + "%");
      statement.setString(3, "%" + keyword + "%");
      ResultSet result = statement.executeQuery();
      ProjectList projects = new ProjectList();
      while (result.next()) {
        String[] coauthors = getProjectCoauthors(result.getLong(1));
        Array typesArray = result.getArray("type");
        String[] types = typesArray == null ? null : (String[]) typesArray.getArray();
        Float score = result.getFloat("score");
        projects.getProjectList()
            .add(new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
                result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
                result.getBoolean("public"), result.getBoolean("show_history"), coauthors, types, score));
      }
      return projects;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public Project updateProject(Long projectId, Project project) {
    String query = "UPDATE project SET name=?,description=?,last_update_date=CURRENT_DATE,public=?,type=?,show_history=? WHERE project_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setString(1, project.getName());
      statement.setString(2, project.getDescription());
      statement.setBoolean(3, project.getIsPublic());
      Array typeArray = conn.createArrayOf("text", project.getType());
      statement.setArray(4, typeArray);
      statement.setBoolean(5, project.getShowHistory());
      statement.setLong(6, projectId);

      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        ResultSet result = statement.getGeneratedKeys();
        if (result.next()) {
          typeArray = result.getArray("type");
          String[] type = typeArray == null ? null : (String[]) typeArray.getArray();
          Float scoreProject = this.getScoreFromProject(result.getLong("project_id"));
          return new Project(result.getLong("project_id"), result.getString("name"), result.getString("description"),
              result.getDate("created_date"), result.getDate("last_update_date"), result.getString("last_commit_id"),
              result.getBoolean("public"), result.getBoolean("show_history"), getProjectCoauthors(projectId), type,
              scoreProject);
        }
      }

    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public int deleteProject(final Long projectId) {
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

  public Boolean userHasRateProject(Long projectId, String userEmail) {
    String query = "SELECT * FROM score_project WHERE project_id = ? AND user_email = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, userEmail);
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

  public int rateProject(final Long projectId, final String userEmail, final Float score) {
    String query = "INSERT INTO score_project (user_email, project_id,score) VALUES (?,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, userEmail);
      statement.setLong(2, projectId);
      statement.setFloat(3, score);
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

  public int updateRateProject(final Long projectId, final String userEmail, final Float score) {
    String query = "UPDATE score_project SET score = ? WHERE project_id = ? AND user_email =?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setFloat(1, score);
      statement.setLong(2, projectId);
      statement.setString(3, userEmail);
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

  public Float getScoreFromProject(Long projectId) {
    String query = "SELECT AVG(score) AS \"score\" FROM score_project WHERE project_id = ? GROUP BY project_id;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return result.getFloat("score");
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Boolean userIsCoauthor(Long projectId, String coauthor) {
    String query = "SELECT * FROM coauthor_project WHERE project_id = ? AND coauthor_username = ?;";
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
    String query = "INSERT INTO coauthor_project (coauthor_username,project_id) VALUES (?,?);";
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
    String query = "DELETE FROM coauthor_project WHERE project_id=? AND coauthor_username=?;";
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

  public boolean versionExistsOnProject(final Long projectId, final String versionName) {
    String query = "SELECT * FROM project_version WHERE project_id=? AND name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
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

  public boolean versionIsPublic(final Long projectId, final String versionName) {
    String query = "SELECT public FROM project_version WHERE project_id=? AND name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
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

  public int removeVersion(final Long projectId, final String versionName) {
    String query = "DELETE FROM project_version WHERE project_id=? AND name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
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

  public String getCommitIdFromVersion(final Long projectId, final String versionName) {
    String query = "SELECT version_commit FROM project_version WHERE project_id = ? AND name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return result.getString("version_commit");
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
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

  public Version getVersionFromName(final Long projectId, final String versionName) {
    String query = "SELECT * FROM project_version WHERE project_id = ? AND name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query);) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return new Version(rs.getLong("project_id"), rs.getString("version_commit"), rs.getString("name"),
            rs.getBoolean("public"));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public int updateVersionCommit(final Long projectId, final String versionName, final String commitId) {
    String query = "UPDATE project_version SET version_commit = ? WHERE project_id = ? AND name = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commitId);
      statement.setLong(2, projectId);
      statement.setString(3, versionName);
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

  public int addCommentToProject(Long projectId, String commentId, String commentText, String userName,
      String responseCommentId) {
    String query = "INSERT INTO comment (comment_id, username, comment_text, response_comment_id, project_id, post_date) VALUES(?,?,?,?,?,CURRENT_DATE);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commentId);
      statement.setString(2, userName);
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

  public Long getNumberResponseComment(String commentId) {
    String query = "SELECT COUNT(*) FROM comment WHERE response_comment_id=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commentId);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0L;
  }

  public Comment getCommentById(String commentId) {
    String query = "SELECT * FROM comment WHERE comment_id = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, commentId);
      ResultSet result = statement.executeQuery();
      if (result.next()) {

        return new Comment(result.getString("comment_id"), result.getString("username"),
            result.getString("response_comment_id"), result.getLong("project_id"), result.getDate("post_date"),
            result.getString("comment_text"), getNumberResponseComment(commentId));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ListComments getComments(final Long projectId, final Long offset, final Long numberCommentsGet) {
    String query = "SELECT * FROM comment WHERE project_id = ? AND response_comment_id IS NULL ORDER BY post_date DESC LIMIT ? OFFSET ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setLong(2, numberCommentsGet);
      statement.setLong(3, offset);
      ResultSet result = statement.executeQuery();
      ListComments comments = new ListComments();
      while (result.next()) {
        comments.getComments()
            .add(new Comment(result.getString("comment_id"), result.getString("username"),
                result.getString("response_comment_id"), result.getLong("project_id"), result.getDate("post_date"),
                result.getString("comment_text"), getNumberResponseComment(result.getString("comment_id"))));
      }
      return comments;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ListComments getCommentResponses(final Long projectId, final String commentId, final Long offset,
      final Long numberCommentsGet) {
    String query = "SELECT * FROM comment WHERE project_id = ? AND response_comment_id = ? ORDER BY post_date ASC LIMIT ? OFFSET ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, commentId);
      statement.setLong(3, numberCommentsGet);
      statement.setLong(4, offset);
      ResultSet result = statement.executeQuery();
      ListComments comments = new ListComments();
      while (result.next()) {
        comments.getComments()
            .add(new Comment(result.getString("comment_id"), result.getString("username"),
                result.getString("response_comment_id"), result.getLong("project_id"), result.getDate("post_date"),
                result.getString("comment_text"), 0L));
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

  public Long addChangeMessageProject(final Long projectId, final String versionName, final String changeMessage,
      final String itemName, final String fileName) {
    String query = "INSERT INTO project_changes (project_id,version_name,change_message,item_name,file_name) VALUES(?,?,?,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
      statement.setString(3, changeMessage);
      statement.setString(4, itemName);
      statement.setString(5, fileName);
      int numRows = statement.executeUpdate();
      if (numRows > 0) {
        ResultSet result = statement.getGeneratedKeys();
        if (result.next()) {
          return result.getLong("change_id");
        }
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public int removeChangeMessagesFromVersion(final Long projectId, final String versionName) {
    String query = "DELETE FROM project_changes WHERE project_id = ? AND version_name = ?;";

    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, versionName);
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return -1;
  }

  public ArrayList<HistorialMessages> getHistorialProject(final Long projectId) {
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    String currentVersion = environmentVariablesManager.get("CURRENT_VERSION_NAME");
    String query = "SELECT * FROM project_changes WHERE project_id=? AND (version_name IS NULL OR version_name= ?)  ORDER BY change_date DESC;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, currentVersion);
      ResultSet result = statement.executeQuery();
      ArrayList<HistorialMessages> historial = new ArrayList<HistorialMessages>();
      while (result.next()) {
        HistorialMessages message = jsonManager.fromJson(result.getString("change_message"), HistorialMessages.class);
        message.setProjectId(result.getLong("project_id"));
        message.setFolder(result.getString("item_name"));
        message.setFile(result.getString("file_name"));
        message.setVersionName(result.getString("version_name"));
        historial.add(message);
      }
      return historial;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ArrayList<String> getHistorialProjectFromVersion(final Long projectId, final String versionName) {
    Dotenv environmentVariablesManager = Dotenv.load();
    String currentVersion = environmentVariablesManager.get("CURRENT_VERSION_NAME");
    String query = "SELECT change_message FROM project_changes WHERE project_id=? &&  version_name= ?  ORDER BY change_date DESC;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setLong(1, projectId);
      statement.setString(2, currentVersion);
      ResultSet result = statement.executeQuery();
      ArrayList<String> historial = new ArrayList<String>();
      while (result.next()) {
        historial.add(result.getString("change_message"));
      }
      return historial;
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Boolean shortUrlExist(String shortUrl) throws Exception {
    String query = "SELECT * FROM short_url WHERE short_url = ?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, shortUrl);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception();
    }
    return false;
  }

  public String getShortUrl(final Long projectId, final String folderName, final String fileName,
      final String versionName) {
    String query = String.format(
        "SELECT short_url FROM short_url WHERE project_id = %d AND folder_name %s AND file_name %s AND version_name %s;",
        projectId,
        folderName == null ? "IS NULL" : " = '" + folderName + "'",
        fileName == null ? "IS NULL" : " = '" + fileName + "'",
        versionName.equals("") ? "IS NULL" : " = '" + versionName + "'");
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {

      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return result.getString("short_url");
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public Boolean resourcesHasUrl(final Long projectId, final String folderName, final String fileName,
      final String versionName) throws Exception {
    String query = String.format(
        "SELECT short_url FROM short_url WHERE project_id = %d AND folder_name %s AND file_name %s AND version_name %s;",
        projectId,
        folderName == null ? "IS NULL" : " = '" + folderName + "'",
        fileName == null ? "IS NULL" : " = '" + fileName + "'",
        versionName.equals("") ? "IS NULL" : " = '" + versionName + "'");
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return true;
      } else {
        return false;
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    throw new Exception("Error on database");
  }

  public int insertShortUrl(final String shortUrl, final Long projectId, final String folderName, final String fileName,
      final String versionName) {
    String query = "INSERT INTO short_url (short_url, project_id, folder_name, file_name, version_name) VALUES (?,?,?,?,?);";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, shortUrl);
      statement.setLong(2, projectId);
      statement.setString(3, folderName);
      statement.setString(4, fileName);
      statement.setString(5, versionName.equals("") ? null : versionName);
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

  public ShortUrlResource getShortUrlResource(final String shortUrl) {
    String query = "SELECT * FROM short_url WHERE short_url=?;";
    try (Connection conn = DriverManager.getConnection(url, username, password);
        PreparedStatement statement = conn.prepareStatement(query)) {
      statement.setString(1, shortUrl);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return new ShortUrlResource(rs.getLong("project_id"), rs.getString("folder_name"), rs.getString("file_name"),
            rs.getString("version_name"));
      }
    } catch (SQLException e) {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}