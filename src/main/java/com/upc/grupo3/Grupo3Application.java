package com.upc.grupo3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Grupo3Application {

    public static void main(String[] args) {
        SpringApplication.run(Grupo3Application.class, args);
    }

}
