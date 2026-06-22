package com.clinica.support;

import com.clinica.dtos.AbrirHistoriaClinicaRequestDTO;
import com.clinica.dtos.AtencionMedicaRequestDTO;
import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.DetalleRecetaRequestDTO;
import com.clinica.dtos.ItemRecetaRequestDTO;
import com.clinica.dtos.MedicamentoRequestDTO;
import com.clinica.dtos.RecetaRequestDTO;
import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TurnoRequestDTO;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.entities.CategoriaMedicamento;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.Consultorio;
import com.clinica.model.entities.DetalleReceta;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.HistoriaClinica;
import com.clinica.model.entities.Medicamento;
import com.clinica.model.entities.OrdenAtencionEmergencia;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.Receta;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.TipoCita;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.entities.Turno;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Rol rol(Long id, String nombre) {
        return Rol.builder()
                .id(id)
                .nombre(nombre)
                .descripcion(nombre)
                .build();
    }

    public static Especialidad especialidad(Long id, String nombre) {
        return Especialidad.builder()
                .id(id)
                .nombre(nombre)
                .descripcion(nombre)
                .build();
    }

    public static Trabajador trabajador(Long id, String username, String rolNombre, boolean activo) {
        return Trabajador.builder()
                .id(id)
                .dni(String.format("%08d", id))
                .nombreCompleto("Trabajador " + id)
                .username(username)
                .email(username + "@cpluzdeltunel.com")
                .passwordHash("$2a$10$123456789012345678901u")
                .rol(rol(id, rolNombre))
                .activo(activo)
                .especialidades(new HashSet<>())
                .build();
    }

    public static Trabajador medico(Long id) {
        Trabajador medico = trabajador(id, "medico" + id, "MEDICO", true);
        medico.getEspecialidades().add(especialidad(10L, "Medicina General"));
        medico.setColegiatura("CMP-" + id);
        return medico;
    }

    public static Paciente paciente(Long id) {
        return Paciente.builder()
                .id(id)
                .dni("7000000" + id)
                .nombreCompleto("Paciente " + id)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .historiaClinicaId(id)
                .registradoPor(trabajador(99L, "admision", "ADMISION", true))
                .activo(true)
                .build();
    }

    public static HistoriaClinica historia(Long id) {
        Paciente paciente = paciente(id);
        return HistoriaClinica.builder()
                .id(id)
                .numeroHistoria("HC-" + id)
                .dniPaciente(paciente.getDni())
                .nombreCompleto(paciente.getNombreCompleto())
                .telefono("999999999")
                .email("paciente" + id + "@mail.com")
                .fechaNacimiento("1990-01-01")
                .genero("M")
                .direccion("Av. Salud 123")
                .creadoPor(trabajador(99L, "admision", "ADMISION", true))
                .paciente(paciente)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static CategoriaMedicamento categoria(Integer id) {
        return CategoriaMedicamento.builder()
                .id(id)
                .nombre("Analgesicos")
                .build();
    }

    public static Medicamento medicamento(Long id, int stock) {
        return Medicamento.builder()
                .id(id)
                .codigo("MED-" + id)
                .nombre("Paracetamol " + id)
                .categoria(categoria(1))
                .presentacion("Tabletas")
                .laboratorio("Lab")
                .precioUnitario(new BigDecimal("4.50"))
                .stockActual(stock)
                .stockMinimo(5)
                .requiereReceta(false)
                .activo(true)
                .createdBy(trabajador(99L, "farmacia", "FARMACIA", true))
                .build();
    }

    public static Consultorio consultorio(Long id, Especialidad especialidad) {
        return Consultorio.builder()
                .id(id)
                .nombre("Consultorio " + id)
                .numero(String.valueOf(id))
                .piso("1")
                .especialidad(especialidad)
                .activo(true)
                .build();
    }

    public static Turno turno(Long id, Trabajador medico, Consultorio consultorio, LocalDate fecha,
            LocalTime inicio, LocalTime fin) {
        return Turno.builder()
                .id(id)
                .medico(medico)
                .consultorio(consultorio)
                .fecha(fecha)
                .diaSemana(Turno.DiaSemana.valueOf(switch (fecha.getDayOfWeek()) {
                    case MONDAY -> "LUNES";
                    case TUESDAY -> "MARTES";
                    case WEDNESDAY -> "MIERCOLES";
                    case THURSDAY -> "JUEVES";
                    case FRIDAY -> "VIERNES";
                    case SATURDAY -> "SABADO";
                    case SUNDAY -> "DOMINGO";
                }))
                .horaInicio(inicio)
                .horaFin(fin)
                .duracionMinutos(30)
                .activo(true)
                .build();
    }

    public static TipoCita tipoCita() {
        return TipoCita.builder()
                .id(1L)
                .nombre("CONSULTA EXTERNA")
                .descripcion("Cita externa")
                .duracionMinutos(30)
                .activo(true)
                .build();
    }

    public static Cita cita(Long id, HistoriaClinica historia, Trabajador medico, Turno turno, LocalDateTime fechaHora) {
        return Cita.builder()
                .id(id)
                .numeroCita("CT-20260622-ABCDEF")
                .historiaClinica(historia)
                .paciente(historia.getPaciente())
                .medico(medico)
                .consultorio(turno.getConsultorio())
                .turno(turno)
                .tipoCita(tipoCita())
                .fechaHoraCita(fechaHora)
                .motivoConsulta("Consulta")
                .creadoPor(trabajador(99L, "admision", "ADMISION", true))
                .estado(Cita.EstadoCita.CONFIRMADA)
                .build();
    }

    public static Receta receta(Long id, Receta.EstadoReceta estado, Medicamento medicamento, int cantidad) {
        HistoriaClinica historia = historia(id);
        Trabajador medico = medico(10L);
        Receta receta = Receta.builder()
                .id(id)
                .numeroReceta("REC-00000" + id)
                .atencionMedica(AtencionMedica.builder().id(1L).historiaClinica(historia).medico(medico).build())
                .medico(medico)
                .paciente(historia.getPaciente())
                .indicacionesGenerales("Tomar con agua")
                .estado(estado)
                .detalles(new ArrayList<>())
                .build();
        DetalleReceta detalle = DetalleReceta.builder()
                .id(1L)
                .receta(receta)
                .medicamento(medicamento)
                .dosis("500mg")
                .frecuencia("Cada 8 horas")
                .duracion("3 dias")
                .cantidadPrescrita(cantidad)
                .cantidadDespachada(0)
                .viaAdministracion("Oral")
                .indicaciones("Despues de comer")
                .build();
        receta.getDetalles().add(detalle);
        return receta;
    }

    public static OrdenAtencionEmergencia ordenEmergencia(Long id, HistoriaClinica historia, Trabajador medico) {
        return OrdenAtencionEmergencia.builder()
                .id(id)
                .numeroOrden("OE-ABC123")
                .historiaClinica(historia)
                .medico(medico)
                .motivo("Dolor agudo")
                .generadoPor(trabajador(99L, "admision", "ADMISION", true))
                .estado(OrdenAtencionEmergencia.EstadoOrden.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static MedicamentoRequestDTO medicamentoRequest() {
        return MedicamentoRequestDTO.builder()
                .nombre("Paracetamol")
                .nombreGenerico("Acetaminofen")
                .descripcion("Analgesico")
                .categoriaId(1)
                .presentacion("Tabletas")
                .laboratorio("Lab")
                .precioUnitario(new BigDecimal("4.50"))
                .stockInicial(20)
                .stockMinimo(5)
                .requiereReceta(false)
                .build();
    }

    public static TrabajadorRequestDTO trabajadorRequest(Long rolId, String nombre, String dni, String password) {
        TrabajadorRequestDTO dto = new TrabajadorRequestDTO();
        dto.setRolId(rolId);
        dto.setNombreCompleto(nombre);
        dto.setDni(dni);
        dto.setPassword(password);
        dto.setTelefono("999999999");
        dto.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return dto;
    }

    public static RecetaRequestDTO recetaRequest() {
        return RecetaRequestDTO.builder()
                .atencionMedicaId(1L)
                .medicoId(10L)
                .pacienteId(20L)
                .indicacionesGenerales("Reposo")
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .detalles(List.of(DetalleRecetaRequestDTO.builder()
                        .medicamentoId(30L)
                        .dosis("500mg")
                        .frecuencia("Cada 8 horas")
                        .duracion("3 dias")
                        .cantidadPrescrita(3)
                        .viaAdministracion("Oral")
                        .indicaciones("Despues de comer")
                        .build()))
                .build();
    }

    public static CitaRequestDTO citaRequest(LocalDateTime fechaHora, Long turnoId) {
        return CitaRequestDTO.builder()
                .historiaClinicaId(1L)
                .medicoId(10L)
                .especialidadId(10L)
                .fechaHora(fechaHora)
                .turnoId(turnoId)
                .motivoConsulta("Control")
                .build();
    }

    public static TurnoRequestDTO turnoRequest(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        TurnoRequestDTO dto = new TurnoRequestDTO();
        dto.setMedicoId(10L);
        dto.setEspecialidadId(10L);
        dto.setFecha(fecha);
        dto.setHoraInicio(inicio);
        dto.setHoraFin(fin);
        return dto;
    }

    public static AbrirHistoriaClinicaRequestDTO abrirHistoriaRequest(String documento) {
        return AbrirHistoriaClinicaRequestDTO.builder()
                .dniPaciente(documento)
                .nombreCompleto("Paciente Nuevo")
                .telefono("999999999")
                .email("nuevo@mail.com")
                .fechaNacimiento("1995-05-15")
                .genero("F")
                .direccion("Av. Nueva 100")
                .desdeAdmision(true)
                .build();
    }

    public static AtencionMedicaRequestDTO atencionRequest(String codigoAtencion) {
        return AtencionMedicaRequestDTO.builder()
                .historiaClinicaId(1L)
                .medicoId(10L)
                .numeroCita(codigoAtencion)
                .diagnosticoPrincipal("Gripe")
                .notasEvolucion("Paciente estable")
                .itemsReceta(List.of(ItemRecetaRequestDTO.builder()
                        .medicamentoId(30L)
                        .cantidad(2)
                        .dias(3)
                        .indicaciones("Cada 12 horas")
                        .build()))
                .build();
    }

    public static void autenticarComo(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(username, "N/A", List.of()));
    }

    public static void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }
}
