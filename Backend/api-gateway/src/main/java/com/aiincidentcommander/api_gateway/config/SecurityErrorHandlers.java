package com.aiincidentcommander.api_gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Map;

public class SecurityErrorHandlers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AuthenticationEntryPoint unauthorizedHandler() {
        return (HttpServletRequest req, HttpServletResponse res, org.springframework.security.core.AuthenticationException ex) -> {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write(MAPPER.writeValueAsString(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Missing or invalid token",
                    "path", req.getRequestURI()
            )));
        };
    }

    public static AccessDeniedHandler forbiddenHandler() {
        return (HttpServletRequest req, HttpServletResponse res, org.springframework.security.access.AccessDeniedException ex) -> {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write(MAPPER.writeValueAsString(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "You don't have permission for this resource",
                    "path", req.getRequestURI()
            )));
        };
    }
}