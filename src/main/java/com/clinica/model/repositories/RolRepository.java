package com.clinica.model.repositories;

import com.clinica.model.entities.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepository extends JpaRepository<Rol, Byte> {
    // Spring Data JPA ya nos da el método findById() por defecto
}