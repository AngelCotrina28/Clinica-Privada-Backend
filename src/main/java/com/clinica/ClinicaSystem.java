package com.clinica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class ClinicaSystem {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
			.directory(System.getProperty("user.dir") + "/backend")
			.ignoreIfMissing()
			.load();

		dotenv.entries().forEach(e ->
			System.setProperty(e.getKey(), e.getValue())
		);

		SpringApplication.run(ClinicaSystem.class, args);
	}

}
