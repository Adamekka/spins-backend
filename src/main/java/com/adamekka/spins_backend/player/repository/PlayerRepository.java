package com.adamekka.spins_backend.player.repository;

import com.adamekka.spins_backend.player.model.Player;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.username = :username")
    Optional<Player>
    findByUsernameForUpdate(@Param("username") String username);
}
