package com.tfg.api.controllers;

import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.tfg.api.data.GoogleLoginTokenJson;
import com.tfg.api.data.User;
import com.tfg.api.data.bodies.UserBody;
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
  public static Response CreateUserSesion(UserBody user, final String OAuthtoken){
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    User userLogged;
    if(user.getEmail() == null) 
      return Response.status(Status.BAD_REQUEST).entity("{\"message\":\"Email is required\"}").type(MediaType.APPLICATION_JSON).build();
    
    StringBuilder resultado = new StringBuilder();
    URL googleAuthURL=null;
    String validationGoogleLoginURL = environmentVariablesManager.get("OAUTH2_GOOGLE_LOGIN_VALIDATION_EMAIL");
    
    try {
      googleAuthURL = new URL(validationGoogleLoginURL);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while validating login\"}").build();
    }

    HttpURLConnection googleAuthHttpConnection=null;
    try {
      googleAuthHttpConnection = (HttpURLConnection) googleAuthURL.openConnection();
      googleAuthHttpConnection.setRequestMethod("GET");
      String authorizationHeader = "Bearer "+OAuthtoken;
      googleAuthHttpConnection.setRequestProperty("Authorization", authorizationHeader);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while validating login\"}").build();
    }
    BufferedReader rd=null;
    try {
      rd = new BufferedReader(new InputStreamReader(googleAuthHttpConnection.getInputStream()));
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while validating login\"}").build();
    }
    String linea;
    try {
      while ((linea = rd.readLine()) != null) {
        resultado.append(linea);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while validating login\"}").build();
    }
    try {
      rd.close();
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while validating login\"}").build();
    }
    
    GoogleLoginTokenJson googleLoginToken = jsonManager.fromJson(resultado.toString(), GoogleLoginTokenJson.class);
    
    if(!googleLoginToken.getEmail_verified() || googleLoginToken.getEmail().compareTo(user.getEmail())!=0)
    {
      return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Failure on authentication\"}").build();
    }

    if(database.UserExistsByEmail(user.getEmail()))
    {
      userLogged = database.getUserByEmail(user.getEmail());
      if(userLogged == null)
      {
        return Response.status(Status.BAD_REQUEST).entity("{\"message\":\"Error while login\"}").type(MediaType.APPLICATION_JSON).build();
      }
    }
    else
    {
      userLogged = database.insertUser(user.getEmail());
      if(userLogged == null)
      {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while creating user\"}").type(MediaType.APPLICATION_JSON).build();
      }
    }
    String token = Jwts.builder()
      .setSubject(user.getEmail())
      .setIssuer("/create_user_sesion")
      .setIssuedAt(new Date())
      .setExpiration(new Date(new Date().getTime() + Integer.parseInt(environmentVariablesManager.get("JWT_TIME_EXP"))))
      .signWith(SignatureAlgorithm.HS512, environmentVariablesManager.get("JWT_SECRET"))
      .compact();
    
    String response = "{\"user\":"+jsonManager.toJson(userLogged)+", \"token\":\""+token+"\"}";
    return Response.status(Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
  }
}
