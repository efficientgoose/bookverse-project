package com.efficientgoose.book;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import com.efficientgoose.book.role.Role;
import com.efficientgoose.book.role.RoleRepository;

@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
public class BookverseApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookverseApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(RoleRepository roleRepository) {
		return args -> {
			if (roleRepository.findByName("USER").isEmpty()) {
				roleRepository.save(
						Role.builder().name("USER").build());
			}
		};
	}

}
