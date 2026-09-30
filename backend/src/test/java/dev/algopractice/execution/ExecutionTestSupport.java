package dev.algopractice.execution;

import dev.algopractice.execution.sandbox.LocalProcessSandboxExecutor;
import dev.algopractice.execution.sandbox.SandboxProperties;
import dev.algopractice.telemetry.MicrometerTelemetry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import tools.jackson.databind.json.JsonMapper;

/** Builds an {@link ExecutionService} backed by local child JVMs, without a Spring context. */
public final class ExecutionTestSupport {

    private static ExecutionService shared;

    private ExecutionTestSupport() {
    }

    public static synchronized ExecutionService executionService() {
        if (shared == null) {
            SandboxProperties properties = new SandboxProperties();
            properties.setStartupGraceMs(10_000);
            shared = new ExecutionService(new JavaSourceCompiler(), new HarnessBundle(),
                    new LocalProcessSandboxExecutor(properties), properties,
                    new MicrometerTelemetry(new SimpleMeterRegistry(), ObservationRegistry.NOOP),
                    JsonMapper.builder().build(), 3000);
        }
        return shared;
    }
}
