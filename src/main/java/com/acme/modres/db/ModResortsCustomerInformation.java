package com.acme.modres.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Customer information service backed by Amazon ElastiCache (Redis) on EKS.
 *
 * <p>The former EJB {@code @Singleton}/{@code @Startup} pattern stored state
 * inside a single JVM, which causes data inconsistencies when the application
 * is scaled horizontally across multiple container replicas.  All shared state
 * is now externalised into Redis so every pod reads from and writes to the same
 * data store.
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>{@code REDIS_HOST}  – ElastiCache primary endpoint (default: {@code localhost})</li>
 *   <li>{@code REDIS_PORT}  – ElastiCache port              (default: {@code 6379})</li>
 * </ul>
 */
public class ModResortsCustomerInformation {

    private static final Logger LOGGER =
            Logger.getLogger(ModResortsCustomerInformation.class.getName());

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

    /** Redis key under which the customer-info list is cached. */
    private static final String REDIS_CUSTOMER_KEY = "modresorts:customer:info";

    // -----------------------------------------------------------------------
    // Redis connection pool – configured entirely from environment variables
    // so that no host/port is hard-coded in the image.
    // -----------------------------------------------------------------------
    private static final JedisPool JEDIS_POOL;

    static {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST") : "localhost";
        int redisPort;
        try {
            redisPort = Integer.parseInt(
                    System.getenv("REDIS_PORT") != null ? System.getenv("REDIS_PORT") : "6379");
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid REDIS_PORT value; falling back to 6379");
            redisPort = 6379;
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        JEDIS_POOL = new JedisPool(poolConfig, redisHost, redisPort);
        LOGGER.info("Redis connection pool initialised – host=" + redisHost
                + " port=" + redisPort);
    }

    // -----------------------------------------------------------------------
    // Removing DB connection for ease of demo setup
    // @Resource(lookup = "jdbc/ModResortsJndi")
    // private DataSource dataSource;
    // -----------------------------------------------------------------------

    /**
     * Returns customer information.
     *
     * <p>The result list is stored in Redis so that all EKS pod replicas share
     * the same cached data.  If the Redis cache is empty the method falls back
     * to the relational database, populates the cache, and returns the result.
     *
     * @return list of customer info strings; never {@code null}
     */
    public ArrayList<String> getCustomerInformation() {
        // 1. Try to serve from Redis cache first
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            List<String> cached = jedis.lrange(REDIS_CUSTOMER_KEY, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                LOGGER.fine("Returning customer info from Redis cache ("
                        + cached.size() + " entries)");
                return new ArrayList<>(cached);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Redis read failed; falling back to database", e);
        }

        // 2. Cache miss – query the database
        ArrayList<String> customerInfo = fetchFromDatabase();

        // 3. Populate Redis cache so subsequent requests (on any pod) are fast
        if (!customerInfo.isEmpty()) {
            try (Jedis jedis = JEDIS_POOL.getResource()) {
                jedis.del(REDIS_CUSTOMER_KEY);
                for (String info : customerInfo) {
                    jedis.rpush(REDIS_CUSTOMER_KEY, info);
                }
                LOGGER.fine("Customer info cached in Redis ("
                        + customerInfo.size() + " entries)");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Redis write failed; data served from DB only", e);
            }
        }

        return customerInfo;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Queries the relational database for customer information.
     * The DataSource is intentionally left un-injected for the demo setup.
     */
    private ArrayList<String> fetchFromDatabase() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> customerInfo = new ArrayList<>();

        try {
            // DataSource injection is disabled for the demo setup.
            // When re-enabled, replace the null check below with the actual
            // injected DataSource.
            if (/* dataSource */ null == null) {
                LOGGER.warning("DataSource is not configured; "
                        + "returning empty customer list from DB.");
                return customerInfo;
            }
            // conn = dataSource.getConnection();
            stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String info = rs.getString("INFO");
                customerInfo.add(info);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database query failed", e);
        } finally {
            try {
                if (rs != null)   rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close DB resources", e);
            }
        }
        return customerInfo;
    }
}
