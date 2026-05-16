package com.clinica.controllers;

import com.clinica.dtos.AuthLoginRequestDTO;
import com.clinica.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

        private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

        private final AuthService authService;

        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody AuthLoginRequestDTO request) {
                logger.info("> Intento de login para el usuario: {}", request.getUsername());

                try {
                        return ResponseEntity.ok(authService.login(request));

                } catch (DisabledException e) {
                        // Leemos el mensaje exacto que le mandamos desde el AuthService
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("mensaje", e.getMessage()));

                } catch (BadCredentialsException e) {
                        // Leemos el mensaje exacto que le mandamos desde el AuthService
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("mensaje", e.getMessage()));

                } catch (IllegalStateException e) {
                        logger.error("Configuracion invalida de usuario en login: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("mensaje", e.getMessage()));

                } catch (Exception e) {
                        // ¡Clave para futuros bugs! Imprime el error real en la consola de Spring Boot
                        logger.error("Error crítico no controlado en login: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of(
                                                        "mensaje", "Error interno en login",
                                                        "detalle", e.getClass().getSimpleName() + ": " + e.getMessage()));
                }
        }

}
