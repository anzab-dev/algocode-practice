package dev.algocode.language;

import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import dev.algocode.execution.JavaSourceCompiler;
import dev.algocode.execution.SourceDiagnostic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.springframework.stereotype.Component;

/**
 * Compiler errors and lint warnings from javac, plus a few IntelliJ-style inspections javac
 * does not offer: unused local variables and unused imports.
 */
@Component
public class DiagnosticsProvider {

    public static final String UNUSED = "algocode.unused";

    public List<SourceDiagnostic> diagnostics(JavaAnalysis analysis) {
        List<SourceDiagnostic> out = new ArrayList<>(JavaSourceCompiler.toDiagnostics(analysis.diagnostics(),
                analysis.lines()));
        boolean hasErrors = out.stream().anyMatch(d -> d.severity() == SourceDiagnostic.Severity.ERROR);
        UsageScanner usage = new UsageScanner(analysis);
        usage.scan(analysis.unit(), null);

        for (Map.Entry<Element, VariableTree> local : usage.locals.entrySet()) {
            if (!usage.referenced.contains(local.getKey())) {
                VariableTree tree = local.getValue();
                long start = namePosition(analysis, tree);
                out.add(new SourceDiagnostic(SourceDiagnostic.Severity.WARNING,
                        "Variable '" + tree.getName() + "' is never used", UNUSED,
                        analysis.lines().line(start), analysis.lines().column(start),
                        analysis.lines().line(start + tree.getName().length()),
                        analysis.lines().column(start + tree.getName().length())));
            }
        }
        // With errors present, attribution is incomplete and imports may look unused when they are not.
        if (!hasErrors) {
            for (ImportTree imp : analysis.unit().getImports()) {
                if (imp.isStatic() || !(imp.getQualifiedIdentifier() instanceof MemberSelectTree select)
                        || select.getIdentifier().contentEquals("*")) {
                    continue;
                }
                if (!usage.referencedTypes.contains(select.toString())) {
                    long start = analysis.start(imp);
                    long end = analysis.end(imp);
                    out.add(new SourceDiagnostic(SourceDiagnostic.Severity.WARNING,
                            "Unused import statement", UNUSED,
                            analysis.lines().line(start), analysis.lines().column(start),
                            analysis.lines().line(end), analysis.lines().column(end)));
                }
            }
        }
        return out;
    }

    /** javac positions a variable at its type; IntelliJ highlights the name, so find it. */
    private static long namePosition(JavaAnalysis analysis, VariableTree tree) {
        long start = Math.max(0, analysis.start(tree));
        long typeEnd = tree.getType() == null ? -1 : analysis.end(tree.getType());
        int from = (int) Math.max(start, typeEnd);
        String source = analysis.source();
        String name = tree.getName().toString();
        for (int idx = source.indexOf(name, from); idx >= 0; idx = source.indexOf(name, idx + 1)) {
            boolean before = idx == 0 || !Character.isJavaIdentifierPart(source.charAt(idx - 1));
            int after = idx + name.length();
            if (before && (after >= source.length() || !Character.isJavaIdentifierPart(source.charAt(after)))) {
                return idx;
            }
        }
        return start;
    }

    private static final class UsageScanner extends TreePathScanner<Void, Void> {
        private final JavaAnalysis analysis;
        final Map<Element, VariableTree> locals = new LinkedHashMap<>();
        final Set<Element> referenced = new HashSet<>();
        final Set<String> referencedTypes = new HashSet<>();

        UsageScanner(JavaAnalysis analysis) {
            this.analysis = analysis;
        }

        @Override
        public Void visitErroneous(ErroneousTree node, Void unused) {
            return scan(node.getErrorTrees(), unused);
        }

        @Override
        public Void visitImport(ImportTree node, Void unused) {
            return null; // an import does not count as a use of itself
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            Element element = analysis.trees().getElement(getCurrentPath());
            if (element != null && element.getKind() == ElementKind.LOCAL_VARIABLE
                    && !node.getName().contentEquals("_")) {
                locals.put(element, node);
            }
            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void unused) {
            record(getCurrentPath());
            return super.visitIdentifier(node, unused);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void unused) {
            record(getCurrentPath());
            return super.visitMemberSelect(node, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree node, Void unused) {
            record(getCurrentPath());
            return super.visitMemberReference(node, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void unused) {
            record(getCurrentPath());
            return super.visitNewClass(node, unused);
        }

        private void record(TreePath path) {
            Element element = analysis.trees().getElement(path);
            if (element == null) {
                return;
            }
            referenced.add(element);
            Element type = element;
            while (type != null && !(type instanceof TypeElement)) {
                type = type.getEnclosingElement();
            }
            while (type instanceof TypeElement te) {
                referencedTypes.add(te.getQualifiedName().toString());
                type = te.getEnclosingElement() instanceof TypeElement outer ? outer : null;
            }
            Tree leaf = path.getLeaf();
            if (leaf instanceof NewClassTree && element.getEnclosingElement() instanceof TypeElement owner) {
                referencedTypes.add(owner.getQualifiedName().toString());
            }
        }
    }
}
