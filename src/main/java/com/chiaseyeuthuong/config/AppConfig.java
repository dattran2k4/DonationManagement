package com.chiaseyeuthuong.config;

import com.chiaseyeuthuong.security.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.TimeZone;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableAsync
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
@RequiredArgsConstructor
public class AppConfig {

    private static final String APP_TIME_ZONE = "Asia/Ho_Chi_Minh";
    public static final List<String> WHITE_LIST_URL = List.of(
            "/",
            "/about", "/ve-chung-toi",
            "/contact", "/lien-he",
            "/events", "/events/*",
            "/su-kien", "/su-kien/*",
            "/activities/*", "/hoat-dong/*",
            "/donations", "/donations/**",
            "/quyen-gop", "/quyen-gop/**"
    );
    private final CustomUserDetailsService customUserDetailsService;

    @PostConstruct
    public void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APP_TIME_ZONE));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF cho REST API
                .authorizeHttpRequests(auth -> auth

                        // Public resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**", "/favicon.ico", "/error").permitAll()

                        // Public pages
                        .requestMatchers(WHITE_LIST_URL.toArray(String[]::new)).permitAll()

                        // Public API docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .requestMatchers("/payments/**").permitAll()

                        // Spring Security default login page
                        .requestMatchers("/login").permitAll()

                        // Admin pages are only for back-office roles
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "ACCOUNTING", "STAFF")

                        // The rest is public
                        .anyRequest().permitAll()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .successHandler((request, response, authentication) -> {
                            String targetUrl = isBackOfficeUser(authentication) ? "/admin/dashboard" : "/";
                            response.sendRedirect(targetUrl);
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .userDetailsService(customUserDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getTimeZone(APP_TIME_ZONE));
        return objectMapper;
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

    @Bean(name = "mailTaskExecutor")
    public TaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }

    private boolean isBackOfficeUser(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(role -> role.equals("ROLE_ADMIN")
                        || role.equals("ROLE_ACCOUNTING")
                        || role.equals("ROLE_STAFF"));
    }
}
