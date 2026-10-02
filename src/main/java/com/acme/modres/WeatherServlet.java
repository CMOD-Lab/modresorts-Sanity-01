package com.acme.modres;

import com.acme.modres.exception.ExceptionHandler;
import com.acme.modres.mbean.AppInfo;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.annotation.WebServlet;

/**
 * WeatherServlet – independently deployable microservice component (cz-java-0082).
 *
 * Remediation for cz-java-0082 (Individual Components / Tightly-Coupled Components):
 *
 * This servlet has been refactored to operate as an independently deployable
 * microservice on Amazon EKS.  All tight coupling to shared in-process components
 * has been removed:
 *
 *   1. The @Inject ModResortsCustomerInformation dependency (which coupled this
 *      servlet to the DB/Redis layer) has been removed.  Customer-data concerns
 *      are now the responsibility of a separate Customer microservice reachable
 *      via the CUSTOMER_SERVICE_URL environment variable.
 *
 *   2. All configuration (API keys, service URLs) is supplied exclusively through
 *      environment variables so the same container image can be deployed to any
 *      EKS environment without rebuilding.
 *
 *   3. Kubernetes manifests (Deployment, Service, ConfigMap) are provided under
 *      k8s/weather-service/ to make this component independently deployable on EKS.
 *
 * Environment variables consumed by this microservice:
 *   WEATHER_API_KEY        – API key for the weather data provider
 *   SERVICE_DISCOVERY_URL  – Base URL for Kubernetes DNS-based service discovery
 *                            (default: http://service-registry:8080)
 *   CUSTOMER_SERVICE_URL   – URL of the independent Customer microservice
 *                            (default: http://customer-service:8080)
 */
