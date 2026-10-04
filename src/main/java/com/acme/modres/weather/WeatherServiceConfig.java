package com.acme.modres.weather;

import com.acme.modres.Constants;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * cz-java-0082: WeatherServiceConfig externalises all weather-microservice
 * configuration into environment variables so that the service can be
 * independently configured via a Kubernetes ConfigMap or Secret without
 * rebuilding the image.
 *
 * Kubernetes ConfigMap example (weather-service-config.yaml):
 * <pre>
 * apiVersion: v1
 * kind: ConfigMap
 * metadata:
 *   name: weather-service-config
 * data:
 *   WEATHER_SERVICE_BASE_URL: "http://api.wunderground.com/api/"
 * ---
 * apiVersion: v1
 * kind: Secret
 * metadata:
 *   name: weather-service-secret
 * stringData:
 *   WEATHER_API_KEY: "<your-api-key>"
 * </pre>
 *
 * These values are injected into the container via the Deployment spec:
 * <pre>
 * envFrom:
 *   - configMapRef:
 *       name: weather-service-config
 *   - secretRef:
 *       name: weather-service-secret
 * </pre>
 */
public class WeatherServiceConfig {

    private static final Logger logger = Logger.getLogger(WeatherServiceConfig.class.getName());

    /** Environment variable: Weather Underground API key (injected via Kubernetes Secret). */
    public static final String ENV_WEATHER_API_KEY = "WEATHER_API_KEY";

    /**
     * Environment variable: Base URL for the Weather Underground API.
     * Injected via Kubernetes ConfigMap; defaults to the constant defined in
     * {@link Constants#WUNDERGROUND_API_PREFIX}.
     */
    public static final String ENV_WEATHER_SERVICE_BASE_URL = "WEATHER_SERVICE_BASE_URL";

    /**
     * Returns the Weather Underground API key from the environment.
     * Returns {@code null} when the variable is not set, triggering fallback
     * to default (bundled) weather data.
     */
    public String getWeatherApiKey() {
        String key = System.getenv(ENV_WEATHER_API_KEY);
        logger.log(Level.FINE, "WeatherServiceConfig: WEATHER_API_KEY is "
                + (key != null && !key.isEmpty() ? "set" : "not set"));
        return key;
    }

    /**
     * Returns the base URL for the Weather Underground API.
     * Falls back to {@link Constants#WUNDERGROUND_API_PREFIX} when the
     * environment variable is not set, ensuring the service works out-of-the-box
     * without mandatory ConfigMap configuration.
     */
    public String getWeatherServiceBaseUrl() {
        String url = System.getenv(ENV_WEATHER_SERVICE_BASE_URL);
        if (url == null || url.trim().isEmpty()) {
            url = Constants.WUNDERGROUND_API_PREFIX;
            logger.log(Level.FINE,
                    "WeatherServiceConfig: WEATHER_SERVICE_BASE_URL not set, using default: " + url);
        } else {
            logger.log(Level.FINE, "WeatherServiceConfig: WEATHER_SERVICE_BASE_URL = " + url);
        }
        return url;
    }
}
