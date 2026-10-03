package com.acme.modres.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Externalizes customer information state into Amazon ElastiCache (Redis)
 * so that all EKS pod replicas share a single consistent data store.
 *
 * Previously this class was annotated with @Singleton/@Startup (EJB singleton),
 * which caused state inconsistencies when scaling containers horizontally.
 * The singleton-held state is now stored and retrieved from Redis via a
 * JedisPool backed by the REDIS_HOST / REDIS_PORT environment variables,
 * which should point to an Amazon ElastiCache Redis endpoint on EKS.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache Redis primary endpoint (default: localhost)
 *   REDIS_PORT  – ElastiCache Redis port              (default: 6379)
 */
public class ModResortsCustomerInformation {

    private static final Logger LOGGER =
            Logger.getLogger(ModResortsCustomerInformation.class.getName());

    /** Redis key under which the customer info list is stored. */
    private static final String CUSTOMER_INFO_KEY = "modresorts:customer:info";

    private final JedisPool jedisPool;

    public ModResortsCustomerInformation() {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST") : "localhost";
        int redisPort = 6379;
        String redisPortEnv = System.getenv("REDIS_PORT");
        if (redisPortEnv != null && !redisPortEnv.isEmpty()) {
            try {
                redisPort = Integer.parseInt(redisPortEnv);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING,
                        "Invalid REDIS_PORT value ''{0}'', using default 6379", redisPortEnv);
            }
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        this.jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
        LOGGER.log(Level.INFO,
                "ModResortsCustomerInformation initialised with Redis at {0}:{1}",
                new Object[]{redisHost, redisPort});
    }

    /**
     * Retrieves customer information from Amazon ElastiCache (Redis).
     * Each list element is stored as a separate Redis list entry under
     * {@value #CUSTOMER_INFO_KEY}.
     *
     * @return list of customer info strings; empty list if none found or on error.
     */
    public ArrayList<String> getCustomerInformation() {
        ArrayList<String> customerInfo = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> results = jedis.lrange(CUSTOMER_INFO_KEY, 0, -1);
            if (results != null) {
                customerInfo.addAll(results);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to retrieve customer information from Redis", e);
        }
        return customerInfo;
    }

    /**
     * Stores (appends) a customer info entry into Amazon ElastiCache (Redis).
     *
     * @param info the customer information string to store.
     */
    public void addCustomerInformation(String info) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(CUSTOMER_INFO_KEY, info);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to store customer information in Redis", e);
        }
    }

    /**
     * Clears all customer information from Amazon ElastiCache (Redis).
     */
    public void clearCustomerInformation() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(CUSTOMER_INFO_KEY);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to clear customer information from Redis", e);
        }
    }
}
