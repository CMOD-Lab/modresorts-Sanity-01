package com.acme.modres.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * cz-java-0082: Independently deployable Weather microservice client.
 *
 * <p>This class decouples the weather-data concern from the WeatherServlet,
 * allowing the weather functionality to be deployed as a standalone EKS
 * microservice.  All service coordinates are resolved exclusively from
 * environment variables so that no compile-time coupling to any specific
 * host, port, or API key exists.
 *
 * <p>Kubernetes Deployment / Service / ConfigMap keys consumed:
 * <ul>
 *   <li>{@code WEATHER_SERVICE_HOST}  – hostname of the weather microservice
 *       (default: {@code weather-service})</li>
 *   <li>{@code WEATHER_SERVICE_PORT}  – port of the weather microservice
 *       (default: {@code 8080})</li>
 *   <li>{@code WEATHER_SERVICE_BASE_PATH} – base path of the weather API
 *       (default: {@code /api/weather})</li>
 *   <li>{@code WEATHER_API_KEY}       – external weather-provider API key</li>
 * </ul>
 */
public class WeatherServiceClient {

    private static final Logger logger = Logger.getLogger(WeatherServiceClient.class.getName());

    /** Environment variable: hostname of the independent weather microservice. */
    private static final String ENV_WEATHER_SERVICE_HOST = "WEATHER_SERVICE_HOST";

    /** Environment variable: port of the independent weather microservice. */
    private static final String ENV_WEATHER_SERVICE_PORT = "WEATHER_SERVICE_PORT";

    /** Environment variable: base path of the independent weather microservice API. */
    private static final String ENV_WEATHER_SERVICE_BASE_PATH = "WEATHER_SERVICE_BASE_PATH";

    /** Base URL resolved once at construction time from environment variables. */
    private final String serviceBaseUrl;

    /**
     * Constructs a new client, resolving the service endpoint from environment
     * variables injected by Kubernetes (EKS) at pod start-up.
     */
    public WeatherServiceClient() {
        String host = System.getenv().getOrDefault(ENV_WEATHER_SERVICE_HOST, "weather-service");
        String port = System.getenv().getOrDefault(ENV_WEATHER_SERVICE_PORT, "8080");
        String basePath = System.getenv().getOrDefault(ENV_WEATHER_SERVICE_BASE_PATH, "/api/weather");
        this.serviceBaseUrl = "http://" + host + ":" + port + basePath;
        logger.log(Level.INFO,
                "WeatherServiceClient initialised – endpoint resolved via environment variables: "
                        + serviceBaseUrl);
    }

    /**
     * Fetches weather data for the given city from the independent weather
     * microservice endpoint.
     *
     * @param city the city name to query
     * @return JSON string with weather data, or {@code null} on error
     * @throws IOException if the HTTP call fails
     */
    public String fetchWeatherData(String city) throws IOException {
        String requestUrl = serviceBaseUrl + "?city=" + city;
        logger.log(Level.FINE, "Fetching weather data from: " + requestUrl);

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
                return responseBody.toString();
            } else {
                logger.log(Level.WARNING,
                        "Weather service returned non-2xx status " + responseCode
                                + " for city: " + city);
                return null;
            }
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) { }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Returns the resolved base URL of the weather microservice endpoint.
     * Useful for logging and diagnostics.
     *
     * @return base URL string
     */
    public String getServiceBaseUrl() {
        return serviceBaseUrl;
    }
}
