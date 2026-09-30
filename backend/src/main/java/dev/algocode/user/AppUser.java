package dev.algocode.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String handle;
    private Instant createdAt;

    protected AppUser() {
    }

    public AppUser(String handle) {
        this.handle = handle;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getHandle() { return handle; }
    public Instant getCreatedAt() { return createdAt; }
}
