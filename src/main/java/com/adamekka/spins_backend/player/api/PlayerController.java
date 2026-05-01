package com.adamekka.spins_backend.player.api;

import com.adamekka.spins_backend.player.dto.PlayerResponse;
import com.adamekka.spins_backend.player.model.Player;
import com.adamekka.spins_backend.player.service.PlayerService;
import com.adamekka.spins_backend.spin.dto.SpinHistoryListResponse;
import com.adamekka.spins_backend.spin.service.SpinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    private final SpinService spinService;

    @GetMapping
    public PlayerResponse getPlayer() {
        Player player = playerService.getCurrentPlayer();
        return new PlayerResponse(
            player.getId(), player.getUsername(), player.getBalance()
        );
    }

    @PostMapping("/reset")
    public PlayerResponse resetPlayer() {
        Player player = playerService.resetCurrentPlayer();
        return new PlayerResponse(
            player.getId(), player.getUsername(), player.getBalance()
        );
    }

    @GetMapping("/history")
    public SpinHistoryListResponse
    getHistory(@RequestParam(defaultValue = "20") int limit) {
        return spinService.getCurrentPlayerHistory(limit);
    }
}