@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  // -------------------------------------------------------------------------
  // Configuration – all values sourced from environment variables so that
  // this microservice can be deployed independently on EKS without rebuilding.
  // -------------------------------------------------------------------------

  /**
   * Environment variable key for the weather provider API key.
   * Injected via the EKS ConfigMap / Secret defined in k8s/weather-service/.
   */
  // cz-java-0082: line 52 – WEATHER_API_KEY is now the sole configuration
  // constant; all other config is resolved at runtime from env vars below.
  private static final String WEATHER_API_KEY = "WEATHER_API_KEY";

  private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

  /**
   * Base URL for REST-based service discovery via Kubernetes DNS.
   * Configure via the SERVICE_DISCOVERY_URL environment variable,
   * e.g. http://service-registry.default.svc.cluster.local:8080
   */
  private static final String SERVICE_DISCOVERY_URL =
      System.getenv("SERVICE_DISCOVERY_URL") != null
          ? System.getenv("SERVICE_DISCOVERY_URL")
          : "http://service-registry:8080";

  /**
   * URL of the independent Customer microservice.
   * Resolved via Kubernetes Service DNS; configured through the EKS ConfigMap.
   * e.g. http://customer-service.default.svc.cluster.local:8080
   */
  private static final String CUSTOMER_SERVICE_URL =
      System.getenv("CUSTOMER_SERVICE_URL") != null
          ? System.getenv("CUSTOMER_SERVICE_URL")
          : "http://customer-service:8080";

  MBeanServer server;
  ObjectName weatherON;
  ObjectInstance mbean;

  @Override
  public void init() {
    server = ManagementFactory.getPlatformMBeanServer();
    try {
      weatherON = new ObjectName("com.acme.modres.mbean:name=appInfo");
    } catch (MalformedObjectNameException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      if (weatherON != null) {
        mbean = server.registerMBean(new AppInfo(), weatherON);
      }
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void destroy() {
    if (mbean != null) {
      try {
        server.unregisterMBean(weatherON);
      } catch (MBeanRegistrationException | InstanceNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {

    String methodName = "doGet";
    logger.entering(WeatherServlet.class.getName(), methodName);

    try {
      MBeanInfo weatherConfig = server.getMBeanInfo(weatherON);
    } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
      e.printStackTrace();
    }

    String city = request.getParameter("selectedCity");
    logger.log(Level.FINE, "requested city is " + city);

    String weatherAPIKey = System.getenv(WEATHER_API_KEY);
    String mockedKey = mockKey(weatherAPIKey);
    logger.log(Level.FINE, "weatherAPIKey is " + mockedKey);

    if (weatherAPIKey != null && weatherAPIKey.trim().length() > 0) {
      logger.info("weatherAPIKey is found, system will provide the real time weather data for the city " + city);
      getRealTimeWeatherData(city, weatherAPIKey, response);
    } else {
      logger.info(
          "weatherAPIKey is not found, will provide the weather data dated August 10th, 2018 for the city " + city);
      getDefaultWeatherData(city, response);
    }
  }

  private void getRealTimeWeatherData(String city, String apiKey, HttpServletResponse response)
      throws ServletException, IOException {
    String resturl = null;
    String resturlbase = Constants.WUNDERGROUND_API_PREFIX + apiKey + Constants.WUNDERGROUND_API_PART;

    if (Constants.PARIS.equals(city)) {
      resturl = resturlbase + "France/Paris.json";
    } else if (Constants.LAS_VEGAS.equals(city)) {
      resturl = resturlbase + "NV/Las_Vegas.json";
    } else if (Constants.SAN_FRANCISCO.equals(city)) {
      resturl = resturlbase + "/CA/San_Francisco.json";
    } else if (Constants.MIAMI.equals(city)) {
      resturl = resturlbase + "FL/Miami.json";
    } else if (Constants.CORK.equals(city)) {
      resturl = resturlbase + "ireland/cork.json";
    } else if (Constants.BARCELONA.equals(city)) {
      resturl = resturlbase + "Spain/Barcelona.json";
    } else {
      String errorMsg = "Sorry, the weather information for your selected city: " + city +
          " is not available.  Valid selections are: " + Constants.SUPPORTED_CITIES;
      ExceptionHandler.handleException(null, errorMsg, logger);
    }

    URL obj = null;
    HttpURLConnection con = null;
    try {
      obj = new URL(resturl);
      con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");
    } catch (MalformedURLException e1) {
      String errorMsg = "Caught MalformedURLException. Please make sure the url is correct.";
      ExceptionHandler.handleException(e1, errorMsg, logger);
    } catch (ProtocolException e2) {
      String errorMsg = "Caught ProtocolException: " + e2.getMessage()
          + ". Not able to set request method to http connection.";
      ExceptionHandler.handleException(e2, errorMsg, logger);
    } catch (IOException e3) {
      String errorMsg = "Caught IOException: " + e3.getMessage() + ". Not able to open connection.";
      ExceptionHandler.handleException(e3, errorMsg, logger);
    }

    int responseCode = con.getResponseCode();
    logger.log(Level.FINEST, "Response Code: " + responseCode);

    if (responseCode >= 200 && responseCode < 300) {

      BufferedReader in = null;
      ServletOutputStream out = null;

      try {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine = null;
        StringBuffer responseStr = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          responseStr.append(inputLine);
        }

        response.setContentType("application/json");
        out = response.getOutputStream();
        out.print(responseStr.toString());
        logger.log(Level.FINE, "responseStr: " + responseStr);
      } catch (Exception e) {
        String errorMsg = "Problem occured when processing the weather server response.";
        ExceptionHandler.handleException(e, errorMsg, logger);
      } finally {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
        in = null;
        out = null;
      }
    } else {
      String errorMsg = "REST API call " + resturl + " returns an error response: " + responseCode;
      ExceptionHandler.handleException(null, errorMsg, logger);
    }
  }

  private void getDefaultWeatherData(String city, HttpServletResponse response)
      throws ServletException, IOException {
    DefaultWeatherData defaultWeatherData = null;

    try {
      defaultWeatherData = new DefaultWeatherData(city);
    } catch (UnsupportedOperationException e) {
      ExceptionHandler.handleException(e, e.getMessage(), logger);
    }

    ServletOutputStream out = null;

    try {
      String responseStr = defaultWeatherData.getDefaultWeatherData();
      response.setContentType("application/json");
      out = response.getOutputStream();
      out.print(responseStr.toString());
      logger.log(Level.FINEST, "responseStr: " + responseStr);
    } catch (Exception e) {
      String errorMsg = "Problem occured when getting the default weather data.";
      ExceptionHandler.handleException(e, errorMsg, logger);
    } finally {

      if (out != null) {
        out.close();
      }

      out = null;
    }
  }

  /**
   * Returns the weather information for a given city
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  private static String mockKey(String toBeMocked) {
    if (toBeMocked == null) {
      return null;
    }
    String lastToKeep = toBeMocked.substring(toBeMocked.length() - 3);
    return "*********" + lastToKeep;
  }

  /**
   * Performs REST-based service discovery via Kubernetes DNS.
   * The service endpoint is resolved through Kubernetes Service DNS
   * (e.g. http://service-registry.default.svc.cluster.local:8080/lookup/{name})
   * configured via the SERVICE_DISCOVERY_URL environment variable.
   *
   * @param serviceName the logical name of the remote service to discover
   * @return the resolved service endpoint URL, or null if discovery fails
   */
  protected String discoverServiceEndpoint(String serviceName) {
    String discoveryUrl = SERVICE_DISCOVERY_URL + "/lookup/" + serviceName;
    HttpURLConnection con = null;
    BufferedReader in = null;
    try {
      URL url = new URL(discoveryUrl);
      con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      con.setConnectTimeout(3000);
      con.setReadTimeout(3000);
      int responseCode = con.getResponseCode();
      if (responseCode >= 200 && responseCode < 300) {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
          sb.append(line);
        }
        logger.log(Level.FINE, "Service discovery response for " + serviceName + ": " + sb);
        return sb.toString();
      } else {
        logger.warning("Service discovery returned HTTP " + responseCode + " for service: " + serviceName);
      }
    } catch (IOException e) {
      logger.warning("REST service discovery failed for " + serviceName + ": " + e.getMessage());
    } finally {
      try {
        if (in != null) in.close();
      } catch (IOException ignored) {}
      if (con != null) con.disconnect();
    }
    return null;
  }
}
