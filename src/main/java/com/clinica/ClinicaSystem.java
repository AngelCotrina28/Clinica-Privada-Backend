package com.clinica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class ClinicaSystem {

	public static void main(String[] args) {
		Path currentDir = Path.of(System.getProperty("user.dir"));
		Path dotenvDir = Files.exists(currentDir.resolve(".env"))
			? currentDir
			: currentDir.resolve("backend");

		Dotenv dotenv = Dotenv.configure()
			.directory(dotenvDir.toString())
			.ignoreIfMissing()
			.load();

		dotenv.entries().forEach(e ->
			System.setProperty(e.getKey(), e.getValue())
		);

		SpringApplication.run(ClinicaSystem.class, args);
	}

}
