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
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/").permitAll()      // Allow the root path
                        .requestMatchers("/error").permitAll() // Allow error pages so you see the real error

                        // Allow access to uploaded files
                        .requestMatchers("/uploads/**").permitAll()

                        // 🔓 PUBLIC/GUEST ACCESS (Like TikTok)
                        .requestMatchers("/api/auth/**").permitAll()           // Auth endpoints
                        .requestMatchers("/api/public/**").permitAll()         // Public content
                        .requestMatchers("/api/health").permitAll()           // Health check
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 📍 DESTINATIONS: READ for all, WRITE for ADMIN only
                        .requestMatchers(HttpMethod.GET, "/api/destinations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/destinations/with-images").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/destinations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/destinations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/destinations/**").hasRole("ADMIN")

                        // 👤 USER-ONLY FEATURES
                        .requestMatchers("/api/itineraries/**").hasRole("USER")       // Trip planning
                        .requestMatchers("/api/reviews/**").hasRole("USER")           // Reviews
                        .requestMatchers("/api/likes/**").hasRole("USER")             // Likes
                        .requestMatchers("/api/bookmarks/**").hasRole("USER")         // Bookmarks
                        .requestMatchers("/api/profile/**").hasRole("USER")           // User profile

                        // User review for destination
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/reviews/**/verify").hasRole("ADMIN")

                        // Community posts endpoints
                        .requestMatchers(HttpMethod.GET, "/api/community/posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/community/posts/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/community/posts/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/community/posts/**").hasRole("USER")


                        // File upload endpoints
                        // 1. Specific Admin Rule
                        .requestMatchers("/api/uploads/destination/**").hasRole("ADMIN")

                        // 2. Specific User Rules
                        .requestMatchers("/api/uploads/post/**").hasRole("USER")
                        .requestMatchers("/api/uploads/profile/**").hasRole("USER")

                        // 3. General Fallback for other upload types (e.g., community media)
                        .requestMatchers(HttpMethod.POST, "/api/uploads/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/uploads/**").authenticated()

                        // User management endpoints
                        .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                        // 🛡️ ADMIN-ONLY FEATURES
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")           // Admin dashboard
                        .requestMatchers("/api/analytics/**").hasRole("ADMIN")        // Analytics
                        .requestMatchers("/api/moderation/**").hasRole("ADMIN")       // Moderation

                        // User flags endpoint
                        .requestMatchers("/api/flags/**").authenticated()

                        // 📚 DOCUMENTATION
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()

                        // 🔒 Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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

        configuration.setAllowedOriginPatterns(List.of("*")); // ✅ ngrok + localhost + prod
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        );
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}