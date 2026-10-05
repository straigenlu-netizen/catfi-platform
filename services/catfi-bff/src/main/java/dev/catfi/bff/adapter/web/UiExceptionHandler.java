package dev.catfi.bff.adapter.web;

import feign.FeignException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class UiExceptionHandler {
  @ExceptionHandler(FeignException.class)
  public ProblemDetail upstream(FeignException exception) {
    return ProblemDetail.forStatusAndDetail(
        exception.status() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY,
        exception.status() == 404 ? "Resource not found" : "A backend service is unavailable");
  }
}
