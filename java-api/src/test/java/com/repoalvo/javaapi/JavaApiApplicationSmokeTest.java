package com.repoalvo.javaapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JavaApiApplicationSmokeTest {

    @Test
    void contextLoads() {
        // Testa se a aplicação Spring Boot inicia sem erros
        // Este é um teste básico de smoke test para garantir que a classe principal está configurada corretamente
        assertThatCode(() -> SpringApplication.run(JavaApiApplication.class)).doesNotThrowAnyException();
    }
}