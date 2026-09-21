package com.tuckersoft.branchengine.common.exception;

/** Lanzar para reglas de negocio invalidas que no cubre @Valid (rol invalido, nodo lleno, etc). Se traduce a HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
