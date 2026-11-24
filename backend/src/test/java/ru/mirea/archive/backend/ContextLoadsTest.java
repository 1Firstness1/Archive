package ru.mirea.archive.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContextLoadsTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("1. Тест загрузки Spring Application Context")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.containsBean("documentRequestService")).isTrue();
        assertThat(applicationContext.containsBean("authService")).isTrue();
        assertThat(applicationContext.containsBean("securityConfig")).isTrue();
        assertThat(applicationContext.containsBean("globalExceptionHandler")).isTrue();
    }
}
