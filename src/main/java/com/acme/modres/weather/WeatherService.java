package com.acme.modres.weather;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * cz-java-0082: WeatherService interface extracted from WeatherServlet to
 * decompose the tightly-coupled component into an independently deployable
 * microservice contract.
 *
 * In an EKS microservices architecture this interface represents the service
 * boundary. The implementation can be deployed as a separate Kubernetes
 * Deployment with its own Service and ConfigMap, allowing independent scaling,
 * versioning, and deployment of the weather capability.
 *
 * Kubernetes resources for this microservice:
 *   - Deployment: weather-service-deployment.yaml
 *   - Service:    weather-service-svc.yaml
 *   - ConfigMap:  weather-service-config.yaml  (WEATHER_API_KEY, WEATHER_SERVICE_HOST, etc.)
 */
public interface WeatherService {

    /**
     * Returns weather data (real-time or default) for the given city as a JSON string.
     *
     * @param city the city name (must be one of the supported cities)
     * @return JSON string containing weather information
     * @throws ServletException if a servlet-level error occurs
     * @throws IOException      if an I/O error occurs while fetching data
     */
    String getWeatherData(String city) throws ServletException, IOException;
}
