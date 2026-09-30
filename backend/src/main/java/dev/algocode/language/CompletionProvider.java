package dev.algocode.language;

import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import dev.algocode.execution.LineMap;
import dev.algocode.language.LanguageModels.CompletionItem;
import dev.algocode.language.LanguageModels.CompletionResponse;
import dev.algocode.language.LanguageModels.Range;
import dev.algocode.language.LanguageModels.TextEdit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.springframework.stereotype.Component;

/**
 * Context-aware code completion backed by javac's attribution.
 * <p>
 * A marker identifier is inserted at the caret so that even an incomplete expression such as
 * {@code map.} parses into a member select; the type of the receiver then tells us which
 * members to offer. Plain identifiers get locals, fields, methods and types in scope, JDK
 * types with an auto-import edit, and keywords.
 */
@Component
public class CompletionProvider {

    static final String MARKER = "__algocodeCaret__";
    static final int MAX_ITEMS = 400;

    static final List<String> KEYWORDS = List.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue",
            "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for",
            "if", "implements", "import", "instanceof", "int", "interface", "long", "new", "null", "private",
            "protected", "public", "record", "return", "short", "static", "super", "switch", "this", "throw",
            "throws", "true", "try", "var", "void", "while", "yield");

    private final JavaAnalyzer analyzer;
    private final JdkTypeIndex jdkTypes;

    public CompletionProvider(JavaAnalyzer analyzer, JdkTypeIndex jdkTypes) {
        this.analyzer = analyzer;
        this.jdkTypes = jdkTypes;
    }

    public CompletionResponse complete(String source, int line, int column, String fallbackClassName) {
        LineMap lines = new LineMap(source);
        int offset = lines.offset(line, column);
        int start = SourceContext.identifierStart(source, offset);
        Range replace = new Range(lines.line(start), lines.column(start), line, column);
        if (SourceContext.inCommentOrLiteral(source, offset)) {
            return new CompletionResponse(List.of(), replace, false);
        }
        String prefix = source.substring(start, offset);
        int dot = SourceContext.dotBefore(source, start);
        boolean member = dot >= 0;
        if (member) {
            int receiverStart = SourceContext.identifierStart(source, dot);
            if (receiverStart < dot && Character.isDigit(source.charAt(receiverStart))) {
                return new CompletionResponse(List.of(), replace, false); // "1." starts a number literal
            }
        }

        String patched = source.substring(0, offset) + MARKER + source.substring(offset);
        JavaAnalysis analysis = analyzer.analyze(patched, fallbackClassName);
        TreePath path = findMarker(analysis);
        Collector items = new Collector(prefix);

        if (path != null && path.getLeaf() instanceof MemberSelectTree select) {
            memberCompletions(analysis, path, select, items);
        } else if (!member) {
            if (path != null) {
                scopeCompletions(analysis, path, items);
            }
            typeCompletions(analysis, source, items);
            KEYWORDS.forEach(k -> items.add(k, "Keyword", null, null, null, k, false, 5, List.of()));
        }
        return new CompletionResponse(items.result(), replace, items.truncated());
    }

    // ------------------------------------------------------------------ member access: expr.<caret>

    private void memberCompletions(JavaAnalysis a, TreePath path, MemberSelectTree select, Collector items) {
        Trees trees = a.trees();
        TreePath receiverPath = new TreePath(path, select.getExpression());
        Element receiver = trees.getElement(receiverPath);
        TypeMirror type = trees.getTypeMirror(receiverPath);
        Scope scope = trees.getScope(path);

        if (receiver instanceof PackageElement pkg) {
            for (Element e : pkg.getEnclosedElements()) {
                if (e instanceof TypeElement t && t.getModifiers().contains(Modifier.PUBLIC)) {
                    addType(t, items, 1, List.of());
                }
            }
            return;
        }
        boolean staticOnly = receiver instanceof TypeElement;
        if (type instanceof TypeVariable tv) {
            type = tv.getUpperBound();
        }
        if (type instanceof ArrayType) {
            items.add("length", "Field", null, "int", null, "length", false, 1, List.of());
            items.add("clone", "Method", "()", type.toString(), null, "clone()", false, 2, List.of());
            type = a.elements().getTypeElement("java.lang.Object").asType();
        }
        if (!(type instanceof DeclaredType declared) || !(declared.asElement() instanceof TypeElement owner)) {
            return;
        }
        if (staticOnly) {
            items.add("class", "Keyword", null, "Class<" + owner.getSimpleName() + ">", null, "class", false, 4,
                    List.of());
        }
        for (Element m : a.elements().getAllMembers(owner)) {
            boolean isStatic = m.getModifiers().contains(Modifier.STATIC);
            if (m.getKind() == ElementKind.CONSTRUCTOR || m.getKind() == ElementKind.STATIC_INIT
                    || m.getKind() == ElementKind.INSTANCE_INIT || (staticOnly && !isStatic && !isType(m))
                    || (!staticOnly && (isStatic || isType(m)))) {
                continue;
            }
            if (!accessible(trees, scope, m, declared)) {
                continue;
            }
            int priority = m.getEnclosingElement().equals(owner) ? 1
                    : isObjectMember(m) ? 3 : 2;
            addElement(a, m, declared, items, priority);
        }
    }

    // ------------------------------------------------------------------ identifiers: <caret>

    private void scopeCompletions(JavaAnalysis a, TreePath path, Collector items) {
        Trees trees = a.trees();
        Scope scope = trees.getScope(path);
        boolean staticContext = scope.getEnclosingMethod() != null
                && scope.getEnclosingMethod().getModifiers().contains(Modifier.STATIC);
        for (Scope s = scope; s != null; s = s.getEnclosingScope()) {
            for (Element e : s.getLocalElements()) {
                if (e.getSimpleName().toString().contains(MARKER)) {
                    continue;
                }
                switch (e.getKind()) {
                    case LOCAL_VARIABLE, PARAMETER, EXCEPTION_PARAMETER, RESOURCE_VARIABLE, BINDING_VARIABLE ->
                            addElement(a, e, null, items, 0);
                    case CLASS, INTERFACE, ENUM, RECORD, ANNOTATION_TYPE -> addType((TypeElement) e, items, 4, List.of());
                    default -> { }
                }
            }
        }
        for (TypeElement cls = scope.getEnclosingClass(); cls != null; cls = enclosingType(cls)) {
            DeclaredType declared = (DeclaredType) cls.asType();
            for (Element m : a.elements().getAllMembers(cls)) {
                if (m.getKind() == ElementKind.CONSTRUCTOR || m.getKind() == ElementKind.STATIC_INIT
                        || m.getKind() == ElementKind.INSTANCE_INIT) {
                    continue;
                }
                if (staticContext && cls.equals(scope.getEnclosingClass())
                        && !m.getModifiers().contains(Modifier.STATIC) && !isType(m)) {
                    continue;
                }
                if (!accessible(trees, scope, m, declared)) {
                    continue;
                }
                if (isType(m)) {
                    addType((TypeElement) m, items, 4, List.of());
                } else {
                    int priority = m.getEnclosingElement().equals(cls) ? 1 : isObjectMember(m) ? 3 : 2;
                    addElement(a, m, declared, items, priority);
                }
            }
        }
    }

    private void typeCompletions(JavaAnalysis a, String originalSource, Collector items) {
        if (items.prefix.isEmpty()) {
            return; // listing every JDK type on an empty prefix is noise
        }
        Set<String> imported = new HashSet<>();
        Set<String> starImports = new HashSet<>();
        for (ImportTree imp : a.unit().getImports()) {
            String name = imp.getQualifiedIdentifier().toString();
            if (name.endsWith(".*")) {
                starImports.add(name.substring(0, name.length() - 2));
            } else {
                imported.add(name);
            }
        }
        for (JdkTypeIndex.Entry entry : jdkTypes.entries()) {
            if (!items.matches(entry.simpleName())) {
                continue;
            }
            boolean visible = !entry.needsImport() || imported.contains(entry.qualifiedName())
                    || starImports.contains(entry.packageName());
            List<TextEdit> edits = visible ? List.of() : List.of(importEdit(originalSource, entry.qualifiedName()));
            TypeElement type = a.elements().getTypeElement(entry.qualifiedName());
            if (type != null) {
                addType(type, items, visible ? 4 : 6, edits);
            }
        }
    }

    /** Inserts {@code import qualifiedName;} after the last import, the package clause, or at the top. */
    static TextEdit importEdit(String source, String qualifiedName) {
        LineMap lines = new LineMap(source);
        int insertLine = 1;
        boolean afterPackage = false;
        String[] rows = source.split("\n", -1);
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i].strip();
            if (row.startsWith("import ")) {
                insertLine = i + 2;
                afterPackage = false;
            } else if (row.startsWith("package ")) {
                insertLine = i + 2;
                afterPackage = true;
            } else if (!row.isEmpty() && !row.startsWith("//") && !row.startsWith("/*") && !row.startsWith("*")) {
                break;
            }
        }
        String text = (afterPackage ? "\n" : "") + "import " + qualifiedName + ";\n";
        if (insertLine == 1 && !source.startsWith("import ")) {
            text = text + "\n";
        }
        int offset = lines.offset(insertLine, 1);
        return new TextEdit(new Range(lines.line(offset), lines.column(offset), lines.line(offset),
                lines.column(offset)), text);
    }

    // ------------------------------------------------------------------ item building

    private void addElement(JavaAnalysis a, Element e, DeclaredType site, Collector items, int priority) {
        String name = e.getSimpleName().toString();
        String owner = e.getEnclosingElement() instanceof TypeElement t ? t.getSimpleName().toString() : null;
        TypeMirror type = e.asType();
        if (site != null) {
            try {
                type = a.types().asMemberOf(site, e);
            } catch (IllegalArgumentException ignored) {
                // not a member of this site; keep the declared type
            }
        }
        if (e instanceof ExecutableElement method && type instanceof ExecutableType exec) {
            List<String> names = parameterNames(method, exec);
            StringBuilder signature = new StringBuilder("(");
            StringBuilder snippet = new StringBuilder(name).append('(');
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    signature.append(", ");
                    snippet.append(", ");
                }
                String paramType = shortType(exec.getParameterTypes().get(i));
                if (method.isVarArgs() && i == names.size() - 1 && paramType.endsWith("[]")) {
                    paramType = paramType.substring(0, paramType.length() - 2) + "...";
                }
                signature.append(paramType).append(' ').append(names.get(i));
                snippet.append("${").append(i + 1).append(':').append(names.get(i)).append('}');
            }
            signature.append(')');
            snippet.append(names.isEmpty() ? ")" : ")$0");
            items.add(name, "Method", signature.toString(), shortType(exec.getReturnType()), owner,
                    snippet.toString(), !names.isEmpty(), priority, List.of());
        } else if (e instanceof VariableElement) {
            String kind = switch (e.getKind()) {
                case ENUM_CONSTANT -> "EnumMember";
                case FIELD -> e.getModifiers().contains(Modifier.STATIC) && e.getModifiers().contains(Modifier.FINAL)
                        ? "Constant" : "Field";
                default -> "Variable";
            };
            items.add(name, kind, null, shortType(type), owner, name, false, priority, List.of());
        }
    }

    private static void addType(TypeElement t, Collector items, int priority, List<TextEdit> edits) {
        String kind = switch (t.getKind()) {
            case INTERFACE, ANNOTATION_TYPE -> "Interface";
            case ENUM -> "Enum";
            case RECORD -> "Struct";
            default -> "Class";
        };
        String pkg = t.getQualifiedName().toString();
        int dot = pkg.lastIndexOf('.');
        String name = t.getSimpleName().toString();
        items.add(name, kind, null, dot > 0 ? pkg.substring(0, dot) : null, null, name, false, priority, edits);
    }

    private static List<String> parameterNames(ExecutableElement method, ExecutableType type) {
        List<String> names = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int i = 0; i < method.getParameters().size(); i++) {
            String name = method.getParameters().get(i).getSimpleName().toString();
            if (name.matches("arg\\d+")) {
                name = nameForType(method.getParameters().get(i).asType());
            }
            String unique = name;
            for (int n = 2; !used.add(unique); n++) {
                unique = name + n;
            }
            names.add(unique);
        }
        return names;
    }

    /** JDK signatures from ct.sym carry no parameter names, so derive one from the type. */
    static String nameForType(TypeMirror type) {
        return switch (type.getKind()) {
            case INT, SHORT, BYTE -> "i";
            case LONG -> "l";
            case CHAR -> "c";
            case BOOLEAN -> "b";
            case DOUBLE, FLOAT -> "d";
            case ARRAY -> nameForType(((ArrayType) type).getComponentType()) + "s";
            case TYPEVAR -> type.toString().toLowerCase(Locale.ROOT);
            case DECLARED -> {
                String simple = ((DeclaredType) type).asElement().getSimpleName().toString();
                yield switch (simple) {
                    case "Object" -> "o";
                    case "String", "CharSequence" -> "s";
                    default -> Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
                };
            }
            default -> "arg";
        };
    }

    /** {@code java.util.List<java.lang.String>} becomes {@code List<String>}. */
    static String shortType(TypeMirror type) {
        return type.toString().replaceAll("\\b(?:[a-z_][\\w]*\\.)+([A-Z])", "$1");
    }

    private static boolean accessible(Trees trees, Scope scope, Element member, DeclaredType site) {
        try {
            return trees.isAccessible(scope, member, site);
        } catch (RuntimeException e) {
            return !member.getModifiers().contains(Modifier.PRIVATE);
        }
    }

    private static boolean isType(Element e) {
        return e instanceof TypeElement;
    }

    private static boolean isObjectMember(Element e) {
        return e.getEnclosingElement() instanceof TypeElement t
                && t.getQualifiedName().contentEquals("java.lang.Object");
    }

    private static TypeElement enclosingType(TypeElement type) {
        Element outer = type.getEnclosingElement();
        while (outer != null && !(outer instanceof TypeElement)) {
            outer = outer.getEnclosingElement();
        }
        return (TypeElement) outer;
    }

    private static TreePath findMarker(JavaAnalysis analysis) {
        TreePath[] found = new TreePath[1];
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitErroneous(ErroneousTree node, Void unused) {
                // Incomplete statements such as "map.<caret>" become erroneous trees; javac still
                // attributes their children, so look inside.
                return scan(node.getErrorTrees(), unused);
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void unused) {
                if (found[0] == null && node.getIdentifier().toString().contains(MARKER)) {
                    found[0] = getCurrentPath();
                    return null;
                }
                return super.visitMemberSelect(node, unused);
            }

            @Override
            public Void visitIdentifier(IdentifierTree node, Void unused) {
                if (found[0] == null && node.getName().toString().contains(MARKER)) {
                    found[0] = getCurrentPath();
                }
                return null;
            }
        }.scan(analysis.unit(), null);
        return found[0];
    }

    /** Collects, de-duplicates and filters items against the typed prefix. */
    static final class Collector {
        final String prefix;
        private final Map<String, CompletionItem> items = new LinkedHashMap<>();
        private boolean truncated;

        Collector(String prefix) {
            this.prefix = prefix;
        }

        boolean matches(String name) {
            return fuzzyMatch(prefix, name);
        }

        void add(String label, String kind, String signature, String type, String owner, String insertText,
                 boolean snippet, int priority, List<TextEdit> edits) {
            if (!matches(label)) {
                return;
            }
            String key = label + '|' + kind + '|' + signature;
            if (items.containsKey(key)) {
                return;
            }
            if (items.size() >= MAX_ITEMS) {
                truncated = true;
                return;
            }
            String sort = priority + (label.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)) ? "0" : "1")
                    + label.toLowerCase(Locale.ROOT);
            items.put(key, new CompletionItem(label, kind, signature, type, owner, insertText, snippet, sort, edits));
        }

        List<CompletionItem> result() {
            return List.copyOf(items.values());
        }

        boolean truncated() {
            return truncated;
        }
    }

    /**
     * IntelliJ-style matching: the first letter must match, then the rest of the prefix may
     * skip ahead, so {@code hm} finds {@code HashMap} and {@code gOD} finds {@code getOrDefault}.
     */
    static boolean fuzzyMatch(String prefix, String name) {
        if (prefix.isEmpty()) {
            return true;
        }
        if (name.isEmpty() || Character.toLowerCase(prefix.charAt(0)) != Character.toLowerCase(name.charAt(0))) {
            return false;
        }
        int j = 1;
        for (int i = 1; i < prefix.length(); i++) {
            char p = Character.toLowerCase(prefix.charAt(i));
            while (j < name.length() && Character.toLowerCase(name.charAt(j)) != p) {
                j++;
            }
            if (j == name.length()) {
                return false;
            }
            j++;
        }
        return true;
    }
}
