package com.acme.modres.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer information service backed by Amazon ElastiCache (Redis) on EKS.
 *
 * Singleton EJB state has been externalized to Redis so that all EKS pod
 * replicas share a single consistent data store, eliminating horizontal-scaling
 * inconsistencies caused by in-process singleton state storage (cz-java-0064).
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint (default: localhost)
 *   REDIS_PORT  – ElastiCache port             (default: 6379)
 */
@ApplicationScoped
@Startup
public class ModResortsCustomerInformation {

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

    /** Redis key under which the customer-info list is cached. */
    private static final String REDIS_CUSTOMER_KEY = "modresorts:customer:info";

    /** TTL for cached entries (seconds) – 10 minutes. */
    private static final int CACHE_TTL_SECONDS = 600;

    // Removing DB connection for ease of demo setup
    // @Resource(lookup = "jdbc/ModResortsJndi")
    private DataSource dataSource;

    private JedisPool jedisPool;

    @PostConstruct
    public void init() {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST") : "localhost";
        int redisPort = 6379;
        String redisPortEnv = System.getenv("REDIS_PORT");
        if (redisPortEnv != null && !redisPortEnv.isEmpty()) {
            try {
                redisPort = Integer.parseInt(redisPortEnv);
            } catch (NumberFormatException e) {
                // fall back to default port
            }
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
    }

    @PreDestroy
    public void destroy() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    /**
     * Returns customer information.
     *
     * <p>The result is first looked up in Redis (ElastiCache). On a cache miss the
     * data is fetched from the relational database, stored in Redis with a TTL,
     * and then returned to the caller.  All pod replicas therefore read from and
     * write to the same Redis instance, ensuring consistency across horizontal
     * scale-out scenarios.
     */
    public ArrayList<String> getCustomerInformation() {
        // 1. Try to serve from Redis cache
        List<String> cached = getFromRedis();
        if (cached != null && !cached.isEmpty()) {
            return new ArrayList<>(cached);
        }

        // 2. Cache miss – fetch from the database
        ArrayList<String> customerInfo = fetchFromDatabase();

        // 3. Persist result in Redis for subsequent requests
        if (!customerInfo.isEmpty()) {
            storeInRedis(customerInfo);
        }

        return customerInfo;
    }

    // -------------------------------------------------------------------------
    // Redis helpers
    // -------------------------------------------------------------------------

    private List<String> getFromRedis() {
        if (jedisPool == null) {
            return null;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> values = jedis.lrange(REDIS_CUSTOMER_KEY, 0, -1);
            return (values != null && !values.isEmpty()) ? values : null;
        } catch (Exception e) {
            // Redis unavailable – fall through to database
            e.printStackTrace();
            return null;
        }
    }

    private void storeInRedis(List<String> customerInfo) {
        if (jedisPool == null) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            // Atomically replace the list and set its TTL
            jedis.del(REDIS_CUSTOMER_KEY);
            for (String info : customerInfo) {
                jedis.rpush(REDIS_CUSTOMER_KEY, info);
            }
            jedis.expire(REDIS_CUSTOMER_KEY, CACHE_TTL_SECONDS);
        } catch (Exception e) {
            // Non-fatal – the caller already has the data from the DB
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Database helper
    // -------------------------------------------------------------------------

    private ArrayList<String> fetchFromDatabase() {
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
}
