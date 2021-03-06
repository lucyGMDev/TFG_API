package com.tfg.api.controllers;

import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.tfg.api.data.OAuthLoginAuthenticationJSON;
import com.tfg.api.data.User;
import com.tfg.api.utils.DBManager;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AuthenticationController {
  /**
   * Create the sesion of a user
   * 
   * @param OAuthtoken Token with Oauth login data
   * @return
   */
  public static Response createUserSesion(final String OAuthtoken) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();

    StringBuilder oauthAuthenticationResponse = new StringBuilder();
    URL authenticationLoginURL = null;
    String authenticationLoginURLString = environmentVariablesManager.get("OAUTH2_LOGIN_VALIDATION_URL");

    try {
      authenticationLoginURL = new URL(authenticationLoginURLString);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }

    HttpURLConnection authenticationHttpConnection = null;
    try {
      authenticationHttpConnection = (HttpURLConnection) authenticationLoginURL.openConnection();
      authenticationHttpConnection.setRequestMethod("GET");
      String authorizationHeader = "Bearer " + OAuthtoken;
      authenticationHttpConnection.setRequestProperty("Authorization", authorizationHeader);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }
    BufferedReader rd = null;
    try {
      rd = new BufferedReader(new InputStreamReader(authenticationHttpConnection.getInputStream()));
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }
    String linea;
    try {
      while ((linea = rd.readLine()) != null) {
        oauthAuthenticationResponse.append(linea);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }
    try {
      rd.close();
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }

    OAuthLoginAuthenticationJSON oauthLoginToken = jsonManager.fromJson(oauthAuthenticationResponse.toString(),
        OAuthLoginAuthenticationJSON.class);

    String userEmail = oauthLoginToken.getEmail();

    if (!database.userExistsByEmail(userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    User user = database.getUserByEmail(userEmail);
    String token = Jwts.builder()
        .setSubject(userEmail)
        .setIssuer("/create_user_sesion")
        .setIssuedAt(new Date())
        .setExpiration(
            new Date(new Date().getTime() + Integer.parseInt(environmentVariablesManager.get("JWT_TIME_EXP"))))
        .signWith(SignatureAlgorithm.HS512, environmentVariablesManager.get("JWT_SECRET"))
        .compact();

    String response = "{\"user\":" + jsonManager.toJson(user) + ",\"token\":\"" + token + "\"" + "}";
    return Response.status(Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Sing up a user on the application
   * 
   * @param OAuthtoken Token with Oauth login data
   * @param username
   * @return
   */
  public static Response singUp(final String OAuthtoken, final String username) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();

    if (database.userExistsByUsername(username)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are another user with this username\"}").build();
    }

    StringBuilder oauthAuthenticationResponse = new StringBuilder();
    URL authenticationLoginURL = null;
    String authenticationLoginURLString = environmentVariablesManager.get("OAUTH2_LOGIN_VALIDATION_URL");

    try {
      authenticationLoginURL = new URL(authenticationLoginURLString);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }

    HttpURLConnection authenticationHttpConnection = null;
    try {
      authenticationHttpConnection = (HttpURLConnection) authenticationLoginURL.openConnection();
      authenticationHttpConnection.setRequestMethod("GET");
      String authorizationHeader = "Bearer " + OAuthtoken;
      authenticationHttpConnection.setRequestProperty("Authorization", authorizationHeader);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }
    BufferedReader rd = null;
    try {
      rd = new BufferedReader(new InputStreamReader(authenticationHttpConnection.getInputStream()));
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }
    String linea;
    try {
      while ((linea = rd.readLine()) != null) {
        oauthAuthenticationResponse.append(linea);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }
    try {
      rd.close();
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"message\":\"Error while validating login\"}").build();
    }

    OAuthLoginAuthenticationJSON oauthLoginToken = jsonManager.fromJson(oauthAuthenticationResponse.toString(),
        OAuthLoginAuthenticationJSON.class);

    String userEmail = oauthLoginToken.getEmail();

    if (database.userExistsByEmail(userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    User user = database.insertUser(userEmail, username);
    String token = Jwts.builder()
        .setSubject(userEmail)
        .setIssuer("/create_user_sesion")
        .setIssuedAt(new Date())
        .setExpiration(
            new Date(new Date().getTime() + Integer.parseInt(environmentVariablesManager.get("JWT_TIME_EXP"))))
        .signWith(SignatureAlgorithm.HS512, environmentVariablesManager.get("JWT_SECRET"))
        .compact();

    String response = "{\"user\":" + jsonManager.toJson(user) + ",\"token\":\"" + token + "\"" + "}";
    return Response.status(Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
  }
}
