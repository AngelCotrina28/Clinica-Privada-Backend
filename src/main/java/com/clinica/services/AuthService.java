package com.clinica.services;

import com.clinica.dtos.AuthLoginRequestDTO;
import com.clinica.dtos.AuthLoginResponseDTO;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final TrabajadorRepository trabajadorRepository;
        private final JwtService jwtService;

        public AuthLoginResponseDTO login(AuthLoginRequestDTO request) {

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getUsername(),
                                                        request.getPassword()));
                } catch (DisabledException e) {
                        throw new DisabledException("Usuario deshabilitado, contacte al administrador");
                } catch (Exception e) {
                        throw new BadCredentialsException("Credenciales incorrectas");
                }

                Trabajador trabajador = trabajadorRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new RuntimeException("Error interno al recuperar el usuario"));

                String nombreRol = trabajador.getRol().getNombre();

                String token = jwtService.generateToken(
                                new User(
                                                trabajador.getUsername(),
                                                trabajador.getPasswordHash(),
                                                List.of(new SimpleGrantedAuthority("ROLE_" + nombreRol))),
                                nombreRol);

                return new AuthLoginResponseDTO(token, trabajador.getUsername(), nombreRol);
        }
}