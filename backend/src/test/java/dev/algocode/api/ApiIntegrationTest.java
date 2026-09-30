package dev.algocode.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.algocode.problem.ProblemRepository;
import dev.algocode.user.UserResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    ProblemRepository problems;

    private MockHttpServletRequestBuilder as(String handle, MockHttpServletRequestBuilder request) {
        return request.header(UserResolver.HEADER, handle).contentType(MediaType.APPLICATION_JSON);
    }

    private String body(Object value) {
        return json.writeValueAsString(value);
    }

    @Test
    void listsSeededProblems() throws Exception {
        mvc.perform(as("lister", get("/api/problems")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) problems.count())))
                .andExpect(jsonPath("$[0].slug").value("two-sum"))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"));
        mvc.perform(get("/api/problems/two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("twoSum"))
                .andExpect(jsonPath("$.samples", hasSize(3)))
                .andExpect(jsonPath("$.params[0].name").value("nums"))
                .andExpect(jsonPath("$.nextSlug").value("valid-parentheses"));
        mvc.perform(get("/api/problems/nope")).andExpect(status().isNotFound());
    }

    @Test
    void runShowsSampleAndCustomResultsWithReferenceExpectations() throws Exception {
        String wrong = """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """;
        mvc.perform(as("runner", post("/api/problems/two-sum/run"))
                        .content(body(Map.of("code", wrong, "customInputs", java.util.List.of(
                                json.readTree("[[1,5,9],14]"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.cases", hasSize(4)))
                .andExpect(jsonPath("$.cases[0].passed").value(true))
                .andExpect(jsonPath("$.cases[1].passed").value(false))
                .andExpect(jsonPath("$.cases[3].custom").value(true))
                .andExpect(jsonPath("$.cases[3].expected").value("[1,2]"));
    }

    @Test
    void acceptedSubmissionAwardsXpAndAchievements() throws Exception {
        String solution = problems.findBySlug("climbing-stairs").orElseThrow().getReferenceCode();
        mvc.perform(as("solver", post("/api/problems/climbing-stairs/submit")).content(body(Map.of("code", solution))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ACCEPTED"))
                .andExpect(jsonPath("$.passed").value(9))
                .andExpect(jsonPath("$.runtimeMs", greaterThan(0.0)))
                .andExpect(jsonPath("$.xpGained").value(20)) // 10 for an Easy + 2 achievements * 5
                .andExpect(jsonPath("$.newAchievements", hasItem("FIRST_SOLVE")))
                .andExpect(jsonPath("$.newAchievements", hasItem("CLEAN_SHEET")))
                .andExpect(jsonPath("$.runtimeBeats", nullValue()))
                .andExpect(jsonPath("$.streak").value(1));

        mvc.perform(as("solver", get("/api/me")))
                .andExpect(jsonPath("$.profile.solved").value(1))
                .andExpect(jsonPath("$.profile.xp").value(20))
                .andExpect(jsonPath("$.recent[0].verdict").value("ACCEPTED"));
        mvc.perform(as("solver", get("/api/problems")))
                .andExpect(jsonPath("$[?(@.slug == 'climbing-stairs')].status").value("SOLVED"));
        mvc.perform(get("/api/leaderboard"))
                .andExpect(jsonPath("$[?(@.handle == 'solver')].xp").value(20));
        mvc.perform(as("solver", get("/api/problems/climbing-stairs/stats")))
                .andExpect(jsonPath("$.runtimeMs.count", greaterThan(0)));
    }

    @Test
    void failingSubmissionReportsTheFirstFailingCase() throws Exception {
        String wrong = "class Solution { public int climbStairs(int n) { return n; } }";
        mvc.perform(as("learner", post("/api/problems/climbing-stairs/submit")).content(body(Map.of("code", wrong))))
                .andExpect(jsonPath("$.verdict").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.passed").value(3))
                .andExpect(jsonPath("$.failedCase.input").value("[5]"))
                .andExpect(jsonPath("$.failedCase.expected").value("8"))
                .andExpect(jsonPath("$.failedCase.output").value("5"))
                .andExpect(jsonPath("$.message", containsString("Wrong answer on test 4")))
                .andExpect(jsonPath("$.xpGained").value(0));

        String broken = "class Solution { public int climbStairs(int n) { return \"x\"; } }";
        mvc.perform(as("learner", post("/api/problems/climbing-stairs/submit")).content(body(Map.of("code", broken))))
                .andExpect(jsonPath("$.verdict").value("COMPILE_ERROR"))
                .andExpect(jsonPath("$.diagnostics[0].startLine").value(1));
    }

    @Test
    void scratchpadCrudAndRun() throws Exception {
        String code = "public class Main { public static void main(String[] a) { System.out.println(\"hi\"); } }";
        String created = mvc.perform(as("tinker", post("/api/scratchpads"))
                        .content(body(Map.of("title", "idea", "code", code, "stdin", ""))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(as("tinker", put("/api/scratchpads/" + id))
                        .content(body(Map.of("title", "renamed", "code", code, "stdin", "1"))))
                .andExpect(jsonPath("$.title").value("renamed"));
        mvc.perform(as("tinker", get("/api/scratchpads"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as("someone-else", get("/api/scratchpads"))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as("someone-else", delete("/api/scratchpads/" + id))).andExpect(status().isNotFound());

        mvc.perform(as("tinker", post("/api/scratchpads/run")).content(body(Map.of("code", code, "stdin", ""))))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.stdout").value("hi\n"));
        mvc.perform(as("tinker", delete("/api/scratchpads/" + id))).andExpect(status().isNoContent());
    }

    @Test
    void languageEndpoints() throws Exception {
        mvc.perform(post("/api/lang/diagnostics").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("code", "class Solution { int f() { return x; } }", "kind", "SOLUTION"))))
                .andExpect(jsonPath("$.diagnostics[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.diagnostics[0].message", containsString("cannot find symbol")));
        mvc.perform(post("/api/lang/completion").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("code", "class Solution { int f(String s) { return s.le } }",
                                "kind", "SOLUTION", "line", 1, "column", 46))))
                .andExpect(jsonPath("$.items[0].label").value("length"));
        mvc.perform(post("/api/lang/hover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("code", "class Solution { }", "kind", "SOLUTION", "line", 1, "column", 1))))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void rejectsInvalidHandles() throws Exception {
        mvc.perform(as("bad handle!", get("/api/me"))).andExpect(status().isBadRequest());
    }
}
