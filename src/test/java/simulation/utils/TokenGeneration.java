package simulation.utils;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import simulation.configuration.Config;

import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class TokenGeneration {

    private String bearerToken;
    public static long tokenExpiryTime;
    private final Config config;
    private final String fullUrl;

    public static final Map<String, String> SESSION_HEADERS_AUTH = Map.of(
            "accept", "*/*",
            "Content-Type", "application/json"
    );

    public final Map<String, String> sessionHeaders() {
        return Map.of(
                "accept", "application/json",
                "Content-Type", "application/json",
                "Authorization", "Bearer #{access_token}"
        );
    }

    public TokenGeneration(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        this.config = config;
        this.fullUrl = config.getProperty("api.jwtToken.baseurl") + "/jwtauth/v1/token";
    }

    public static boolean isTokenExpired() {
        return System.currentTimeMillis() >= tokenExpiryTime;
    }

    private HttpRequestActionBuilder createTokenRequest() {
        return http("Post Auth token")
                .post(fullUrl)
                .headers(SESSION_HEADERS_AUTH)
                .body(ElFileBody("testData/URMPayload/UrmAuthPayload.json")).asJson()
                .check(status().is(200))
                .check(jsonPath("$.token").saveAs("access_token"))
                .check(responseTimeInMillis().lt(10000)) // Add a response time check for better performance insight
                .check(bodyString().notNull()); // Ensure response body is not empty
    }

    public ChainBuilder fetchToken() {
        return exec(createTokenRequest())
                .exec(session -> {
                    String accessToken = session.getString("access_token");
                    if (accessToken == null || accessToken.isEmpty()) {
                        throw new RuntimeException("Failed to retrieve access token from response");
                    }
                    long expiryDuration;
                    try {
                        expiryDuration = Long.parseLong(config.getProperty("adminJwtTokenExpiryDurationInMin")) * 60000; // Convert minutes to milliseconds
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid token expiry duration in configuration", e);
                    }
                    tokenExpiryTime = System.currentTimeMillis() + expiryDuration;
                    return session.set("access_token", accessToken); // Ensure token is set correctly in session
                }).exitHereIfFailed();
    }
}
