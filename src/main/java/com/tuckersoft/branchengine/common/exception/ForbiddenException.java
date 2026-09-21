package com.tuckersoft.branchengine.common.exception;

/** Lanzar cuando el usuario autenticado no es dueno del recurso que intenta tocar. Se traduce a HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
