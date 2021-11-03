package com.tfg.api.controllers;

import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.tfg.api.data.User;
import com.tfg.api.data.bodies.UserBody;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.JwtUtils;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class UserController {
  public static Response login(UserBody user){
    DBManager dbManager = new DBManager();
    Gson gson = new Gson();
    Dotenv dotenv = Dotenv.load();
    User userLogged;
    if(user.getEmail() == null) 
      return Response.status(Status.BAD_REQUEST).entity("{\"message\":\"Email is required\"}").type(MediaType.APPLICATION_JSON).build();
    
    if(dbManager.UserExistsByEmail(user.getEmail()))
    {
      userLogged = dbManager.getUserByEmail(user.getEmail());
      if(userLogged == null)
      {
        return Response.status(Status.BAD_REQUEST).entity("{\"message\":\"Error while login\"}").type(MediaType.APPLICATION_JSON).build();
      }
    }
    else
    {
      userLogged = dbManager.insertUser(user.getEmail());
      if(userLogged == null)
      {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while creating user\"}").type(MediaType.APPLICATION_JSON).build();
      }
    }
    String token = Jwts.builder()
      .setSubject(user.getEmail())
      .setIssuer("login")
      .setIssuedAt(new Date())
      .setExpiration(new Date(new Date().getTime() + Integer.parseInt(dotenv.get("JWT_TIME_EXP"))))
      .signWith(SignatureAlgorithm.HS512, dotenv.get("JWT_SECRET"))
      .compact();
    String response = "{\"user\":"+gson.toJson(userLogged)+", \"token\":\""+token+"\"}";
    return Response.status(Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
  }
}
