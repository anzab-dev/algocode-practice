package dev.algopractice.execution;

import algopractice.harness.Runner;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The bytecode of the {@code algopractice.harness} classes, shipped into every sandbox next to the
 * user's classes. The harness is compiled with the backend, so it is read from our own class path.
 */
@Component
public class HarnessBundle {

    public static final String MAIN_CLASS = Runner.class.getName();

    private final Map<String, byte[]> classFiles;

    public HarnessBundle() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        String pkg = Runner.class.getPackageName();
        for (String simple : new String[] {"Runner", "Json", "ArgConverter", "CaptureStream"}) {
            try {
                queue.add(Class.forName(pkg + "." + simple));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Harness class missing: " + simple, e);
            }
        }
        while (!queue.isEmpty()) {
            Class<?> cls = queue.poll();
            files.put(cls.getName(), read(cls));
            queue.addAll(java.util.List.of(cls.getDeclaredClasses()));
        }
        this.classFiles = Map.copyOf(files);
    }

    /** Binary class name to bytecode. */
    public Map<String, byte[]> classFiles() {
        return classFiles;
    }

    private static byte[] read(Class<?> cls) {
        String resource = "/" + cls.getName().replace('.', '/') + ".class";
        try (InputStream in = cls.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Cannot read " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
