package com.clinica.exceptions;

public class MedicamentoInactivoException extends RuntimeException {
    public MedicamentoInactivoException(String msg) { super(msg); }
}