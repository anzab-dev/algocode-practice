package dev.algopractice.language;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import dev.algopractice.execution.LineMap;
import dev.algopractice.language.LanguageModels.HoverResponse;
import dev.algopractice.language.LanguageModels.Range;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.springframework.stereotype.Component;

/** Quick documentation on hover: the declaration of the symbol under the cursor. */
@Component
public class HoverProvider {

    private final JavaAnalyzer analyzer;

    public HoverProvider(JavaAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public HoverResponse hover(String source, int line, int column, String fallbackClassName) {
        LineMap lines = new LineMap(source);
        int offset = lines.offset(line, column);
        JavaAnalysis a = analyzer.analyze(source, fallbackClassName);
        TreePath path = deepestAt(a, offset);
        if (path == null) {
            return null;
        }
        Element element = a.trees().getElement(path);
        if (element == null) {
            return null;
        }
        long start = a.start(path.getLeaf());
        long end = a.end(path.getLeaf());
        if (path.getLeaf() instanceof MemberSelectTree select) {
            start = end - select.getIdentifier().length();
        }
        String doc = a.elements().getDocComment(element);
        StringBuilder md = new StringBuilder("```java\n").append(declaration(element)).append("\n```");
        String where = location(element);
        if (where != null) {
            md.append("\n\n").append(where);
        }
        if (doc != null && !doc.isBlank()) {
            md.append("\n\n").append(doc.strip());
        }
        return new HoverResponse(md.toString(), new Range(lines.line(start), lines.column(start),
                lines.line(end), lines.column(end)));
    }

    private static TreePath deepestAt(JavaAnalysis a, int offset) {
        TreePath[] best = new TreePath[1];
        new TreePathScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void unused) {
                if (tree == null) {
                    return null;
                }
                long start = a.start(tree);
                long end = a.end(tree);
                if (start < 0 || end < 0 || offset < start || offset > end) {
                    return null;
                }
                return super.scan(tree, unused);
            }

            @Override
            public Void visitErroneous(ErroneousTree node, Void unused) {
                return scan(node.getErrorTrees(), unused);
            }

            @Override
            public Void visitIdentifier(IdentifierTree node, Void unused) {
                best[0] = getCurrentPath();
                return null;
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void unused) {
                best[0] = getCurrentPath();
                return super.visitMemberSelect(node, unused);
            }

            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                best[0] = getCurrentPath();
                return super.visitVariable(node, unused);
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                best[0] = getCurrentPath();
                return super.visitMethod(node, unused);
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                best[0] = getCurrentPath();
                return super.visitClass(node, unused);
            }

            @Override
            public Void visitNewClass(NewClassTree node, Void unused) {
                best[0] = getCurrentPath();
                return super.visitNewClass(node, unused);
            }
        }.scan(new TreePath(a.unit()), null);
        return best[0];
    }

    static String declaration(Element e) {
        String mods = e.getModifiers().stream()
                .filter(m -> m != Modifier.DEFAULT || e instanceof ExecutableElement)
                .map(Modifier::toString)
                .collect(Collectors.joining(" "));
        String prefix = mods.isEmpty() ? "" : mods + " ";
        if (e instanceof ExecutableElement m) {
            String params = m.getParameters().stream()
                    .map(p -> CompletionProvider.shortType(p.asType()) + " " + p.getSimpleName())
                    .collect(Collectors.joining(", "));
            String name = m.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR
                    ? m.getEnclosingElement().getSimpleName().toString()
                    : CompletionProvider.shortType(m.getReturnType()) + " " + m.getSimpleName();
            return prefix + name + "(" + params + ")";
        }
        if (e instanceof VariableElement v) {
            return prefix + CompletionProvider.shortType(v.asType()) + " " + v.getSimpleName();
        }
        if (e instanceof TypeElement t) {
            String kind = switch (t.getKind()) {
                case INTERFACE -> "interface";
                case ENUM -> "enum";
                case RECORD -> "record";
                case ANNOTATION_TYPE -> "@interface";
                default -> "class";
            };
            return prefix + kind + " " + t.getQualifiedName();
        }
        return e.toString();
    }

    private static String location(Element e) {
        return switch (e.getKind()) {
            case LOCAL_VARIABLE, RESOURCE_VARIABLE, BINDING_VARIABLE -> "Local variable";
            case PARAMETER -> "Parameter";
            case EXCEPTION_PARAMETER -> "Exception parameter";
            default -> e.getEnclosingElement() instanceof TypeElement owner
                    ? "Declared in `" + owner.getQualifiedName() + "`" : null;
        };
    }
}
