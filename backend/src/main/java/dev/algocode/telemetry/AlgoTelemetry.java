package dev.algocode.telemetry;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The single seam between the application and whatever collects its telemetry.
 * <p>
 * Domain code reports what happened in domain terms and never touches a metrics or tracing
 * library directly. The default implementation, {@link MicrometerTelemetry}, maps these calls
 * onto Micrometer meters and observations, which Spring Boot can export to OpenTelemetry (OTLP),
 * Prometheus and others purely through configuration. Swap the bean to send events elsewhere.
 */
public interface AlgoTelemetry {

    /** Wraps a unit of work so it is timed and, when tracing is enabled, becomes a span. */
    <T> T observe(String name, Map<String, String> tags, Supplier<T> work);

    /** A sandbox run finished (either a judge run or a scratchpad run). */
    void executionFinished(ExecutionEvent event);

    /** A submission received its final verdict. */
    void submissionJudged(SubmissionEvent event);

    /** A language-service request (diagnostics, completion, hover) was served. */
    void languageRequest(String kind, long durationNanos);

    record ExecutionEvent(String kind, String sandbox, String status, long compileNanos, long wallMillis) {
    }

    record SubmissionEvent(String problemSlug, String difficulty, String verdict, Double runtimeMs, Long memoryBytes) {
    }
}
