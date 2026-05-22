package com.clinica.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clinica.model.entities.Trabajador;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {
    Optional<Trabajador> findByUsername(String username);
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);
    List<Trabajador> findAllByActivoTrue();
    List<Trabajador> findByRolNombreIgnoreCaseAndActivoTrue(String nombreRol);
}
