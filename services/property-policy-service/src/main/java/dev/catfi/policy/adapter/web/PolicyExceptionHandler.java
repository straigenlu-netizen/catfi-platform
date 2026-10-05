package dev.catfi.policy.adapter.web;

@org.springframework.web.bind.annotation.RestControllerAdvice
public class PolicyExceptionHandler {
  @org.springframework.web.bind.annotation.ExceptionHandler(
      dev.catfi.policy.application.PolicyNotFoundException.class)
  public org.springframework.http.ProblemDetail notFound() {
    return org.springframework.http.ProblemDetail.forStatusAndDetail(
        org.springframework.http.HttpStatus.NOT_FOUND, "Active policy not found");
  }
}
