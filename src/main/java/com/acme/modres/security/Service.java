package com.acme.modres.security;

/**
 * Service class updated for Java 17 compatibility.
 * SecurityManager (deprecated for removal in Java 17 via JEP 411) has been removed.
 */
public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager was deprecated for removal in Java 17 (JEP 411)
    // Removed System.getSecurityManager() usage - no longer supported in Java 17+
    System.out.println("Operation is executed");
  }
}
