package com.clinica.services;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.exceptions.CodigoDuplicadoException;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.EspecialidadRepository;
import com.clinica.model.repositories.RolRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrabajadorServiceTest {

    @Mock
    EspecialidadRepository especialidadRepository;

    @Mock
    TrabajadorRepository trabajadorRepository;

    @Mock
    RolRepository rolRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    TrabajadorService trabajadorService;

    @Test
    void crearGeneraUsernameEmailInstitucionalYPasswordHasheado() {
        Rol admin = TestFixtures.rol(1L, "ADMINISTRADOR");
        TrabajadorRequestDTO dto = TestFixtures.trabajadorRequest(1L, "Ana Torres Vega", "12345678", "secreto1");
        when(trabajadorRepository.existsByDni("12345678")).thenReturn(false);
        when(rolRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(trabajadorRepository.existsByUsername("atorres")).thenReturn(false);
        when(trabajadorRepository.existsByEmail("atorres@cpluzdeltunel.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto1")).thenReturn("hash");
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(invocation -> {
            Trabajador trabajador = invocation.getArgument(0);
            trabajador.setId(1L);
            return trabajador;
        });

        TrabajadorResponseDTO response = trabajadorService.crear(dto);

        assertThat(response.getUsername()).isEqualTo("atorres");
        assertThat(response.getEmail()).isEqualTo("atorres@cpluzdeltunel.com");
        ArgumentCaptor<Trabajador> captor = ArgumentCaptor.forClass(Trabajador.class);
        verify(trabajadorRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    void crearConDniDuplicadoLanzaCodigoDuplicado() {
        TrabajadorRequestDTO dto = TestFixtures.trabajadorRequest(1L, "Ana Torres", "12345678", "secreto1");
        when(trabajadorRepository.existsByDni("12345678")).thenReturn(true);

        assertThatThrownBy(() -> trabajadorService.crear(dto))
                .isInstanceOf(CodigoDuplicadoException.class)
                .hasMessageContaining("DNI");
    }

    @Test
    void crearConPasswordCortaLanzaIllegalArgumentException() {
        TrabajadorRequestDTO dto = TestFixtures.trabajadorRequest(1L, "Ana Torres", "12345678", "123");
        when(trabajadorRepository.existsByDni("12345678")).thenReturn(false);

        assertThatThrownBy(() -> trabajadorService.crear(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 6");
    }

    @Test
    void crearMedicoSinColegiaturaLanzaIllegalArgumentException() {
        TrabajadorRequestDTO dto = TestFixtures.trabajadorRequest(2L, "Luis Ramos Perez", "87654321", "secreto1");
        when(trabajadorRepository.existsByDni("87654321")).thenReturn(false);
        when(rolRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.rol(2L, "Medico")));
        when(trabajadorRepository.existsByUsername("lramos")).thenReturn(false);
        when(trabajadorRepository.existsByEmail("lramos@cpluzdeltunel.com")).thenReturn(false);

        assertThatThrownBy(() -> trabajadorService.crear(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colegiatura");
    }

    @Test
    void crearMedicoCargaEspecialidadesAsignadas() {
        Rol medicoRol = TestFixtures.rol(2L, "Medico");
        Especialidad cardiologia = TestFixtures.especialidad(10L, "Cardiologia");
        TrabajadorRequestDTO dto = TestFixtures.trabajadorRequest(2L, "Luis Ramos Perez", "87654321", "secreto1");
        dto.setColegiatura("CMP123");
        dto.setEspecialidadesIds(List.of(10L));
        when(trabajadorRepository.existsByDni("87654321")).thenReturn(false);
        when(rolRepository.findById(2L)).thenReturn(Optional.of(medicoRol));
        when(trabajadorRepository.existsByUsername("lramos")).thenReturn(false);
        when(trabajadorRepository.existsByEmail("lramos@cpluzdeltunel.com")).thenReturn(false);
        when(especialidadRepository.findAllById(List.of(10L))).thenReturn(List.of(cardiologia));
        when(passwordEncoder.encode("secreto1")).thenReturn("hash");
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrabajadorResponseDTO response = trabajadorService.crear(dto);

        assertThat(response.getEspecialidades()).containsExactly("Cardiologia");
    }

    @Test
    void actualizarConDniDeOtroTrabajadorLanzaCodigoDuplicado() {
        Trabajador existente = TestFixtures.trabajador(1L, "atorres", "ADMINISTRADOR", true);
        TrabajadorRequestDTO dto = TestFixtures.trabajadorRequest(1L, "Ana Torres", "87654321", "secreto1");
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(trabajadorRepository.existsByDniAndIdNot("87654321", 1L)).thenReturn(true);

        assertThatThrownBy(() -> trabajadorService.actualizar(1L, dto))
                .isInstanceOf(CodigoDuplicadoException.class);
    }

    @Test
    void cambiarEstadoInvierteActivoDelTrabajador() {
        Trabajador trabajador = TestFixtures.trabajador(1L, "atorres", "ADMINISTRADOR", true);
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));

        trabajadorService.cambiarEstado(1L);

        assertThat(trabajador.isActivo()).isFalse();
        verify(trabajadorRepository).save(trabajador);
    }

    @Test
    void cambiarEstadoDeTrabajadorInexistenteLanzaRecursoNoEncontrado() {
        when(trabajadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trabajadorService.cambiarEstado(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
