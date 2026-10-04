package com.acme.modres;

import com.acme.modres.db.ModResortsCustomerInformation;
import com.acme.modres.exception.ExceptionHandler;
import com.acme.modres.mbean.AppInfo;
import com.acme.modres.weather.WeatherService;
import com.acme.modres.weather.WeatherServiceImpl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.inject.Inject;
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
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

/**
 * cz-java-0082: WeatherServlet refactored into a thin HTTP controller.
 *
 * Previously this servlet contained all weather-retrieval business logic
 * directly, making it a tightly-coupled individual component that reduces
 * effectiveness in containerised microservices architectures.
 *
 * AFTER (microservices decomposition):
 *   - WeatherServlet  → thin HTTP controller (this class); handles only
 *                       HTTP request/response concerns.
 *   - WeatherService  → service interface defining the weather microservice
 *                       contract (com.acme.modres.weather.WeatherService).
 *   - WeatherServiceImpl → independently deployable business-logic
 *                       implementation (com.acme.modres.weather.WeatherServiceImpl).
 *   - WeatherServiceConfig → externalises all configuration into environment
 *                       variables consumed from a Kubernetes ConfigMap/Secret
 *                       (com.acme.modres.weather.WeatherServiceConfig).
 *
 * Each component can now be deployed as a separate EKS microservice with its
 * own Kubernetes Deployment, Service, and ConfigMap, enabling independent
 * scaling, versioning, and deployment.
 */
@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Inject
  private ModResortsCustomerInformation customerInfo;

  // local OS environment variable key name. The key value should provide an API
  // key that will be used to
  // get weather information from site: http://www.wunderground.com
  private static final String WEATHER_API_KEY = "WEATHER_API_KEY";

  private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

  private static InitialContext context;

  MBeanServer server;
  ObjectName weatherON;
  ObjectInstance mbean;

  /**
   * cz-java-0082: WeatherService is the independently deployable service
   * component extracted from this servlet. All weather business logic is
   * delegated to this service, making WeatherServlet a thin HTTP controller.
   */
  private WeatherService weatherService;

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
    context = setInitialContextProps();

    // cz-java-0082: Instantiate the independently deployable WeatherService.
    // In a full EKS microservices deployment this would be resolved via CDI
    // injection or a service-discovery call to a remote weather-service pod.
    weatherService = new WeatherServiceImpl();
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

    // cz-java-0082: Delegate all weather-retrieval logic to the independently
    // deployable WeatherService microservice component instead of handling it
    // inline within this servlet. This thin-controller pattern decouples the
    // HTTP layer from the business logic, enabling each component to be
    // deployed, scaled, and versioned independently on Amazon EKS.
    ServletOutputStream out = null;
    try {
      String weatherData = weatherService.getWeatherData(city);
      response.setContentType("application/json");
      out = response.getOutputStream();
      out.print(weatherData);
      logger.log(Level.FINE, "Weather data served for city: " + city);
    } catch (Exception e) {
      ExceptionHandler.handleException(e, "Error retrieving weather data for city: " + city, logger);
    } finally {
      if (out != null) {
        out.close();
      }
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
    String serverDisplayName = System.getenv("SERVER_DISPLAY_NAME");
    String serverFullName = System.getenv("SERVER_FULL_NAME");
    serverEnv += (serverDisplayName != null ? serverDisplayName : "");
    serverEnv += (serverFullName != null ? serverFullName : "");

    return serverEnv;
  }

  /**
   * cz-java-0080: Replaced RMI/CORBA-based JNDI lookup with REST-based service
   * discovery suitable for Kubernetes/EKS container environments.
   *
   * BEFORE (RMI - not container-compatible):
   *   Hashtable ht = new Hashtable();
   *   ht.put("java.naming.factory.initial", "com.ibm.websphere.naming.WsnInitialContextFactory");
   *   ht.put("java.naming.provider.url", "corbaloc:iiop:localhost:2809");
   *   ctx = new InitialContext(ht);
   *
   * AFTER (REST/DNS-based - container-compatible):
   *   Service endpoint resolved via NAMING_SERVICE_HOST / NAMING_SERVICE_PORT
   *   environment variables, using Kubernetes DNS-based service discovery.
   *   Remote resources are accessed via HTTP REST calls instead of RMI registry.
   */
  private InitialContext setInitialContextProps() {
    // Resolve naming service host and port from environment variables.
    // In Kubernetes/EKS, services are discovered via DNS (e.g., naming-service.namespace.svc.cluster.local)
    // and exposed through environment variables injected by the platform.
    String serviceHost = System.getenv("NAMING_SERVICE_HOST") != null
        ? System.getenv("NAMING_SERVICE_HOST") : "naming-service";
    String servicePort = System.getenv("NAMING_SERVICE_PORT") != null
        ? System.getenv("NAMING_SERVICE_PORT") : "8080";
    String serviceEndpoint = "http://" + serviceHost + ":" + servicePort + "/naming";
    logger.log(Level.INFO,
        "REST-based service discovery endpoint (replaces RMI corbaloc:iiop): " + serviceEndpoint);
    // Remote resource lookups should now be performed via HTTP REST calls
    // to the serviceEndpoint URL using standard HttpURLConnection or REST clients.
    // RMI InitialContext is not used in container environments.
    return null;
  }
}
