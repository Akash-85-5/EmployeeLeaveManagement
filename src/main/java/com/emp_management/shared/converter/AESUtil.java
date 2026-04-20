package com.emp_management.shared.converter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {

    private static final String SECRET = "MySuperSecretKey"; // 16 chars

    public static String encrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encrypt error", e);
        }
    }

    public static String decrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            return new String(cipher.doFinal(Base64.getDecoder().decode(data)));
        } catch (Exception e) {
            throw new RuntimeException("Decrypt error", e);
        }
    }
}