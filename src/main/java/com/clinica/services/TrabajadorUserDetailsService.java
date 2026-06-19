package com.clinica.services;

import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrabajadorUserDetailsService implements UserDetailsService {

    private final TrabajadorRepository trabajadorRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        var trabajador = trabajadorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Trabajador no encontrado: " + username));

        return new User(
                trabajador.getUsername(),
                trabajador.getPasswordHash(),
                trabajador.isActivo(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + trabajador.getRol().getNombre())));
    }
}
