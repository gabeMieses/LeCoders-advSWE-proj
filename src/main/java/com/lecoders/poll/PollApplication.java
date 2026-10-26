package com.lecoders.poll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the poll service.
 */
@SpringBootApplication
public class PollApplication {

    /**
     * Starts the poll service.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PollApplication.class, args);
    }
}
