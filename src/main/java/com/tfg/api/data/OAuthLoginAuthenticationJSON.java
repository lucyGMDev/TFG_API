package com.tfg.api.data;

public class OAuthLoginAuthenticationJSON {
  String email;
  Boolean email_verified;

  public OAuthLoginAuthenticationJSON() {
  }

  public OAuthLoginAuthenticationJSON(String email, Boolean email_verified) {
    this.email = email;
    this.email_verified = email_verified;
  }
  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  public Boolean getEmail_verified() {
    return email_verified;
  }
  public void setEmail_verified(Boolean email_verified) {
    this.email_verified = email_verified;
  }
  
}
