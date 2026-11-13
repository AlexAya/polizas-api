package com.alexanderaya.polizas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Valida el header obligatorio x-api-key en todas las peticiones a la API.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_API_KEY = "x-api-key";

    private final byte[] apiKeyEsperada;

    public ApiKeyFilter(@Value("${app.security.api-key}") String apiKey) {
        this.apiKeyEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // La consola de H2 es solo una herramienta de desarrollo y no forma parte de la API
        return request.getRequestURI().startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKeyRecibida = request.getHeader(HEADER_API_KEY);

        if (apiKeyRecibida == null || !esValida(apiKeyRecibida)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"mensaje\":\"Header x-api-key ausente o inválido\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean esValida(String apiKeyRecibida) {
        // Comparación en tiempo constante para evitar ataques de temporización
        return MessageDigest.isEqual(apiKeyEsperada, apiKeyRecibida.getBytes(StandardCharsets.UTF_8));
    }
}