// HistoriaDuplicadaException.java
/*Lanzada cuando se intenta crear una historia para un DNI que ya
existe en la base de datos.*/
// ============================================================
package com.clinica.exceptions;

public class HistoriaDuplicadaException extends RuntimeException {
    public HistoriaDuplicadaException(String msg) {
        super(msg);
    }
}


// ============================================================
// AGREGAR en GlobalExceptionHandler.java
// (Dentro de la clase existente, junto a los otros @ExceptionHandler)
// ============================================================

// /** Historia duplicada → 409 Conflict */
// @ExceptionHandler(IllegalStateException.class)
// public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
//     return buildError(HttpStatus.CONFLICT, ex.getMessage(), null);
// }
//
// /** Argumento inválido (ej: trabajador no es médico) → 400 */
// @ExceptionHandler(IllegalArgumentException.class)
// public ResponseEntity<Map<String, Object>> handleBadArg(IllegalArgumentException ex) {
//     return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
// }