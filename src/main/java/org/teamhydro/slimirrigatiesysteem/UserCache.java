package org.teamhydro.slimirrigatiesysteem;

import org.json.JSONObject;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;

public class UserCache {
    private static final String CACHE_FILE = "user_cache.json";
    private static final long CACHE_DURATION = 30L * 24 * 60 * 60 * 1000; // 30 days in milliseconds

    public static void saveUserCache(String token, String name, String email, String address) {
        try {
            JSONObject cache = new JSONObject();
            cache.put("token", token);
            cache.put("name", name);
            cache.put("email", email);
            cache.put("address", address);
            cache.put("timestamp", Instant.now().toEpochMilli());

            Files.write(Paths.get(CACHE_FILE), cache.toString().getBytes());
        } catch (IOException e) {
            System.out.println("Error saving user cache: " + e.getMessage());
        }
    }

    public static JSONObject loadUserCache() {
        try {
            if (!Files.exists(Paths.get(CACHE_FILE))) {
                return null;
            }

            String content = new String(Files.readAllBytes(Paths.get(CACHE_FILE)));
            JSONObject cache = new JSONObject(content);
            
            long timestamp = cache.getLong("timestamp");
            if (System.currentTimeMillis() - timestamp > CACHE_DURATION) {
                // Cache expired, clear personal data but keep token
                cache.put("name", "");
                cache.put("email", "");
                cache.put("address", "");
                return cache;
            }
            
            return cache;
        } catch (IOException e) {
            System.out.println("Error loading user cache: " + e.getMessage());
            return null;
        }
    }

    public static void clearCache() {
        try {
            Files.deleteIfExists(Paths.get(CACHE_FILE));
        } catch (IOException e) {
            System.out.println("Error clearing user cache: " + e.getMessage());
        }
    }
} 