package si.contactgroups.config;

import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import si.contactgroups.security.AccountDirectory;
import si.contactgroups.security.EmailVerifiedFilter;
import si.contactgroups.security.ErrorWriter;
import si.contactgroups.security.FirebaseTokenFilter;
import si.contactgroups.security.HttpsFilter;
import si.contactgroups.security.RateLimitFilter;
import si.contactgroups.security.RequestSizeFilter;
import si.contactgroups.security.SecurityLogger;
import si.contactgroups.security.TokenVerifier;
import si.contactgroups.service.UserService;

/** Stateless bearer-token security: only the health check is public (SR-02, ADR-04, ADR-11). */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      TokenVerifier verifier,
      UserService users,
      AccountDirectory directory,
      AppProperties properties,
      Clock clock)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable) // bearer token in a header, no cookie session
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/health", "/health/**").permitAll().anyRequest().authenticated())
        .addFilterBefore(new SecurityLogger(), CorsFilter.class)
        .addFilterBefore(new RequestSizeFilter(properties.maxRequestSize()), CorsFilter.class)
        .addFilterBefore(new HttpsFilter(properties.requireHttps()), CorsFilter.class)
        .addFilterBefore(
            new FirebaseTokenFilter(verifier, users, directory),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(
            new RateLimitFilter(clock, properties.rateLimitPerMinute()), FirebaseTokenFilter.class)
        .addFilterAfter(new EmailVerifiedFilter(), RateLimitFilter.class)
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (request, response, ex) ->
                            ErrorWriter.write(
                                response,
                                HttpStatus.UNAUTHORIZED.value(),
                                "auth.token_missing",
                                "Authentication required"))
                    .accessDeniedHandler(
                        (request, response, ex) ->
                            ErrorWriter.write(
                                response,
                                HttpStatus.FORBIDDEN.value(),
                                "auth.role_forbidden",
                                "Not allowed")))
        .headers(
            h ->
                h.frameOptions(f -> f.deny())
                    .contentTypeOptions(Customizer.withDefaults())
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                    .cacheControl(Customizer.withDefaults())
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
    return http.build();
  }

  /**
   * The product has no local users: identities come from Firebase tokens only. Declaring this bean
   * stops Spring Boot from creating a default user with a generated password and logging it (SB-03,
   * SB-07).
   */
  @Bean
  public UserDetailsService noLocalUsers() {
    return username -> {
      throw new UsernameNotFoundException("no local users");
    };
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of(properties.corsAllowedOrigin()));
    cors.setAllowedMethods(
        List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cors.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
    cors.setMaxAge(600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
