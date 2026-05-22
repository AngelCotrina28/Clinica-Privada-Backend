package com.clinica.services;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.EspecialidadRepository;
import com.clinica.model.repositories.RolRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrabajadorService {

    private static final String DOMINIO_CORREO = "@cpluzdeltunel.com";
    private static final int USERNAME_MAX_LENGTH = 60;
    private static final Set<String> INICIOS_APELLIDO_COMPUESTO = Set.of(
            "de", "del", "la", "las", "los", "san", "santa"
    );
    private static final Set<String> PARTICULAS_DESPUES_DE = Set.of("la", "las", "los");

    private final EspecialidadRepository especialidadRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TrabajadorResponseDTO crear(TrabajadorRequestDTO dto) {
        if (trabajadorRepository.existsByDni(dto.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }
        validarPasswordCreacion(dto.getPassword());

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        IdentidadInstitucional identidad = generarIdentidadInstitucional(dto.getNombreCompleto(), null);
        
        if (rol.getNombre().equalsIgnoreCase("Medico")) {
            if (dto.getColegiatura() == null || dto.getColegiatura().isBlank()) {
                throw new RuntimeException("El número de colegiatura es obligatorio para el rol Médico.");
            }
        }

        Set<Especialidad> especialidades = new java.util.HashSet<>();
        if (rol.getNombre().equalsIgnoreCase("Medico") && dto.getEspecialidadesIds() != null && !dto.getEspecialidadesIds().isEmpty()) {
            especialidades = new java.util.HashSet<>(especialidadRepository.findAllById(dto.getEspecialidadesIds()));
        }

        Trabajador trabajador = Trabajador.builder()
                .dni(dto.getDni())
                .nombreCompleto(dto.getNombreCompleto())
                .username(identidad.username())
                .email(identidad.email())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .activo(true)
                // --- MAPEO DE NUEVOS CAMPOS ---
                .telefono(dto.getTelefono())
                .fechaNacimiento(dto.getFechaNacimiento())
                .colegiatura(dto.getColegiatura())
                .especialidades(especialidades)
                .build();

        trabajador = trabajadorRepository.save(trabajador);
        return mapToDTO(trabajador);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Trabajador trabajador = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado"));
        
        // Si está en true lo pasa a false, y si está en false lo pasa a true
        trabajador.setActivo(!trabajador.isActivo()); 
        
        trabajadorRepository.save(trabajador);
    }

    public List<TrabajadorResponseDTO> listarTodos() {
        // Volvemos a usar findAll()
        return trabajadorRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TrabajadorResponseDTO> listarMedicosActivos() {
        // Buscamos ignorando mayúsculas/minúsculas para evitar errores tipográficos en BD
        return trabajadorRepository.findByRolNombreIgnoreCaseAndActivoTrue("MEDICO").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TrabajadorResponseDTO mapToDTO(Trabajador trabajador) {
        List<String> especialidadesNombres = trabajador.getEspecialidades() != null 
            ? trabajador.getEspecialidades().stream().map(Especialidad::getNombre).collect(Collectors.toList())
            : new ArrayList<>()
            ;
        return TrabajadorResponseDTO.builder()
                .id(trabajador.getId())
                .dni(trabajador.getDni())
                .nombreCompleto(trabajador.getNombreCompleto())
                .username(trabajador.getUsername())
                .email(trabajador.getEmail())
                .telefono(trabajador.getTelefono()) 
                .fechaNacimiento(trabajador.getFechaNacimiento()) 
                .colegiatura(trabajador.getColegiatura()) 
                .rolId(trabajador.getRol().getId())
                .nombreRol(trabajador.getRol().getNombre())
                .activo(trabajador.isActivo())
                .especialidades(especialidadesNombres)
                .build();
    }

    @Transactional
    public TrabajadorResponseDTO actualizar(Long id, TrabajadorRequestDTO dto) {

        Trabajador trabajador = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado"));

        if (!trabajador.getDni().equals(dto.getDni()) && trabajadorRepository.existsByDniAndIdNot(dto.getDni(), id)) {
            throw new RuntimeException("El DNI ya está registrado por otro trabajador");
        }

        IdentidadInstitucional identidad = generarIdentidadInstitucional(dto.getNombreCompleto(), id);

        // --- APLICAR TODOS LOS CAMBIOS ---
        trabajador.setDni(dto.getDni());
        trabajador.setNombreCompleto(dto.getNombreCompleto());
        trabajador.setUsername(identidad.username());
        trabajador.setEmail(identidad.email());
        trabajador.setTelefono(dto.getTelefono());
        trabajador.setFechaNacimiento(dto.getFechaNacimiento());
        trabajador.setColegiatura(dto.getColegiatura());
        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        trabajador.setRol(rol);

        if (rol.getNombre().equalsIgnoreCase("Medico")) {
            if (dto.getColegiatura() == null || dto.getColegiatura().isBlank()) {
                throw new RuntimeException("El número de colegiatura es obligatorio para el rol Médico.");
            }
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            trabajador.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return mapToDTO(trabajadorRepository.save(trabajador));
    }

    private void validarPasswordCreacion(String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }
        if (password.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        }
    }

    private IdentidadInstitucional generarIdentidadInstitucional(String nombreCompleto, Long idExcluir) {
        NombreInstitucional nombre = extraerNombreInstitucional(nombreCompleto);
        String base = nombre.inicialNombre() + nombre.primerApellido();

        String candidato = armarCandidato(base, "");
        if (identidadDisponible(candidato, idExcluir)) {
            return identidadDesdeUsername(candidato);
        }

        String prefijoConSegundoApellido = base;
        if (!nombre.inicialSegundoApellido().isBlank()) {
            prefijoConSegundoApellido = base + nombre.inicialSegundoApellido();
            candidato = armarCandidato(base, nombre.inicialSegundoApellido());
            if (identidadDisponible(candidato, idExcluir)) {
                return identidadDesdeUsername(candidato);
            }
        }

        int correlativo = 2;
        while (true) {
            candidato = armarCandidato(prefijoConSegundoApellido, String.valueOf(correlativo));
            if (identidadDisponible(candidato, idExcluir)) {
                return identidadDesdeUsername(candidato);
            }
            correlativo++;
        }
    }

    private boolean identidadDisponible(String username, Long idExcluir) {
        String email = username + DOMINIO_CORREO;
        if (idExcluir == null) {
            return !trabajadorRepository.existsByUsername(username)
                    && !trabajadorRepository.existsByEmail(email);
        }
        return !trabajadorRepository.existsByUsernameAndIdNot(username, idExcluir)
                && !trabajadorRepository.existsByEmailAndIdNot(email, idExcluir);
    }

    private IdentidadInstitucional identidadDesdeUsername(String username) {
        return new IdentidadInstitucional(username, username + DOMINIO_CORREO);
    }

    private NombreInstitucional extraerNombreInstitucional(String nombreCompleto) {
        List<String> partes = Arrays.stream(normalizarNombre(nombreCompleto).split(" "))
                .filter(parte -> !parte.isBlank())
                .toList();

        if (partes.size() < 2) {
            throw new RuntimeException("Ingrese al menos un nombre y un apellido para generar el usuario institucional.");
        }

        String inicialNombre = primeraLetra(partes.get(0));
        BloqueApellido primerApellido = extraerPrimerApellido(partes, indiceInicioPrimerApellido(partes));
        String inicialSegundoApellido = primerApellido.siguienteIndice() < partes.size()
                ? primeraLetra(partes.get(primerApellido.siguienteIndice()))
                : "";

        return new NombreInstitucional(inicialNombre, primerApellido.valor(), inicialSegundoApellido);
    }

    private int indiceInicioPrimerApellido(List<String> partes) {
        if (partes.size() == 2 || INICIOS_APELLIDO_COMPUESTO.contains(partes.get(1))) {
            return 1;
        }
        return partes.size() >= 4 ? 2 : 1;
    }

    private BloqueApellido extraerPrimerApellido(List<String> partes, int inicio) {
        List<String> apellido = new ArrayList<>();
        int indice = inicio;
        String primeraParte = partes.get(indice++);
        apellido.add(primeraParte);

        if ("de".equals(primeraParte)) {
            while (indice < partes.size() && PARTICULAS_DESPUES_DE.contains(partes.get(indice))) {
                apellido.add(partes.get(indice++));
            }
            if (indice < partes.size()) {
                apellido.add(partes.get(indice++));
            }
        } else if (INICIOS_APELLIDO_COMPUESTO.contains(primeraParte) && indice < partes.size()) {
            apellido.add(partes.get(indice++));
        }

        String valor = apellido.stream()
                .map(this::soloAlfanumerico)
                .collect(Collectors.joining());

        if (valor.isBlank()) {
            throw new RuntimeException("No se pudo generar el usuario con el apellido ingresado.");
        }

        return new BloqueApellido(valor, indice);
    }

    private String normalizarNombre(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String primeraLetra(String valor) {
        String limpio = soloAlfanumerico(valor);
        if (limpio.isBlank()) {
            throw new RuntimeException("No se pudo generar el usuario con el nombre ingresado.");
        }
        return limpio.substring(0, 1);
    }

    private String soloAlfanumerico(String valor) {
        return valor == null ? "" : valor.replaceAll("[^a-z0-9]", "");
    }

    private String armarCandidato(String prefijo, String sufijo) {
        int longitudDisponible = USERNAME_MAX_LENGTH - sufijo.length();
        String prefijoRecortado = prefijo.length() > longitudDisponible
                ? prefijo.substring(0, longitudDisponible)
                : prefijo;
        return prefijoRecortado + sufijo;
    }

    private record IdentidadInstitucional(String username, String email) { }

    private record NombreInstitucional(
            String inicialNombre,
            String primerApellido,
            String inicialSegundoApellido
    ) { }

    private record BloqueApellido(String valor, int siguienteIndice) { }
}
