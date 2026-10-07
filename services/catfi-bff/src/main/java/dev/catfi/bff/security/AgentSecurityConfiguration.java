package dev.catfi.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
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
    users.setUsersByUsernameQuery(
        "select username, password_hash, enabled from agent_accounts where username = ?");
    users.setAuthoritiesByUsernameQuery(
        "select username, role from agent_accounts where username = ?");
    return users;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AuditService audit) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/api/ui/**").hasRole("AGENT")
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginProcessingUrl("/api/auth/login")
            .successHandler((request, response, authentication) -> {
              request.getSession().setAttribute("catfi.agent", authentication.getName());
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
                request.getSession(false).setAttribute("catfi.explicitLogout", Boolean.TRUE);
              }
            })
            .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
        .sessionManagement(session -> session.maximumSessions(1));
    return http.build();
  }
}
