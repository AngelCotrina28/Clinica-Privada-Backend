package com.clinica.services;

import com.clinica.dtos.AuthLoginRequestDTO;
import com.clinica.dtos.AuthLoginResponseDTO;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    TrabajadorRepository trabajadorRepository;

    @Mock
    JwtService jwtService;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @Test
    void loginConCredencialesValidasRetornaTokenYDatosDelTrabajador() {
        Trabajador trabajador = TestFixtures.trabajador(1L, "admin", "ADMINISTRADOR", true);
        when(trabajadorRepository.findByUsername("admin")).thenReturn(Optional.of(trabajador));
        when(passwordEncoder.matches("secreto", trabajador.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(any(), eq("ADMINISTRADOR"))).thenReturn("jwt-token");

        AuthLoginResponseDTO response = authService.login(new AuthLoginRequestDTO("admin", "secreto"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRol()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    void loginConUsuarioInexistenteLanzaBadCredentials() {
        when(trabajadorRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthLoginRequestDTO("nadie", "secreto")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciales incorrectas");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginConTrabajadorInactivoLanzaDisabledException() {
        Trabajador trabajador = TestFixtures.trabajador(1L, "admin", "ADMINISTRADOR", false);
        when(trabajadorRepository.findByUsername("admin")).thenReturn(Optional.of(trabajador));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequestDTO("admin", "secreto")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("Usuario deshabilitado");
    }

    @Test
    void loginConPasswordHashVacioLanzaBadCredentials() {
        Trabajador trabajador = TestFixtures.trabajador(1L, "admin", "ADMINISTRADOR", true);
        trabajador.setPasswordHash(" ");
        when(trabajadorRepository.findByUsername("admin")).thenReturn(Optional.of(trabajador));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequestDTO("admin", "secreto")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginConHashNoBCryptLanzaBadCredentials() {
        Trabajador trabajador = TestFixtures.trabajador(1L, "admin", "ADMINISTRADOR", true);
        trabajador.setPasswordHash("texto-plano");
        when(trabajadorRepository.findByUsername("admin")).thenReturn(Optional.of(trabajador));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequestDTO("admin", "secreto")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginConRolInvalidoLanzaBadCredentials() {
        Trabajador trabajador = TestFixtures.trabajador(1L, "admin", "ADMINISTRADOR", true);
        trabajador.setRol(TestFixtures.rol(1L, " "));
        when(trabajadorRepository.findByUsername("admin")).thenReturn(Optional.of(trabajador));
        when(passwordEncoder.matches("secreto", trabajador.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new AuthLoginRequestDTO("admin", "secreto")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("rol valido");
    }
}
