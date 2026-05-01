package com.repoalvo.javaapi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class DependencyConflictBuildTest {

    @Test
    public void whenBuildProject_thenNoDependencyConflictWarnings() throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("mvn", "dependency:tree");
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exitCode = process.waitFor();
        assertTrue(exitCode == 0, "Maven build failed");

        // Check for duplicate dependency warnings in output
        boolean hasDuplicates = output.toLowerCase().contains("duplicate") || output.toLowerCase().contains("conflict");
        assertTrue(!hasDuplicates, "Dependency tree contains duplicates or conflicts:\n" + output);
    }
}