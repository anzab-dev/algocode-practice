package dev.algopractice.language;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import dev.algopractice.execution.JavaSourceCompiler;
import dev.algopractice.execution.LineMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import org.springframework.stereotype.Component;

/** Parses and attributes a source file without generating bytecode. */
@Component
public class JavaAnalyzer {

    static final List<String> OPTIONS = List.of(
            "--release", "21", "-proc:none", "-Xlint:all,-serial,-processing,-classfile,-path",
            "-XDshould-stop.ifError=FLOW", "-Xmaxerrs", "200");

    private final JavaSourceCompiler compiler;

    public JavaAnalyzer(JavaSourceCompiler compiler) {
        this.compiler = compiler;
    }

    public JavaAnalysis analyze(String source, String fallbackClassName) {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager =
                compiler.compiler().getStandardFileManager(collector, null, StandardCharsets.UTF_8);
        JavaFileObject file = JavaSourceCompiler.sourceFile(
                JavaSourceCompiler.fileNameFor(source, fallbackClassName), source);
        JavacTask task = (JavacTask) compiler.compiler().getTask(null, fileManager, collector, OPTIONS, null,
                List.of(file));
        try {
            CompilationUnitTree unit = task.parse().iterator().next();
            task.analyze();
            return new JavaAnalysis(source, new LineMap(source), task, unit, Trees.instance(task),
                    task.getElements(), task.getTypes(), List.copyOf(collector.getDiagnostics()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
