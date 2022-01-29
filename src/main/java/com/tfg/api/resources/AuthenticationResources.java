package com.tfg.api.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.AuthenticationController;
import com.tfg.api.data.bodies.UserBody;

@Path("/create_user_sesion")
public class AuthenticationResources {
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createUserSesion(@HeaderParam("Authorization") final String authorizationHeader, UserBody user) {
    String OAuthtoken = authorizationHeader.substring("Bearer".length()).trim();
    return AuthenticationController.createUserSesion(user,OAuthtoken);
  }
}
