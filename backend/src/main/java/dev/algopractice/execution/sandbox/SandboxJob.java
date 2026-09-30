package dev.algopractice.execution.sandbox;

import java.time.Duration;
import java.util.Map;

/**
 * Everything a sandbox needs to run one harness invocation.
 *
 * @param classFiles  binary class name to bytecode (user classes plus harness)
 * @param jobJson     the harness job description, written to {@code job.json}
 * @param mainClass   harness entry point
 * @param hardTimeout wall-clock limit after which the sandbox is killed
 */
public record SandboxJob(Map<String, byte[]> classFiles, String jobJson, String mainClass, Duration hardTimeout) {

    public static final String JOB_FILE = "job.json";
}
