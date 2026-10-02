package com.acme.modres.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ModResortsCustomerInformation
 *
 * Replaced EJB @Singleton state storage with Amazon ElastiCache (Redis) so that
 * all EKS pod replicas share a single consistent data store.
 *
 * Blocker fix: cz-java-0064 – Singleton State Storage
 * The previous @Singleton / @Startup EJB annotations caused in-process state that
 * is not shared across horizontally-scaled container replicas.  Customer information
 * is now cached in Redis using a JedisPool whose connection parameters are supplied
 * via environment variables:
 *
 *   REDIS_HOST  – ElastiCache primary endpoint (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 *
 * The cache key "customer:info" stores a pipe-delimited list of customer records.
 * On a cache miss the data is fetched from the relational database and written back
 * to Redis so subsequent calls are served from the shared cache.
 */
public class ModResortsCustomerInformation {

  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";
  private static final String CACHE_KEY = "customer:info";
  private static final String CACHE_DELIMITER = "|";

  // Redis connection pool – endpoint resolved from environment variables so that
  // the same container image works across all EKS environments without rebuilding.
  private static final JedisPool jedisPool;

  static {
    String redisHost = System.getenv("REDIS_HOST") != null
        ? System.getenv("REDIS_HOST") : "localhost";
    int redisPort = System.getenv("REDIS_PORT") != null
        ? Integer.parseInt(System.getenv("REDIS_PORT")) : 6379;

    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(10);
    poolConfig.setMaxIdle(5);
    poolConfig.setMinIdle(1);
    jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
  }

  // Removing DB connection for ease of demo setup
  // @Resource(lookup = "jdbc/ModResortsJndi")
  private DataSource dataSource;

  /**
   * Returns customer information, serving from the shared Redis cache when
   * available and falling back to the relational database on a cache miss.
   */
  public ArrayList<String> getCustomerInformation() {
    // 1. Try to serve from the shared Redis cache first.
    try (Jedis jedis = jedisPool.getResource()) {
      String cached = jedis.get(CACHE_KEY);
      if (cached != null && !cached.isEmpty()) {
        ArrayList<String> result = new ArrayList<>();
        for (String entry : cached.split("\\" + CACHE_DELIMITER)) {
          if (!entry.isEmpty()) {
            result.add(entry);
          }
        }
        return result;
      }
    } catch (Exception e) {
      // Redis unavailable – fall through to database query.
      e.printStackTrace();
    }

    // 2. Cache miss: query the relational database.
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    ArrayList<String> customerInfo = new ArrayList<>();

    try {
      // Get a connection from the injected data source
      conn = dataSource.getConnection();
      // Create a prepared statement
      stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
      // Execute the query
      rs = stmt.executeQuery();

      // Process the results
      while (rs.next()) {
        String info = rs.getString("INFO");
        customerInfo.add(info);
      }

      // 3. Populate the shared Redis cache so all pod replicas benefit.
      if (!customerInfo.isEmpty()) {
        try (Jedis jedis = jedisPool.getResource()) {
          jedis.set(CACHE_KEY, String.join(CACHE_DELIMITER, customerInfo));
        } catch (Exception e) {
          // Non-fatal: cache write failure should not break the request.
          e.printStackTrace();
        }
      }

    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      // Close the result set, statement, and connection
      try {
        if (rs != null)
          rs.close();
        if (stmt != null)
          stmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return customerInfo;
  }

  /**
   * Invalidates the customer information entry in the shared Redis cache.
   * Call this after any write operation that modifies customer data so that
   * all pod replicas pick up the fresh data on their next request.
   */
  public void invalidateCache() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(CACHE_KEY);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
