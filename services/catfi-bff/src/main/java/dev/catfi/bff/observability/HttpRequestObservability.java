package dev.catfi.bff.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Bounded-cardinality BFF route classes; never tag raw property IDs or user names. */
@Component
public class HttpRequestObservability implements HandlerInterceptor, WebMvcConfigurer {
  private final MeterRegistry registry;
  public HttpRequestObservability(@org.springframework.lang.Nullable MeterRegistry registry) { this.registry = registry; }

  @Override
  public void addInterceptors(InterceptorRegistry interceptors) {
    interceptors.addInterceptor(this).addPathPatterns("/api/**");
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception exception) {
    if (registry == null) return; // MVC test slices need no metrics infrastructure.
    String route = request.getRequestURI().startsWith("/api/ui/") ? "ui" :
                   request.getRequestURI().startsWith("/api/customer/") ? "customer" :
                   request.getRequestURI().startsWith("/api/agent/") ? "agent" : "auth";
    String status = response.getStatus() >= 500 ? "5xx" :
                    response.getStatus() >= 400 ? "4xx" : "success";
    registry.counter("catfi.bff.requests", Tags.of("route", route, "outcome", status)).increment();
  }
}
