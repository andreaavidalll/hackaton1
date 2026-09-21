package com.tuckersoft.branchengine.common.dto;

import java.time.Instant;

/** Formato de error unico exigido por el enunciado: error, message, timestamp, path. */
public record ErrorResponse(String error, String message, Instant timestamp, String path) {
}
