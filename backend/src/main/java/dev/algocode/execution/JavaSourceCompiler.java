package dev.algocode.execution;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.springframework.stereotype.Component;

/**
 * In-memory javac front end. Compiling happens in the backend JVM (javac does not run user
 * code when annotation processing is disabled); executing happens in the sandbox.
 */
@Component
public class JavaSourceCompiler {

    public static final int MAX_SOURCE_CHARS = 64 * 1024;
    static final List<String> OPTIONS = List.of(
            "--release", "21", "-proc:none", "-g", "-encoding", "UTF-8",
            "-Xlint:all,-serial,-processing,-classfile,-path", "-Xmaxerrs", "100");

    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "(?m)^\\s*public\\s+(?:(?:final|abstract|sealed|non-sealed|strictfp)\\s+)*(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");

    private final JavaCompiler compiler;

    public JavaSourceCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler: run the backend on a JDK, not a JRE");
        }
    }

    public JavaCompiler compiler() {
        return compiler;
    }

    /** File name javac expects: the public top-level type if there is one, else the fallback. */
    public static String fileNameFor(String source, String fallbackClassName) {
        Matcher m = PUBLIC_TYPE.matcher(source);
        return (m.find() ? m.group(1) : fallbackClassName) + ".java";
    }

    public static JavaFileObject sourceFile(String fileName, String source) {
        return new SimpleJavaFileObject(URI.create("string:///" + fileName), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
    }

    public CompilationResult compile(String source, String fallbackClassName) {
        if (source.length() > MAX_SOURCE_CHARS) {
            SourceDiagnostic tooLong = new SourceDiagnostic(SourceDiagnostic.Severity.ERROR,
                    "Source is too long (max " + MAX_SOURCE_CHARS + " characters)", "size", 1, 1, 1, 2);
            return new CompilationResult(false, List.of(tooLong), Map.of(), List.of());
        }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager standard = compiler.getStandardFileManager(collector, null, StandardCharsets.UTF_8);
        MemoryFileManager fileManager = new MemoryFileManager(standard);
        JavaFileObject file = sourceFile(fileNameFor(source, fallbackClassName), source);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, collector, OPTIONS, null, List.of(file));

        List<String> mainClasses = new ArrayList<>();
        task.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.ANALYZE && e.getTypeElement() != null
                        && hasMainMethod(e.getTypeElement())) {
                    mainClasses.add(e.getTypeElement().getQualifiedName().toString());
                }
            }
        });
        boolean success = Boolean.TRUE.equals(task.call());
        try {
            fileManager.close();
        } catch (IOException ignored) {
            // in-memory; nothing to release
        }
        List<SourceDiagnostic> diagnostics = toDiagnostics(collector.getDiagnostics(), new LineMap(source));
        return new CompilationResult(success, diagnostics, success ? fileManager.classFiles() : Map.of(),
                List.copyOf(mainClasses));
    }

    static boolean hasMainMethod(TypeElement type) {
        for (ExecutableElement m : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (m.getSimpleName().contentEquals("main")
                    && m.getModifiers().contains(Modifier.STATIC)
                    && m.getModifiers().contains(Modifier.PUBLIC)
                    && m.getParameters().size() == 1
                    && m.getParameters().getFirst().asType() instanceof ArrayType at
                    && at.getComponentType().getKind() == TypeKind.DECLARED
                    && at.getComponentType().toString().equals("java.lang.String")) {
                return true;
            }
        }
        return false;
    }

    public static List<SourceDiagnostic> toDiagnostics(List<Diagnostic<? extends JavaFileObject>> raw, LineMap lines) {
        List<SourceDiagnostic> out = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : raw) {
            SourceDiagnostic.Severity severity = switch (d.getKind()) {
                case ERROR -> SourceDiagnostic.Severity.ERROR;
                case WARNING, MANDATORY_WARNING -> SourceDiagnostic.Severity.WARNING;
                default -> SourceDiagnostic.Severity.INFO;
            };
            if (d.getSource() == null && severity != SourceDiagnostic.Severity.ERROR) {
                continue; // global notes such as "Some input files use unchecked operations"
            }
            long start = d.getStartPosition() >= 0 ? d.getStartPosition() : d.getPosition();
            long end = d.getEndPosition() >= 0 ? d.getEndPosition() : start;
            if (start < 0) {
                start = 0;
                end = 0;
            }
            if (end <= start) {
                end = start + 1;
            }
            out.add(new SourceDiagnostic(severity, d.getMessage(null), d.getCode(),
                    lines.line(start), lines.column(start), lines.line(end), lines.column(end)));
        }
        return out;
    }

    /** Keeps compiled classes in memory instead of writing them to disk. */
    static final class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ByteArrayOutputStream> outputs = new LinkedHashMap<>();

        MemoryFileManager(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            outputs.put(className, buffer);
            return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                @Override
                public OutputStream openOutputStream() {
                    return buffer;
                }
            };
        }

        Map<String, byte[]> classFiles() {
            Map<String, byte[]> result = new LinkedHashMap<>();
            outputs.forEach((name, bytes) -> result.put(name, bytes.toByteArray()));
            return result;
        }
    }
}
