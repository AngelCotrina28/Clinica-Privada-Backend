package com.clinica.backend.controllers;

public class MedicamentoInactivoException extends RuntimeException {
    public MedicamentoInactivoException(String msg) { super(msg); }
}