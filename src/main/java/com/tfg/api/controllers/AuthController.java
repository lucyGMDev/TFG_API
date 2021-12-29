package com.tfg.api.controllers;

import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.tfg.api.data.User;
import com.tfg.api.data.bodies.UserBody;
import com.tfg.api.utils.DBManager;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class AuthController {
  public static Response CreateUserSesion(UserBody user){
    DBManager database = new DBManager();
    Gson classToJson = new Gson();
    Dotenv environmentVariablesManager = Dotenv.load();
    User userLogged;
    if(user.getEmail() == null) 
      return Response.status(Status.BAD_REQUEST).entity("{\"message\":\"Email is required\"}").type(MediaType.APPLICATION_JSON).build();
    
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
    
    String response = "{\"user\":"+classToJson.toJson(userLogged)+", \"token\":\""+token+"\"}";
    return Response.status(Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
  }
}
