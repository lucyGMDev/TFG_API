package com.tfg.api.resources;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tfg.api.controllers.UserController;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/user")
public class UserResources {
  @GET
  @Path("/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUser(@PathParam("username") final String username) {
    return UserController.getUser(username);
  }

  @PUT
  @Path("/{userEmail}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateUser(@HeaderParam("Authorization") final String authorizationHeader,
      @PathParam("userEmail") final String userEmail, @FormDataParam("username") final String username,
      @FormDataParam("name") final String name, @FormDataParam("lastName") final String lastName,
      @FormDataParam("picture") InputStream uploadInputStream,
      @FormDataParam("picture") FormDataContentDisposition fileDetails,
      @FormDataParam("picture") final FormDataBodyPart fileBodyPart) {
    String token = authorizationHeader.substring("Bearer".length()).trim();
    return UserController.updateUser(token, userEmail, username, name, lastName, uploadInputStream, fileDetails,
        fileBodyPart);
  }

  @GET
  @Path("/picture/{pictureName}")
  @Produces({ "image/png", "image/jpeg", "image/webp", "image/bmp" })
  public Response getUserPicture(@PathParam("pictureName") final String pictureName) {
    return UserController.getUserPicture(pictureName);
  }

  @GET
  @Path("/{userName}/userExists")
  public Response userExists(@PathParam("userName") final String userName) {
    return UserController.userExists(userName);
  }

}
