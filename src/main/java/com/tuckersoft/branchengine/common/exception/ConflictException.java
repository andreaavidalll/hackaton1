package com.tuckersoft.branchengine.common.exception;

/** Lanzar ante duplicados (email/nodeCode/playerTag) o un conflicto de estado (partida FINALIZADA). Se traduce a HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
