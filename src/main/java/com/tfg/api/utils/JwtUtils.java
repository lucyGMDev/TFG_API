package com.tfg.api.utils;

import java.util.Date;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

public class JwtUtils {
  private Dotenv dotenv;
  private String jwtSecret = "";
  private int jwtTimeExpiration;

  public JwtUtils() {
    dotenv = Dotenv.load();
    jwtSecret = dotenv.get("JWT_SECRET");
    jwtTimeExpiration = Integer.parseInt(dotenv.get("JWT_TIME_EXP"));
  }

  public String generateJwtToken(String email) {
    return Jwts.builder().setSubject(email).setIssuedAt(new Date())
        .setExpiration(new Date(new Date().getTime() + jwtTimeExpiration)).signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
  }

  public String getUserEmailFromJwt(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
  }

  public String getIssuerFromJwtToken(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getIssuer();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
      return true;
    } catch (SignatureException e) {
      System.err.println("Invalid JWT token: " + e.getMessage());
    } catch (MalformedJwtException e) {
      System.err.println("Invalid JWT token: " + e.getMessage());
    } catch (ExpiredJwtException e) {
      System.err.println("JWT token is expired: " + e.getMessage());
    } catch (UnsupportedJwtException e) {
      System.err.println("JWT token is unsupported: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      System.err.println("JWT claims string is empty: " + e.getMessage());
    }

    return false;
  }

}
