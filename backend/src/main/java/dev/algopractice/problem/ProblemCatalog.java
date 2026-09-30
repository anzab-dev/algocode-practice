package dev.algopractice.problem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Reads the problem files bundled under {@code classpath:problems/}. */
@Component
public class ProblemCatalog {

    static final String ROOT = "classpath*:problems/*/problem.yaml";

    private final ObjectMapper json;
    private final YAMLMapper yaml = YAMLMapper.builder().build();

    public ProblemCatalog(ObjectMapper json) {
        this.json = json;
    }

    public List<ProblemDefinition> load() {
        try {
            Resource[] metas = new PathMatchingResourcePatternResolver().getResources(ROOT);
            Map<String, ProblemDefinition> bySlug = new TreeMap<>();
            for (Resource meta : metas) {
                ProblemDefinition def = read(meta);
                bySlug.put(def.slug(), def);
            }
            return List.copyOf(bySlug.values());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ProblemDefinition read(Resource metaResource) throws IOException {
        String path = metaResource.getURL().toString();
        String dir = path.substring(0, path.lastIndexOf('/'));
        String slug = dir.substring(dir.lastIndexOf('/') + 1);
        String metaText = text(metaResource);
        String description = text(metaResource.createRelative("description.md"));
        String reference = text(metaResource.createRelative("Solution.java"));
        String testsText = text(metaResource.createRelative("tests.json"));

        ProblemDefinition.Meta meta = yaml.readValue(metaText, ProblemDefinition.Meta.class);
        List<ProblemDefinition.Test> tests = new ArrayList<>();
        for (JsonNode t : json.readTree(testsText)) {
            tests.add(new ProblemDefinition.Test(t.get("args"), t.get("expected"), t.path("sample").asBoolean(false)));
        }
        validate(slug, meta, tests);
        return new ProblemDefinition(slug, meta, description, reference, List.copyOf(tests),
                sha256(metaText, description, reference, testsText));
    }

    private static void validate(String slug, ProblemDefinition.Meta meta, List<ProblemDefinition.Test> tests) {
        if (meta.title() == null || meta.method() == null || meta.difficulty() == null || meta.starterCode() == null) {
            throw new IllegalStateException("Problem " + slug + " needs title, difficulty, method and starterCode");
        }
        int arity = meta.params() == null ? 0 : meta.params().size();
        for (ProblemDefinition.Test t : tests) {
            if (t.args() == null || !t.args().isArray() || t.args().size() != arity) {
                throw new IllegalStateException("Problem " + slug + " has a test whose args do not match its "
                        + arity + " parameter(s)");
            }
        }
        if (tests.stream().noneMatch(ProblemDefinition.Test::sample)) {
            throw new IllegalStateException("Problem " + slug + " needs at least one sample test");
        }
    }

    private static String text(Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha256(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update(part.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
