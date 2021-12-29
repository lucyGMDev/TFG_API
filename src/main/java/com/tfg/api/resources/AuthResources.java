package com.tfg.api.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.AuthController;
import com.tfg.api.data.bodies.UserBody;

@Path("/create_user_sesion")
public class AuthResources {
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response CreateUserSesion(UserBody user)
  {
    return AuthController.CreateUserSesion(user);
  }
}
