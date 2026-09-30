package dev.algocode.scratchpad;

import dev.algocode.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "scratchpad")
public class Scratchpad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;
    private String title;
    private String code;
    private String stdin;
    private Instant updatedAt;

    protected Scratchpad() {
    }

    public Scratchpad(AppUser user) {
        this.user = user;
    }

    public void update(String title, String code, String stdin) {
        this.title = title;
        this.code = code;
        this.stdin = stdin == null ? "" : stdin;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTitle() { return title; }
    public String getCode() { return code; }
    public String getStdin() { return stdin; }
    public Instant getUpdatedAt() { return updatedAt; }
}
