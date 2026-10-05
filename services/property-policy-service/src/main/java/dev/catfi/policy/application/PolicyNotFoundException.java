package dev.catfi.policy.application;

public class PolicyNotFoundException extends RuntimeException {
  public PolicyNotFoundException(String message) {
    super(message);
  }
}
