package ru.savin.deviceservice.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true) // Включаем поддержку @RolesAllowed
@Slf4j
public class SecurityConfig  {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 1. Отключаем CSRF защиту
//                .authorizeHttpRequests(auth -> auth     // 2. Настройка авторизации запросов
//                        .requestMatchers(HttpMethod.GET,"/api/v1/devices/**").hasAnyRole("admin","user") // GET доступен для admin и user
//                        .requestMatchers("/api/v1/devices/**").hasRole("admin") // Все остальные методы только для admin
//                        .anyRequest().permitAll()  // Остальные запросы доступны все
//                        //.anyRequest().denyAll() // Все остальные запросы запрещены
//                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/**").permitAll(); //для метрик разрешаем доступ
                    auth.anyRequest().authenticated(); // Все запросы требуют аутентификации
                        }
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = realmAccess != null ? (List<String>) realmAccess.get("roles") : Collections.emptyList();
            return roles.stream()
                    .map(role -> "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }

}
