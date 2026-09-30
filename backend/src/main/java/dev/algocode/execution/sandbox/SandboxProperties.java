package dev.algocode.execution.sandbox;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for where and how user code runs ({@code algocode.sandbox.*}). */
@ConfigurationProperties("algocode.sandbox")
public class SandboxProperties {

    public enum Mode { LOCAL, DOCKER }

    /** LOCAL starts a plain child JVM (development only); DOCKER runs each job in a throwaway container. */
    private Mode mode = Mode.LOCAL;
    /** Java launcher for LOCAL mode; defaults to the backend's own JDK. */
    private String javaBinary = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    private String dockerBinary = "docker";
    private String dockerImage = "eclipse-temurin:21-jre-alpine";
    /** -Xmx of the sandbox JVM. Exceeding it is reported as Memory Limit Exceeded. */
    private int maxHeapMb = 256;
    /** -Xmn; a small young generation makes the peak-heap metric reflect retained data. */
    private int youngGenMb = 16;
    private int containerMemoryMb = 512;
    private double cpus = 1.0;
    private int pidsLimit = 64;
    /** Parallel sandbox runs allowed; further requests wait. */
    private int maxConcurrent = 4;
    /** Added to the problem's time limit to cover JVM start-up before the sandbox is killed. */
    private long startupGraceMs = 5000;
    private int maxOutputBytes = 1024 * 1024;

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getJavaBinary() { return javaBinary; }
    public void setJavaBinary(String javaBinary) { this.javaBinary = javaBinary; }
    public String getDockerBinary() { return dockerBinary; }
    public void setDockerBinary(String dockerBinary) { this.dockerBinary = dockerBinary; }
    public String getDockerImage() { return dockerImage; }
    public void setDockerImage(String dockerImage) { this.dockerImage = dockerImage; }
    public int getMaxHeapMb() { return maxHeapMb; }
    public void setMaxHeapMb(int maxHeapMb) { this.maxHeapMb = maxHeapMb; }
    public int getYoungGenMb() { return youngGenMb; }
    public void setYoungGenMb(int youngGenMb) { this.youngGenMb = youngGenMb; }
    public int getContainerMemoryMb() { return containerMemoryMb; }
    public void setContainerMemoryMb(int containerMemoryMb) { this.containerMemoryMb = containerMemoryMb; }
    public double getCpus() { return cpus; }
    public void setCpus(double cpus) { this.cpus = cpus; }
    public int getPidsLimit() { return pidsLimit; }
    public void setPidsLimit(int pidsLimit) { this.pidsLimit = pidsLimit; }
    public int getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    public long getStartupGraceMs() { return startupGraceMs; }
    public void setStartupGraceMs(long startupGraceMs) { this.startupGraceMs = startupGraceMs; }
    public int getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(int maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }

    /** JVM flags shared by every sandbox mode. */
    public java.util.List<String> jvmFlags() {
        return java.util.List.of(
                "-Xmx" + maxHeapMb + "m",
                "-Xmn" + Math.min(youngGenMb, maxHeapMb / 2) + "m",
                "-XX:+UseSerialGC",
                "-XX:-UsePerfData",
                "-Djava.awt.headless=true",
                "-Dfile.encoding=UTF-8");
    }
}
