package dev.algopractice.scratchpad;

import dev.algopractice.execution.ExecutionService;
import dev.algopractice.execution.ScratchReport;
import dev.algopractice.user.AppUser;
import dev.algopractice.user.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Free-form Java programs: save, list and run them with custom stdin. */
@RestController
@RequestMapping("/api/scratchpads")
public class ScratchpadController {

    private final ScratchpadRepository scratchpads;
    private final ExecutionService execution;

    public ScratchpadController(ScratchpadRepository scratchpads, ExecutionService execution) {
        this.scratchpads = scratchpads;
        this.execution = execution;
    }

    public record ScratchpadBody(@NotBlank @Size(max = 200) String title,
                                 @NotNull @Size(max = 65536) String code,
                                 @Size(max = 65536) String stdin) {
    }

    public record ScratchpadView(long id, String title, String code, String stdin, Instant updatedAt) {
        static ScratchpadView of(Scratchpad s) {
            return new ScratchpadView(s.getId(), s.getTitle(), s.getCode(), s.getStdin(), s.getUpdatedAt());
        }
    }

    public record RunBody(@NotNull @Size(max = 65536) String code, @Size(max = 65536) String stdin) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ScratchpadView> list(@CurrentUser AppUser user) {
        return scratchpads.findByUserOrderByUpdatedAtDesc(user).stream().map(ScratchpadView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ScratchpadView create(@CurrentUser AppUser user, @Valid @RequestBody ScratchpadBody body) {
        Scratchpad pad = new Scratchpad(user);
        pad.update(body.title(), body.code(), body.stdin());
        return ScratchpadView.of(scratchpads.save(pad));
    }

    @PutMapping("/{id}")
    @Transactional
    public ScratchpadView update(@CurrentUser AppUser user, @PathVariable long id,
                                 @Valid @RequestBody ScratchpadBody body) {
        Scratchpad pad = find(user, id);
        pad.update(body.title(), body.code(), body.stdin());
        return ScratchpadView.of(pad);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@CurrentUser AppUser user, @PathVariable long id) {
        scratchpads.delete(find(user, id));
    }

    /** Runs code without saving it; the scratchpad UI and the problem page's scratch tab both use this. */
    @PostMapping("/run")
    public ScratchReport run(@Valid @RequestBody RunBody body) {
        return execution.scratch(body.code(), body.stdin());
    }

    private Scratchpad find(AppUser user, long id) {
        return scratchpads.findByIdAndUser(id, user)
                .orElseThrow(() -> new NoSuchElementException("Scratchpad " + id + " not found"));
    }
}
