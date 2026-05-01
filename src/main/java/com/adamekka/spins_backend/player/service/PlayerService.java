package com.adamekka.spins_backend.player.service;

import com.adamekka.spins_backend.error.ApiException;
import com.adamekka.spins_backend.player.model.Player;
import com.adamekka.spins_backend.player.repository.PlayerRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    public static final String SEED_USERNAME = "player";

    // Startup seeding and the reset endpoint should restore the same balance.
    public static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public Player getCurrentPlayer() {
        return playerRepository.findByUsername(SEED_USERNAME)
            .orElseThrow(
                ()
                    -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PLAYER_NOT_FOUND",
                        "Seed player not found"
                    )
            );
    }

    @Transactional
    public Player getCurrentPlayerForUpdate() {
        return playerRepository.findByUsernameForUpdate(SEED_USERNAME)
            .orElseThrow(
                ()
                    -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PLAYER_NOT_FOUND",
                        "Seed player not found"
                    )
            );
    }

    @Transactional
    public Player resetCurrentPlayer() {
        Player player = playerRepository.findByUsername(SEED_USERNAME)
                            .orElseThrow(
                                ()
                                    -> new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "PLAYER_NOT_FOUND",
                                        "Seed player not found"
                                    )
                            );
        player.setBalance(INITIAL_BALANCE);
        return player;
    }
}
