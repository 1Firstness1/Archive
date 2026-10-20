package ru.mirea.archive.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(BackendApplication.class, args);
    }

    private static void loadDotenv() {
        File[] candidates = new File[]{
                new File(".env"),
                new File("backend/.env"),
                new File("../backend/.env")
        };
        for (File f : candidates) {
            if (f.exists() && f.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String val = line.substring(idx + 1).trim();
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                } catch (IOException ignored) {
                }
                break;
            }
        }
    }
}
