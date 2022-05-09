package com.tfg.api.controllers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.tfg.api.data.User;
import com.tfg.api.utils.DBManager;
import com.tfg.api.utils.JwtUtils;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.github.cdimascio.dotenv.Dotenv;

public class UserController {
  /**
   * Get all data from an user from his userEmail
   * 
   * @param userEmail email of the user to get
   * @return
   */
  public static Response getUser(final String username) {
    DBManager database = new DBManager();
    Gson jsonManager = new Gson();
    if (!database.userExistsByUsername(username)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"There are not any user with this email\"}").build();
    }
    User user = database.getUserByUsername(username);
    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(user))
        .build();
  }

  /**
   * Update updatable information from the user (username, name, lastname,
   * picture)
   * 
   * @param token      JWT with the user email from the user
   * @param email      Email from the user to update
   * @param username   Username to update from user
   * @param name       Name to update from user
   * @param lastName   Lastname to update from user
   * @param picture    File with the picture to upload
   * @param fileDetail Details to the file with the picture to upload
   * @return
   */
  public static Response updateUser(final String token, final String email, final String username, final String name,
      final String lastName, final InputStream picture, final FormDataContentDisposition fileDetail,
      FormDataBodyPart fileBodyPart) {
    DBManager database = new DBManager();
    Dotenv environmentVariablesManager = Dotenv.load();
    Gson jsonManager = new Gson();
    JwtUtils jwtManager = new JwtUtils();
    String userEmail;

    try {
      userEmail = jwtManager.getUserEmailFromJwt(token);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"Error with JWT\"}").build();
    }
    if (!userEmail.equals(email)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You can not modificate information from another user\"}").build();
    }

    if (!database.userExistsByEmail(userEmail)) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You are trying update an user who does not exists\"}").build();
    }
    if (fileBodyPart != null && !(fileBodyPart.getMediaType().equals(MediaType.valueOf("image/jpeg"))
        || fileBodyPart.getMediaType().equals(MediaType.valueOf("image/png"))
        || fileBodyPart.getMediaType().equals(MediaType.valueOf("image/bmp"))
        || fileBodyPart.getMediaType().equals(MediaType.valueOf("image/webp")))) {
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
          .entity("{\"message\":\"You only can upload a picture with fomat jpeg, png,bmp or webp\"}").build();
    }

    User user = database.getUserByEmail(userEmail);
    if (username != null) {
      try {
        if (!database.userExistsByUsername(username)) {
          user.setUsername(username);
        }
      } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while updating user\"}").build();
      }
    }
    if (name != null) {
      user.setName(name);
    }
    if (lastName != null) {
      user.setLastName(lastName);
    }

    if (picture != null) {
      String pictureName = userEmail + "_" + fileDetail.getFileName().replace(" ", "_");
      String pictureUrl = String.format("user/picture/%s", pictureName);
      String picturePath = environmentVariablesManager.get("USER_PICTURES_ROOT") + File.separator + pictureName;
      File[] actualFile = new File(environmentVariablesManager.get("USER_PICTURES_ROOT")).listFiles();
      List<File> pictureFilters = Arrays.stream(actualFile)
          .filter(file -> Pattern.compile("^" + userEmail + "_.*").matcher(file.getName()).matches())
          .collect(Collectors.toList());
      if (pictureFilters.size() == 1) {
        pictureFilters.get(0).delete();
      }

      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;
      try {
        bis = new BufferedInputStream(picture);
        bos = new BufferedOutputStream(new FileOutputStream(picturePath));
        int i;
        while ((i = bis.read()) != -1) {
          bos.write(i);
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (bis != null) {
          try {
            bis.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        if (bos != null) {
          try {
            bos.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      user.setPictureUrl(pictureUrl);
    }

    if (username != null || name != null || email != null || picture != null) {
      if (database.updateUser(user) == -1) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
            .entity("{\"message\":\"Error while updating user information\"}").build();
      }
    }
    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(jsonManager.toJson(user))
        .build();
  }

  /***
   * Return a picture from an user
   * 
   * @param pictureName Name of the picture on the server
   * @return Response with the picture of the user
   */
  public static Response getUserPicture(String pictureName) {
    Dotenv environmentVariablesManager = Dotenv.load();
    String path = environmentVariablesManager.get("USER_PICTURES_ROOT") + File.separator + pictureName;
    File picture = new File(path);

    if (!picture.exists()) {
      return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).build();
    }

    return Response.status(Response.Status.OK).header("Content-Disposition", "inline").entity((Object) picture).build();
  }

  public static Response userExists(String userName) {
    DBManager database = new DBManager();
    Boolean exists = database.userExistsByUsername(userName);
    if (exists == null || exists == false) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    return Response.status(Response.Status.OK).build();
  }
}
