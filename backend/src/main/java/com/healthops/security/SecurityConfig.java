package com.healthops.security;

import com.healthops.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;


import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtService jwtService;
  private final UserService userService;

  public SecurityConfig(JwtService jwtService, UserService userService) {
    this.jwtService = jwtService;
    this.userService = userService;
  }
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    // Allow requests from nginx proxy and direct access
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000", 
        "http://frontend:80",
        "http://backend:8080"
    )); 
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
  
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow preflight requests
            .anyRequest().authenticated()
        )
        .addFilterBefore(new JwtAuthFilter(jwtService, userService), UsernamePasswordAuthenticationFilter.class);
      return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() { return NoOpPasswordEncoder.getInstance(); }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  static class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;
    JwtAuthFilter(JwtService jwtService, UserService userService) {
      this.jwtService = jwtService; this.userService = userService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
      String auth = request.getHeader("Authorization");
      if (auth != null && auth.startsWith("Bearer ")) {
        String token = auth.substring(7);
        try {
          var jws = jwtService.parse(token);
          String email = jws.getBody().getSubject();
          var userDetails = userService.loadUserByUsername(email);
          var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception ignored) {}
      }
      chain.doFilter(request, response);
    }
  }
}
