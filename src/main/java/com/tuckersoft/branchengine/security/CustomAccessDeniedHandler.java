package com.tuckersoft.branchengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckersoft.branchengine.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Spring Security responde 403 con cuerpo vacio por defecto. Este bean lo intercepta
 * para que use el formato de error del enunciado. Cubre los 403 que la propia cadena
 * de filtros produce (hasRole() denegado); los 403 de propiedad (partida/decision
 * ajena) los lanza el service como ForbiddenException y los atrapa el
 * GlobalExceptionHandler con el mismo formato.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws java.io.IOException {
        ErrorResponse body = new ErrorResponse(
                "FORBIDDEN",
                "No tienes permiso para acceder a este recurso",
                Instant.now(),
                request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
