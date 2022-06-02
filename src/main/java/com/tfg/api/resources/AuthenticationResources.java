package com.tfg.api.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.AuthenticationController;

import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/")
public class AuthenticationResources {

  /**
   * Create a sesion for a registered user
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @return
   */
  @Path("/create_user_sesion")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response createUserSesion(@HeaderParam("Authorization") final String authorizationHeader) {
    String OAuthtoken = authorizationHeader.substring("Bearer".length()).trim();
    return AuthenticationController.createUserSesion(OAuthtoken);
  }

  /**
   * Sign up an user on the application
   * 
   * @param authorizationHeader Data saved on the Authorization Header
   * @param username            Name of the user
   * @return
   */
  @Path("/signup")
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response signUpUser(@HeaderParam("Authorization") final String authorizationHeader,
      @FormDataParam("username") final String username) {
    String OAuthtoken = authorizationHeader.substring("Bearer".length()).trim();
    return AuthenticationController.singUp(OAuthtoken, username);
  }
}
