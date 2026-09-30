package dev.algopractice.language;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import dev.algopractice.execution.LineMap;
import java.util.List;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** A parsed and attributed compilation unit plus the javac services needed to query it. */
public record JavaAnalysis(
        String source,
        LineMap lines,
        JavacTask task,
        CompilationUnitTree unit,
        Trees trees,
        Elements elements,
        Types types,
        List<Diagnostic<? extends JavaFileObject>> diagnostics) {

    public SourcePositions positions() {
        return trees.getSourcePositions();
    }

    public long start(com.sun.source.tree.Tree tree) {
        return positions().getStartPosition(unit, tree);
    }

    public long end(com.sun.source.tree.Tree tree) {
        return positions().getEndPosition(unit, tree);
    }
}
