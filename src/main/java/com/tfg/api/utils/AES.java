package com.tfg.api.utils;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {
  public String decrypt(String cypherMessage, String key) {
    Cipher decryptionCypher = null;
    try {
      decryptionCypher = Cipher.getInstance("AES/CBC/PKCS5Padding");

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
    }
    SecretKeySpec aesKey = new SecretKeySpec(key.getBytes(), "AES/CBC/PKCS5Padding");
    try {
      byte[] iv = new byte[16];
      new SecureRandom().nextBytes(iv);
      IvParameterSpec ivParam = new IvParameterSpec(iv);
      decryptionCypher.init(Cipher.DECRYPT_MODE, aesKey, ivParam);
    } catch (InvalidKeyException e) {
      System.out.println("Invalid Key");
      e.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
      e.printStackTrace();
    }
    byte[] decryptedMessageBytes = null;
    try {
      decryptedMessageBytes = decryptionCypher.doFinal(cypherMessage.getBytes());
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      e.printStackTrace();
    }

    String decryptedMessage = new String(decryptedMessageBytes);

    return decryptedMessage;
  }
}
