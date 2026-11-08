package si.contactgroups.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.contactgroups.security.ErrorWriter;
import si.contactgroups.security.FirebaseTokenFilter;
import si.contactgroups.security.TokenVerifier;

/** Stateless bearer-token security: only the health check is public (SR-02, ADR-04, ADR-11). */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenVerifier verifier)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable) // bearer token in a header, no cookie session
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/health", "/health/**").permitAll().anyRequest().authenticated())
        .addFilterBefore(
            new FirebaseTokenFilter(verifier), UsernamePasswordAuthenticationFilter.class)
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
