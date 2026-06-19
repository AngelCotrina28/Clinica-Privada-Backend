package com.clinica.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(name = "app.security.require-auth", havingValue = "true")
public class MethodSecurityConfig {
}
