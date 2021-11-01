package com.tfg.api.controllers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.tfg.api.data.User;
import com.tfg.api.data.bodies.UserBody;
import com.tfg.api.utils.DBManager;

public class UserController {
  public static Response login(UserBody user){
    DBManager dbManager = new DBManager();
    Gson gson = new Gson();
    if(user.getEmail() == null) 
      return Response.status(Status.BAD_REQUEST).entity("{\"message\":\"Username is required\"}").type(MediaType.APPLICATION_JSON).build();
    
    if(dbManager.UserExistsByEmail(user.getEmail()))
    {
      return Response.status(Status.OK).entity("{\"message\":\"Email already exists\"}").type(MediaType.APPLICATION_JSON).build();
    }
    else
    {
      User insertedUser = dbManager.insertUser(user.getEmail());
      if(insertedUser == null)
      {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"Error while creating user\"}").type(MediaType.APPLICATION_JSON).build();
      }
      return Response.status(Status.OK).entity(gson.toJson(insertedUser)).type(MediaType.APPLICATION_JSON).build();
    }
  }
}
