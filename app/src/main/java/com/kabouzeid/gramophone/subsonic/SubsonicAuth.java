package com.kabouzeid.gramophone.subsonic;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SubsonicAuth {
    private static final String API_VERSION = "1.16.1";
    private static final String CLIENT_ID = "Phonograph";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SubsonicAuth() {
    }

    @NonNull
    public static Map<String, String> createAuthParams(@NonNull String username, @NonNull String password) {
        String salt = createSalt();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("u", username);
        params.put("t", md5(password + salt));
        params.put("s", salt);
        params.put("v", API_VERSION);
        params.put("c", CLIENT_ID);
        params.put("f", "json");
        return params;
    }

    @NonNull
    public static String createSalt() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        return builder.toString();
    }

    @NonNull
    public static String md5(@NonNull String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is unavailable", e);
        }
    }
}
