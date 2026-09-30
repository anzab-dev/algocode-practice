package dev.algocode.scratchpad;

import dev.algocode.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScratchpadRepository extends JpaRepository<Scratchpad, Long> {

    List<Scratchpad> findByUserOrderByUpdatedAtDesc(AppUser user);

    Optional<Scratchpad> findByIdAndUser(Long id, AppUser user);

    long countByUser(AppUser user);
}
