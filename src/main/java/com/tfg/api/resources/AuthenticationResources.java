package com.tfg.api.resources;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.AuthenticationController;

@Path("/create_user_sesion")
public class AuthenticationResources {
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response createUserSesion(@HeaderParam("Authorization") final String authorizationHeader) {
    String OAuthtoken = authorizationHeader.substring("Bearer".length()).trim();
    return AuthenticationController.createUserSesion(OAuthtoken);
  }
}
