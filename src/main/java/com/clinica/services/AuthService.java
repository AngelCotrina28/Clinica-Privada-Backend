package com.clinica.services;

import com.clinica.dtos.AuthLoginRequestDTO;
import com.clinica.dtos.AuthLoginResponseDTO;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final TrabajadorRepository trabajadorRepository;
        private final JwtService jwtService;
        private final PasswordEncoder passwordEncoder;

        public AuthLoginResponseDTO login(AuthLoginRequestDTO request) {

                Trabajador trabajador = trabajadorRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

                if (!trabajador.isActivo()) {
                        throw new DisabledException("Usuario deshabilitado, contacte al administrador");
                }

                if (trabajador.getPasswordHash() == null || trabajador.getPasswordHash().isBlank()) {
                        throw new IllegalStateException("El usuario no tiene password_hash configurado.");
                }

                if (!esHashBCrypt(trabajador.getPasswordHash())) {
                        throw new IllegalStateException("El password_hash del usuario no tiene formato BCrypt valido.");
                }

                boolean passwordValido;
                try {
                        passwordValido = passwordEncoder.matches(request.getPassword(), trabajador.getPasswordHash());
                } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("El password_hash BCrypt del usuario esta corrupto.");
                }

                if (!passwordValido) {
                        throw new BadCredentialsException("Credenciales incorrectas");
                }

                if (trabajador.getRol() == null || trabajador.getRol().getNombre() == null
                                || trabajador.getRol().getNombre().isBlank()) {
                        throw new IllegalStateException("El usuario no tiene un rol valido asociado.");
                }

                String nombreRol = trabajador.getRol().getNombre();

                String token = jwtService.generateToken(
                                new User(
                                                trabajador.getUsername(),
                                                trabajador.getPasswordHash(),
                                                List.of(new SimpleGrantedAuthority("ROLE_" + nombreRol))),
                                nombreRol);

                return new AuthLoginResponseDTO(
                                token,
                                trabajador.getUsername(),
                                trabajador.getNombreCompleto(),
                                nombreRol);
        }

        private boolean esHashBCrypt(String passwordHash) {
                return passwordHash.startsWith("$2a$")
                                || passwordHash.startsWith("$2b$")
                                || passwordHash.startsWith("$2y$");
        }
}
