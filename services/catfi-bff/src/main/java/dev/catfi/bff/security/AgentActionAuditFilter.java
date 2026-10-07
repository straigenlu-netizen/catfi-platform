package dev.catfi.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
@RequiredArgsConstructor
public class AgentActionAuditFilter implements HandlerInterceptor, WebMvcConfigurer {
  private final AuditService audit;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns("/api/ui/**");
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      audit.recordAction(authentication.getName(), request.getMethod(), normalize(request.getRequestURI()));
    }
    return true;
  }

  static String normalize(String path) {
    return path.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}", "/{id}");
  }
}
