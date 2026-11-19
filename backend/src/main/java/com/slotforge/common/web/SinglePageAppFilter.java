package com.slotforge.common.web;

import java.io.IOException;
import java.util.Arrays;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * When the built frontend is served from here, a browser that opens or refreshes an address such as /sessions/13
 * is given the React app, which owns those addresses. API calls, Google sign-in and files such as /assets/app.js pass through.
 */
@Component
public class SinglePageAppFilter extends OncePerRequestFilter {

    private final boolean appBuilt;

    // Only when the built frontend is among the static files, as in the deployed image. In development Vite serves the pages.
    public SinglePageAppFilter(WebProperties web, ResourceLoader resources) {
        this.appBuilt = Arrays.stream(web.getResources().getStaticLocations())
                .anyMatch(location -> resources.getResource(location + "index.html").exists());
    }

    static boolean isAppPage(String method, String path) {
        if (!"GET".equals(method) || path.equals("/") || path.equals("/index.html")) return false;
        if (path.startsWith("/api/") || path.equals("/api") || path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) return false;
        return !path.substring(path.lastIndexOf('/') + 1).contains(".");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (appBuilt && isAppPage(request.getMethod(), path)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
