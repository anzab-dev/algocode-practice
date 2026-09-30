package dev.algopractice.execution.sandbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SandboxConfiguration {

    @Bean
    SandboxExecutor sandboxExecutor(SandboxProperties properties) {
        return switch (properties.getMode()) {
            case LOCAL -> new LocalProcessSandboxExecutor(properties);
            case DOCKER -> new DockerSandboxExecutor(properties);
        };
    }
}
