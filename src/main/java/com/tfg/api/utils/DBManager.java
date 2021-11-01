package com.tfg.api.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.tfg.api.data.User;

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

  public Boolean UserExistsByEmail(String email)
  {
    String query = "SELECT * FROM users WHERE email = ?";
    try(Connection conn = DriverManager.getConnection(url, username, password);
    PreparedStatement statement = conn.prepareStatement(query))
    {
      statement.setString(1, email);
      ResultSet result =  statement.executeQuery();
      if(result.next())
      {
        return true;
      }
      
    }
    catch(SQLException e)
    {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    return false;
  }


  public User insertUser(String email)
  {
    String query = "INSERT INTO users (email, created_date) VALUES (?,CURRENT_DATE)";
    try(Connection conn = DriverManager.getConnection(url,username,password);
    PreparedStatement statement = conn.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);){
      statement.setString(1, email);
      int numRows = statement.executeUpdate();
      if(numRows > 0)
      {
        ResultSet result = statement.getGeneratedKeys();
        if(result.next())
        {
          User user = new User(result.getString("email"), result.getString("username"),result.getString("name"),result.getString("last_name"),result.getDate("created_date"));
          return user;
        }
      }
    }
    catch(SQLException e)
    {
      System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }


  
}
