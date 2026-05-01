package com.clinica.controllers;

import com.clinica.dtos.AuthLoginRequestDTO;
import com.clinica.dtos.AuthLoginResponseDTO;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TrabajadorRepository trabajadorRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDTO> login(@RequestBody AuthLoginRequestDTO request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        Trabajador trabajador = trabajadorRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String nombreRol = trabajador.getRol().getNombre();

        String token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        trabajador.getUsername(),
                        trabajador.getPasswordHash(),
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + nombreRol))),
                nombreRol);

        AuthLoginResponseDTO response = new AuthLoginResponseDTO(token, trabajador.getUsername(), nombreRol);
        return ResponseEntity.ok(response);
    }
}