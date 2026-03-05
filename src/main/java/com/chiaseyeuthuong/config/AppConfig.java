package com.chiaseyeuthuong.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AppConfig {

    public static final List<String> WHITE_LIST_URL = List.of("/about", "/contact", "/events/*", "/activities/*", "/");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF cho REST API
                .authorizeHttpRequests(auth -> auth

                                .requestMatchers("/**").permitAll()

                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                // 1. Nhóm Public cho Donor
                                .requestMatchers(WHITE_LIST_URL.toArray(String[]::new)).permitAll()

                                // 2. Nhóm Swagger/API Docs
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                // 3. Nhóm Admin/Staff
//                        .requestMatchers("/api/v1/admin/**").hasAnyRole("STAFF", "ADMIN")
//                        .requestMatchers("/api/v1/approval/**").hasRole("ADMIN")

                                .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginProcessingUrl("/api/login") // Endpoint để gửi username/password lên
                        .successHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK)// Trả về 200 thay vì redirect
                        )
                        .failureHandler((request, response, exception) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                )

                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                        })
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:8080/")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Allowed HTTP methods
                        .allowedHeaders("*") // Allowed request headers
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
