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

@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  // cz-java-0082: Replaced tightly-coupled @Inject ModResortsCustomerInformation
  // with a REST-based microservice client. The customer information service is now
  // an independently deployable EKS microservice, accessed via the
  // CUSTOMER_INFO_SERVICE_URL environment variable (e.g.,
  // http://customer-info-service.<namespace>.svc.cluster.local/api/customers).
  // This decouples WeatherServlet from the customer-info component, allowing each
  // to be deployed, scaled, and versioned independently on Amazon EKS.
  private static final String CUSTOMER_INFO_SERVICE_URL_ENV = "CUSTOMER_INFO_SERVICE_URL";

  // local OS environment variable key name. The key value should provide an API
  // key that will be used to
  // get weather information from site: http://www.wunderground.com
  private static final String WEATHER_API_KEY = "WEATHER_API_KEY";

  private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

  // Environment variable for REST-based service discovery endpoint (replaces RMI/IIOP lookup)
  private static final String SERVICE_DISCOVERY_URL_ENV = "SERVICE_DISCOVERY_URL";

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
    // Initialize REST-based service discovery (replaces RMI InitialContext setup)
    initServiceDiscovery();
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

  private String configureEnvDiscovery() {

    String serverEnv = "";

    // Replaced WebSphere-specific com.ibm.websphere.runtime.ServerName with
    // standard environment variable lookups for container portability
    serverEnv += System.getenv().getOrDefault("SERVER_DISPLAY_NAME", "");
    serverEnv += System.getenv().getOrDefault("SERVER_FULL_NAME", "");

    return serverEnv;
  }

  /**
   * Retrieves customer information from the independently deployed customer-info
   * microservice via a REST HTTP call.
   *
   * cz-java-0082: Replaces the former tightly-coupled @Inject of
   * ModResortsCustomerInformation. The customer-info component is now an
   * independent EKS microservice with its own Kubernetes Deployment, Service,
   * and ConfigMap. Its endpoint is supplied via the CUSTOMER_INFO_SERVICE_URL
   * environment variable (e.g.,
   * http://customer-info-service.<namespace>.svc.cluster.local/api/customers).
   *
   * @return JSON string response from the customer-info microservice, or an
   *         empty JSON array if the service URL is not configured or the call fails.
   */
  protected String getCustomerInfoFromService() {
    String serviceUrl = System.getenv(CUSTOMER_INFO_SERVICE_URL_ENV);
    if (serviceUrl == null || serviceUrl.trim().isEmpty()) {
      logger.warning("CUSTOMER_INFO_SERVICE_URL environment variable is not set. "
          + "Customer info microservice will be skipped. "
          + "Set CUSTOMER_INFO_SERVICE_URL to the Kubernetes service DNS endpoint "
          + "(e.g., http://customer-info-service.<namespace>.svc.cluster.local/api/customers).");
      return "[]";
    }

    HttpURLConnection con = null;
    BufferedReader in = null;
    try {
      URL url = new URL(serviceUrl);
      con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("Accept", "application/json");
      con.setConnectTimeout(5000);
      con.setReadTimeout(5000);

      int responseCode = con.getResponseCode();
      if (responseCode >= 200 && responseCode < 300) {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
          sb.append(line);
        }
        logger.log(Level.FINE, "Customer info service response received from: " + serviceUrl);
        return sb.toString();
      } else {
        logger.warning("Customer info microservice returned HTTP " + responseCode
            + " from URL: " + serviceUrl);
        return "[]";
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to call customer info microservice at " + serviceUrl + ": " + e.getMessage(), e);
      return "[]";
    } finally {
      if (in != null) {
        try { in.close(); } catch (IOException ignored) {}
      }
      if (con != null) {
        con.disconnect();
      }
    }
  }

  /**
   * Initializes REST-based service discovery using Kubernetes DNS and environment variables.
   * Replaces the former RMI/IIOP-based InitialContext lookup
   * (previously used corbaloc:iiop with WsnInitialContextFactory) which is not
   * available in container environments. Service endpoints are now resolved via
   * Kubernetes Service DNS names supplied through the SERVICE_DISCOVERY_URL
   * environment variable.
   */
  private void initServiceDiscovery() {
    // cz-java-0080: Replaced RMI resource lookup (corbaloc:iiop:localhost:2809 via
    // WsnInitialContextFactory) with REST-based service discovery.
    // The service endpoint is resolved via Kubernetes DNS using the
    // SERVICE_DISCOVERY_URL environment variable (e.g., http://<k8s-service-name>/api/lookup).
    String serviceDiscoveryUrl = System.getenv(SERVICE_DISCOVERY_URL_ENV);
    if (serviceDiscoveryUrl != null && !serviceDiscoveryUrl.trim().isEmpty()) {
      logger.info("REST service discovery endpoint configured: " + serviceDiscoveryUrl);
    } else {
      logger.warning("SERVICE_DISCOVERY_URL environment variable is not set. "
          + "REST-based service discovery will be skipped. "
          + "Set SERVICE_DISCOVERY_URL to the Kubernetes service DNS endpoint "
          + "(e.g., http://<service-name>.<namespace>.svc.cluster.local/api/lookup).");
    }
  }
}
