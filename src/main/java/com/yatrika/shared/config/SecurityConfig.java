package com.yatrika.shared.config;

import com.yatrika.shared.security.JwtAuthenticationFilter;
import com.yatrika.shared.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//
//                                // ========================
//                                // 🌍 CORS / PREFLIGHT
//                                // ========================
//                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//
//                                // ========================
//                                // 🌐 PUBLIC
//                                // ========================
//                                .requestMatchers(
//                                        "/",
//                                        "/error",
//                                        "/uploads/**",
//                                        "/api/v1/auth/**",
//                                        "/api/public/**",
//                                        "/api/health",
//                                        "/swagger-ui.html",
//                                        "/swagger-ui/**",
//                                        "/v3/api-docs/**",
//                                        "/api-docs/**"
//                                ).permitAll()
//
//                                // ========================
//                                // 📍 DESTINATIONS (PUBLIC READ)
//                                // ========================
//                                .requestMatchers(HttpMethod.GET, "/api/destinations/**").permitAll()
//                                .requestMatchers(HttpMethod.GET, "/api/v1/itineraries/community").permitAll()
//                                .requestMatchers(HttpMethod.GET, "/api/community/posts/trending").permitAll()
//
//                                .requestMatchers(HttpMethod.GET, "/api/v1/itineraries/my-plans").hasRole("USER")
//
//                                // ========================
//                                // 🛡️ ADMIN (ALL ADMIN APIs)
//                                // ========================
//                                .requestMatchers(HttpMethod.GET, "/api/v1/itineraries/admin-templates").permitAll()
//                                .requestMatchers(HttpMethod.GET, "/api/v1/itineraries/community").permitAll()
//                                .requestMatchers(HttpMethod.GET, "/api/v1/itineraries/search").permitAll()
//
////                        .requestMatchers(HttpMethod.PATCH, "/api/v1/itineraries/**").hasRole("USER")
//
//                                .requestMatchers(
//                                        "/api/v1/admin/itineraries/**",
//                                        "/api/admin/**",
//                                        "/api/analytics/**",
//                                        "/api/moderation/**"
//                                ).hasRole("ADMIN")
//
//                                .requestMatchers(HttpMethod.POST, "/api/destinations/**").hasRole("ADMIN")
//                                .requestMatchers(HttpMethod.PUT, "/api/destinations/**").hasRole("ADMIN")
//                                .requestMatchers(HttpMethod.DELETE, "/api/destinations/**").hasRole("ADMIN")
//
//                                .requestMatchers("/api/uploads/destination/**").hasRole("ADMIN")
//
//                                .requestMatchers(HttpMethod.PATCH, "/api/reviews/**/verify").hasRole("ADMIN")
//                                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
//
//                                // ========================
//                                // 👤 USER
//                                // ========================
//                                .requestMatchers(
//                                        "/api/v1/itineraries/*/like/**"
//                                ).hasRole("USER")
//                                .requestMatchers(
//                                        "/api/v1/itineraries/**",
//                                        "/api/likes/**",
//                                        "/api/bookmarks/**",
//                                        "/api/profile/**"
//                                ).hasRole("USER")
//
//                                .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasRole("USER")
//                                .requestMatchers(HttpMethod.PUT, "/api/reviews/**").hasRole("USER")
//                                .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasRole("USER")
//
//                                .requestMatchers(HttpMethod.POST, "/api/community/posts/**").hasRole("USER")
//                                .requestMatchers(HttpMethod.PUT, "/api/community/posts/**").hasRole("USER")
//                                .requestMatchers(HttpMethod.DELETE, "/api/community/posts/**").hasRole("USER")
//
//                                .requestMatchers("/api/uploads/post/**").hasRole("USER")
//                                .requestMatchers("/api/uploads/profile/**").hasRole("USER")
//
//                                // ========================
//                                // 🔐 AUTHENTICATED (ANY USER)
//                                // ========================
//                                .requestMatchers(
//                                        "/api/users/**",
//                                        "/api/flags/**"
//                                ).authenticated()
//
//                                .requestMatchers(HttpMethod.POST, "/api/uploads/**").authenticated()
//                                .requestMatchers(HttpMethod.DELETE, "/api/uploads/**").authenticated()
//
//                                // ========================
//                                // 🔒 FALLBACK
//                                // ========================
//                                .anyRequest().authenticated()
//                )
//
//                .authenticationProvider(authenticationProvider())
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/error",
                                "/uploads/**",
                                "/api/v1/auth/**",
                                "/api/public/**",
                                "/api/health",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()

                        // AUTH
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // PUBLIC - Anyone can view user profiles
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/destinations/**",
                                "/api/v1/itineraries/admin-templates",
                                "/api/v1/itineraries/{id}"
                        ).permitAll()

                        // USER - Profile and user operations
                        .requestMatchers(
                                "/api/v1/users/me/**",           // Profile endpoints
                                "/api/v1/users/*/follow",          // Follow/unfollow
                                "/api/v1/ml/**"
                        ).hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/{id}").authenticated()  // Self or admin
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/{id}/password").authenticated()  // Self only

                        // ITINERARIES & RECOMMENDATIONS
                        .requestMatchers("/api/v1/itineraries/**", "/api/v1/recommendations/**").hasRole("USER")

                        // ADMIN - User management
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/v1/users/stats/**"         // User stats
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")  // List all users
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/{id}/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/{id}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}