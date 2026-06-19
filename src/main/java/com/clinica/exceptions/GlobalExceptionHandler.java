package com.clinica.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> erroresCampos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erroresCampos.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Error de validacion", erroresCampos);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroFaltante(
            MissingServletRequestParameterException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Falta el parametro requerido: " + ex.getParameterName(),
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoParametroInvalido(
            MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valido";
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Parametro invalido: " + ex.getName() + " debe ser de tipo " + tipoEsperado,
                null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBodyInvalido(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es invalido o esta vacio.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no soportado para este endpoint.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNoSoportado(
            HttpMediaTypeNotSupportedException ex) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRutaNoEncontrada(NoResourceFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "Endpoint no encontrado.", null);
    }

    @ExceptionHandler(CodigoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCodigoDuplicado(CodigoDuplicadoException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleArgumentoInvalido(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoInvalido(IllegalStateException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridadDatos(DataIntegrityViolationException ex) {
        return buildError(HttpStatus.CONFLICT, "No se pudo registrar porque ya existe un dato duplicado o invalido.", null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(AuthorizationDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta accion.", null);
    }

    @ExceptionHandler(MedicamentoInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleInactivo(MedicamentoInactivoException ex) {
        return buildError(HttpStatusCode.valueOf(422), ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Error interno no controlado", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatusCode status, String mensaje, Object detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status instanceof HttpStatus hs ? hs.getReasonPhrase() : "Error");
        body.put("mensaje", mensaje);
        if (detalle != null) {
            body.put("detalle", detalle);
        }
        return ResponseEntity.status(status).body(body);
    }
}
