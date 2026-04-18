package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private byte id;

    @Column(nullable = false, unique = true, length = 60)
    private String usuario;

    @Column(name = "password_hash", nullable = false, length = 30)
    private String passwordHash;

    @Column(name = "nombre_completo", nullable = false, length = 50)
    private String nombreCompleto;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
