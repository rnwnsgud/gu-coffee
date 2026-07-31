package com.coffee.gu.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesHelper {
    private static final Logger log = LoggerFactory.getLogger(AesHelper.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private final SecurityProperty property;

    public AesHelper(SecurityProperty property) {
        this.property = property;
    }

    public String encrypt(String target) {
        try {
            SecretKeySpec key = new SecretKeySpec(property.key().getBytes(StandardCharsets.UTF_8), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(property.iv().getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encrypted = cipher.doFinal(target.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AesHelper.encrypt Exception: {}", e.getMessage(), e);
            return target;
        }
    }

    public String decrypt(String target) {
        try {
            SecretKeySpec key = new SecretKeySpec(property.key().getBytes(StandardCharsets.UTF_8), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(property.iv().getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(target));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AesHelper.decrypt Exception: {}", e.getMessage(), e);
            return target;
        }
    }
}
