package com.slotforge.auth.security;

import com.slotforge.auth.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // ADMIN can do everything a coordinator can, and an HOD everything a faculty member can.
    // ADMIN is deliberately not given HOD review rights. RoleRules keeps the implied pairs off one account.
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_EXAM_COORDINATOR
                ROLE_HOD > ROLE_FACULTY
                """);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppUserRepository users) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/options", "/api/auth/demo-users", "/api/auth/demo-login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.spa())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> SecurityErrors.write(
                                response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in to continue."))
                        .accessDeniedHandler((request, response, ex) -> SecurityErrors.write(
                                response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this.")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .addFilterBefore(new ActiveUserFilter(users), AuthorizationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
