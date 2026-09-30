package dev.algocode.language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.springframework.stereotype.Component;

/**
 * Public JDK types that practice code commonly needs, so completion can offer {@code HashMap}
 * before it is imported and add the import, the way IntelliJ does.
 */
@Component
public class JdkTypeIndex {

    static final List<String> PACKAGES = List.of(
            "java.lang", "java.util", "java.util.function", "java.util.stream", "java.util.concurrent",
            "java.util.concurrent.atomic", "java.util.regex", "java.math", "java.io", "java.nio.file",
            "java.time", "java.text");

    public record Entry(String simpleName, String qualifiedName, String packageName, ElementKind kind) {
        public boolean needsImport() {
            return !packageName.equals("java.lang");
        }
    }

    private final JavaAnalyzer analyzer;
    private volatile List<Entry> entries;

    public JdkTypeIndex(JavaAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public List<Entry> entries() {
        List<Entry> local = entries;
        if (local == null) {
            synchronized (this) {
                if (entries == null) {
                    entries = build();
                }
                local = entries;
            }
        }
        return local;
    }

    private List<Entry> build() {
        JavaAnalysis analysis = analyzer.analyze("class Index {}", "Index");
        List<Entry> out = new ArrayList<>();
        for (String pkg : PACKAGES) {
            PackageElement element = analysis.elements().getPackageElement(pkg);
            if (element == null) {
                continue;
            }
            for (Element e : element.getEnclosedElements()) {
                if (e instanceof TypeElement type && type.getModifiers().contains(Modifier.PUBLIC)
                        && !type.getSimpleName().toString().contains("$")) {
                    out.add(new Entry(type.getSimpleName().toString(), type.getQualifiedName().toString(), pkg,
                            type.getKind()));
                }
            }
        }
        out.sort(Comparator.comparing(Entry::simpleName));
        return List.copyOf(out);
    }
}
