package com.acme.modres.weather;

import com.acme.modres.Constants;
import com.acme.modres.DefaultWeatherData;
import com.acme.modres.exception.ExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

/**
 * cz-java-0082: WeatherServiceImpl is the independently deployable
 * implementation of the WeatherService microservice contract.
 *
 * This class encapsulates all weather-retrieval business logic that was
 * previously embedded directly inside WeatherServlet, making it a
 * tightly-coupled component. By extracting it here, the weather capability
 * can be:
 *   - Deployed as a standalone EKS microservice (separate Deployment + Service)
 *   - Configured independently via a Kubernetes ConfigMap
 *   - Scaled independently from the servlet/web tier
 *   - Tested in isolation without a servlet container
 *
 * Configuration is injected entirely through environment variables so that
 * the same image can run in any environment (dev / staging / prod) by
 * supplying the appropriate ConfigMap or Secret values.
 *
 * Environment variables consumed (defined in weather-service-config.yaml):
 *   WEATHER_API_KEY          - API key for the Weather Underground service
 *   WEATHER_SERVICE_BASE_URL - Override for the Weather Underground base URL
 *                              (defaults to Constants.WUNDERGROUND_API_PREFIX)
 */
public class WeatherServiceImpl implements WeatherService {

    private static final Logger logger = Logger.getLogger(WeatherServiceImpl.class.getName());

    /** Environment variable name for the Weather Underground API key. */
    private static final String WEATHER_API_KEY_ENV = "WEATHER_API_KEY";

    /** Environment variable name for an optional base-URL override. */
    private static final String WEATHER_SERVICE_BASE_URL_ENV = "WEATHER_SERVICE_BASE_URL";

    private final WeatherServiceConfig config;

    /**
     * Constructs a WeatherServiceImpl using configuration resolved from
     * environment variables via {@link WeatherServiceConfig}.
     */
    public WeatherServiceImpl() {
        this.config = new WeatherServiceConfig();
    }

    /**
     * Package-private constructor for unit testing with an injected config.
     */
    WeatherServiceImpl(WeatherServiceConfig config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * Delegates to real-time weather retrieval when an API key is available,
     * otherwise falls back to bundled default data.
     */
    @Override
    public String getWeatherData(String city) throws ServletException, IOException {
        String weatherAPIKey = config.getWeatherApiKey();
        if (weatherAPIKey != null && !weatherAPIKey.trim().isEmpty()) {
            logger.info("WEATHER_API_KEY found – fetching real-time weather data for city: " + city);
            return fetchRealTimeWeatherData(city, weatherAPIKey);
        } else {
            logger.info("WEATHER_API_KEY not set – returning default weather data for city: " + city);
            return fetchDefaultWeatherData(city);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String fetchRealTimeWeatherData(String city, String apiKey)
            throws ServletException, IOException {

        String baseUrl = config.getWeatherServiceBaseUrl();
        String restUrlBase = baseUrl + apiKey + Constants.WUNDERGROUND_API_PART;
        String restUrl = buildCityUrl(city, restUrlBase);

        URL obj = null;
        HttpURLConnection con = null;
        try {
            obj = new URL(restUrl);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
        } catch (MalformedURLException e) {
            ExceptionHandler.handleException(e,
                    "MalformedURLException – check the weather service URL: " + restUrl, logger);
        } catch (ProtocolException e) {
            ExceptionHandler.handleException(e,
                    "ProtocolException: " + e.getMessage(), logger);
        } catch (IOException e) {
            ExceptionHandler.handleException(e,
                    "IOException opening connection: " + e.getMessage(), logger);
        }

        int responseCode = con.getResponseCode();
        logger.log(Level.FINEST, "Weather API response code: " + responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                logger.log(Level.FINE, "Real-time weather response received for city: " + city);
                return sb.toString();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } else {
            ExceptionHandler.handleException(null,
                    "Weather API call returned error response " + responseCode + " for URL: " + restUrl, logger);
            return null;
        }
    }

    private String fetchDefaultWeatherData(String city) throws ServletException, IOException {
        DefaultWeatherData defaultWeatherData;
        try {
            defaultWeatherData = new DefaultWeatherData(city);
        } catch (UnsupportedOperationException e) {
            ExceptionHandler.handleException(e, e.getMessage(), logger);
            return null;
        }
        return defaultWeatherData.getDefaultWeatherData();
    }

    private String buildCityUrl(String city, String restUrlBase) throws ServletException {
        if (Constants.PARIS.equals(city)) {
            return restUrlBase + "France/Paris.json";
        } else if (Constants.LAS_VEGAS.equals(city)) {
            return restUrlBase + "NV/Las_Vegas.json";
        } else if (Constants.SAN_FRANCISCO.equals(city)) {
            return restUrlBase + "/CA/San_Francisco.json";
        } else if (Constants.MIAMI.equals(city)) {
            return restUrlBase + "FL/Miami.json";
        } else if (Constants.CORK.equals(city)) {
            return restUrlBase + "ireland/cork.json";
        } else if (Constants.BARCELONA.equals(city)) {
            return restUrlBase + "Spain/Barcelona.json";
        } else {
            String errorMsg = "Weather information not available for city: " + city
                    + ". Supported cities: " + Constants.SUPPORTED_CITIES;
            ExceptionHandler.handleException(null, errorMsg, logger);
            return null;
        }
    }
}
