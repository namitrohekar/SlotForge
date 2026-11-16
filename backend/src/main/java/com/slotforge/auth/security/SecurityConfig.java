package com.slotforge.auth.security;

import com.slotforge.auth.repository.AppUserRepository;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // ADMIN can do everything a coordinator can, and an HOD everything a faculty member can.
    // ADMIN is deliberately not given HOD review rights. RoleRules keeps the implied pairs off one account.
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_EXAM_COORDINATOR
                ROLE_CONTROLLER > ROLE_HOD
                ROLE_HOD > ROLE_FACULTY
                """);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppUserRepository users, ObjectProvider<GoogleLoginHandler> google)
            throws Exception {
        GoogleLoginHandler googleLogin = google.getIfAvailable();
        if (googleLogin != null) {
            // The React app owns /login, so Spring must not generate its own page there.
            http.oauth2Login(login -> login.loginPage("/login").successHandler(googleLogin).failureHandler(googleLogin));
        }
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/options", "/api/auth/demo-users", "/api/auth/demo-login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.spa())
                // This server returns data and redirects only, so nothing it sends may load a script, be framed or leak the address it came from.
                .headers(headers -> headers
                        .contentSecurityPolicy(policy -> policy.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(policy -> policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
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
