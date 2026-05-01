package com.clinica.config;

import com.clinica.model.repositories.TrabajadorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final TrabajadorRepository TrabajadorRepository;
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(TrabajadorRepository TrabajadorRepository, JwtAuthenticationFilter jwtAuthFilter) {
        this.TrabajadorRepository = TrabajadorRepository;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return TrabajadorStr -> {
            var Trabajador = TrabajadorRepository.findByUsername(TrabajadorStr)
                    .orElseThrow(() -> new UsernameNotFoundException("Trabajador no encontrado: " + TrabajadorStr));

            return new User(
                    Trabajador.getUsername(),
                    Trabajador.getPasswordHash(),
                    Trabajador.isActivo(),
                    true, true, true,
                    List.of(new SimpleGrantedAuthority("ROLE_" + Trabajador.getRol().getNombre())));
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}