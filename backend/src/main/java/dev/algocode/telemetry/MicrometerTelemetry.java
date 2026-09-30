package dev.algocode.telemetry;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Default {@link AlgoTelemetry}: Micrometer meters plus observations (spans once a tracer is on the class path). */
@Component
public class MicrometerTelemetry implements AlgoTelemetry {

    private final MeterRegistry meters;
    private final ObservationRegistry observations;

    public MicrometerTelemetry(MeterRegistry meters, ObservationRegistry observations) {
        this.meters = meters;
        this.observations = observations;
    }

    @Override
    public <T> T observe(String name, Map<String, String> tags, Supplier<T> work) {
        Observation observation = Observation.createNotStarted(name, observations);
        tags.forEach(observation::lowCardinalityKeyValue);
        return observation.observe(work);
    }

    @Override
    public void executionFinished(ExecutionEvent event) {
        Timer.builder("algocode.execution.wall")
                .description("Wall time of a sandbox run including JVM start-up")
                .tag("kind", event.kind())
                .tag("sandbox", event.sandbox())
                .tag("status", event.status())
                .register(meters)
                .record(Duration.ofMillis(event.wallMillis()));
        Timer.builder("algocode.compile")
                .tag("kind", event.kind())
                .register(meters)
                .record(event.compileNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void submissionJudged(SubmissionEvent event) {
        meters.counter("algocode.submissions",
                "problem", event.problemSlug(),
                "difficulty", event.difficulty(),
                "verdict", event.verdict()).increment();
        if (event.runtimeMs() != null) {
            DistributionSummary.builder("algocode.submission.runtime")
                    .baseUnit("milliseconds")
                    .tag("problem", event.problemSlug())
                    .register(meters)
                    .record(event.runtimeMs());
        }
        if (event.memoryBytes() != null) {
            DistributionSummary.builder("algocode.submission.memory")
                    .baseUnit("bytes")
                    .tag("problem", event.problemSlug())
                    .register(meters)
                    .record(event.memoryBytes());
        }
    }

    @Override
    public void languageRequest(String kind, long durationNanos) {
        Timer.builder("algocode.language")
                .tag("kind", kind)
                .register(meters)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
