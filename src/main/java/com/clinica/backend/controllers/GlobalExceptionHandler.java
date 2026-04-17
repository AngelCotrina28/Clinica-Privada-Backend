package com.clinica.backend.controllers;

import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de validación de DTO (@Valid) → 400 con detalle por campo */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> erroresCampos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erroresCampos.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Error de validación", erroresCampos);
    }

    /** Código duplicado → 409 Conflict */
    @ExceptionHandler(CodigoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCodigoDuplicado(CodigoDuplicadoException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Recurso no encontrado → 404 */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /** Operación inválida sobre inactivo → 422 */
    @ExceptionHandler(MedicamentoInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleInactivo(MedicamentoInactivoException ex) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    /** Cualquier otro error no controlado → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String mensaje, Object detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status",  status.value());
        body.put("error",   status.getReasonPhrase());
        body.put("mensaje", mensaje);
        if (detalle != null) body.put("detalle", detalle);
        return ResponseEntity.status(status).body(body);
    }
}