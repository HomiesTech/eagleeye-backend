package com.homenetics.eagleeye.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;

@Component
public class HashUtil {
    private static final Logger logger = LoggerFactory.getLogger(HashUtil.class);

    public String generateEmailHash(String email) {
        try {
            // Create a SHA-256 digest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(email.getBytes());

            // Convert the byte array into a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0,6);
        } catch (Exception e) {
            logger.error("Error in generating email hash for email {} {}",email,e.getMessage());
            return null;
        }
    }
}
