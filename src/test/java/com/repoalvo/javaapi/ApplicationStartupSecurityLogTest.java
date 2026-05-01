package com.repoalvo.javaapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
public class ApplicationStartupSecurityLogTest {

    @Test
    public void whenApplicationStarts_thenSpringSecurityLogsPresent(CapturedOutput output) {
        String logs = output.getAll();

        // Check for Spring Security related log entries
        assertThat(logs).contains("Spring Security");
        assertThat(logs).doesNotContain("ERROR");
        assertThat(logs).doesNotContain("WARN");
    }
}