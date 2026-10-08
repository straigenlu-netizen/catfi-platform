package dev.catfi.bff.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AgentSecurityConfiguration {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean
  UserDetailsService userDetailsService(javax.sql.DataSource dataSource) {
    var users = new JdbcUserDetailsManager(dataSource);
    // A client identity is NEVER obtained from browser-supplied client IDs.
    users.setUsersByUsernameQuery("""
        select username, password_hash, enabled from (
          select username, password_hash, enabled from agent_accounts
          union all
          select username, password_hash, enabled from customer_accounts
        ) accounts where username = ?
        """);
    users.setAuthoritiesByUsernameQuery("""
        select username, role from (
          select username, role from agent_accounts
          union all
          select username, role from customer_accounts
        ) accounts where username = ?
        """);
    return users;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AuditService audit) throws Exception {
    http.csrf(csrf -> {})
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll()
            .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
            .requestMatchers("/api/agent/**", "/api/ui/**").hasRole("AGENT")
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(
            (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
        .formLogin(form -> form
            .loginProcessingUrl("/api/auth/login")
            .successHandler((request, response, authentication) -> {
              boolean agent = authentication.getAuthorities().stream()
                  .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));
              request.getSession().setAttribute(agent ? "catfi.agent" : "catfi.customer",
                  authentication.getName());
              audit.recordSecurity(authentication.getName(), "LOGIN_SUCCESS");
              response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            })
            .failureHandler((request, response, exception) -> {
              audit.recordSecurity(request.getParameter("username"), "LOGIN_FAILURE");
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }))
        .logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .addLogoutHandler((request, response, authentication) -> {
              if (authentication != null) {
                audit.recordSecurity(authentication.getName(), "LOGOUT");
                var session = request.getSession(false);
                if (session != null) {
                  session.setAttribute("catfi.explicitLogout", Boolean.TRUE);
                }
              }
            })
            .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
    return http.build();
  }
}
