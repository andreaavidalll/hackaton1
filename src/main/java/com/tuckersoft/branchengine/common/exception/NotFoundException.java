package com.tuckersoft.branchengine.common.exception;

/** Lanzar cuando un recurso pedido por id/codigo no existe. Se traduce a HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
