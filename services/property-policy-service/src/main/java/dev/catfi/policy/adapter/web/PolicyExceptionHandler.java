package dev.catfi.policy.adapter.web;

import dev.catfi.policy.application.PolicyBudgetExceededException;
import dev.catfi.policy.application.PolicyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PolicyExceptionHandler {
  @ExceptionHandler(PolicyNotFoundException.class)
  public ProblemDetail notFound() {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Active policy not found");
  }

  @ExceptionHandler(PolicyBudgetExceededException.class)
  public ProblemDetail budgetConflict(PolicyBudgetExceededException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
  }
}
