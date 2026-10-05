package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager has been deprecated for removal since Java 17.
    // System.getSecurityManager() should no longer be used.
    // If security checks are needed, use alternative security mechanisms.
    System.out.println("Operation is executed");
  }
}
