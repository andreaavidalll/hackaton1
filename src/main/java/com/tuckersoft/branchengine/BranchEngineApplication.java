package com.tuckersoft.branchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @EnableAsync va en la clase @Configuration del executor (Parte 3: Asincronia),
// no aqui, para que quede claro quien es dueno de esa pieza.
@SpringBootApplication
public class BranchEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BranchEngineApplication.class, args);
    }
}
