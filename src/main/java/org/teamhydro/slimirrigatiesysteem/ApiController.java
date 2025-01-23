package org.teamhydro.slimirrigatiesysteem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.prefs.Preferences;

public class ApiController {

    // Base URL for the API
    private static final String BASE_URL = "http://gatewaygalaxyproject.test/api/";
    private static Preferences prefs = Preferences.userRoot().node(ApiController.class.getName());
    private static String notAvailable = "Not available";

    public static void storeUserData(String token, String name, String email, String address) {
        prefs.put("token", token);
        prefs.put("name", name);
        prefs.put("email", email);
        prefs.put("address", address);
        System.out.println("Stored user data: " + token + ", " + name + ", " + email + ", " + address);
    }

    public static String getStoredToken() {
        return prefs.get("token", null);
    }

    public static String getStoredName() {
        System.out.println(prefs.get("name", notAvailable));
        return prefs.get("name", notAvailable);
    }

    public static String getStoredEmail() {
        System.out.println(prefs.get("email", notAvailable));
        return prefs.get("email", notAvailable);
    }

    public static String getStoredAddress() {
        System.out.println(prefs.get("address", notAvailable));
        return prefs.get("address", notAvailable);
    }

    // Handles Login
    public static String login(String email, String password) throws Exception {
        // Prepare the JSON payload for login
        String payload = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);

        // Send the POST request and get the response
        return sendPostRequest("login", payload);
    }

    // Handles Registration (example)
    public static String register(String name, String email, String password) throws Exception {
        // Prepare the JSON payload for registration
        String payload = String.format("{\"name\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}", name, email, password);

        // Send the POST request and get the response
        return sendPostRequest("register", payload);
    }

    // Handles Logout (example)
    public static String logout(String token) throws Exception {
        // Prepare the JSON payload for logout (e.g., just send the token)
        String payload = String.format("{\"token\": \"%s\"}", token);

        // Send the POST request and get the response
        return sendPostRequest("logout", payload);
    }

    public static String updateUserInfo(String name, String address, String email) throws Exception {
        // Prepare the JSON payload for updating user info
        String payload = String.format("{\"name\": \"%s\", \"address\": \"%s\", \"email\": \"%s\"}", name, address, email);

        // Send the POST request and get the response
        return sendPostRequest("update-user-info", payload);
    }

    // Function to send POST request with JSON payload
    private static String sendPostRequest(String endpoint, String payload) throws Exception {
        // Create the HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Create the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        System.out.println("Sending " + request.method() + " request to " + request.uri());
        // println a copyable version of the request usable in Postman
        System.out.println("curl -X " + request.method() + " " + request.uri() + " -H \"Content-Type: application/json\" -d '" + payload + "'");

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Handle the response and check for success
        return getResponse(response);
    }

    // Read and return the response from the server
    private static String getResponse(HttpResponse<String> response) {
        // Check if the response is successful (HTTP status 200)
        int statusCode = response.statusCode();
        if (statusCode != 200) {
            return "Error: " + statusCode + " - " + response.body();
        }

        // Return the response body
        return response.body();
    }
}