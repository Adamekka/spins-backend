package com.adamekka.spins_backend.seed;

import com.adamekka.spins_backend.paytable.model.Paytable;
import com.adamekka.spins_backend.paytable.model.SymbolConfig;
import com.adamekka.spins_backend.paytable.model.SymbolType;
import com.adamekka.spins_backend.paytable.repository.PaytableRepository;
import com.adamekka.spins_backend.player.model.Player;
import com.adamekka.spins_backend.player.repository.PlayerRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {
    private final PlayerRepository playerRepository;
    private final PaytableRepository paytableRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (playerRepository.findByUsername("player").isEmpty()) {
            playerRepository.save(new Player(
                "player", new BigDecimal("1000.00"), LocalDateTime.now()
            ));
        }

        if (paytableRepository.findByName("Gates of Olympus").isPresent()) {
            return;
        }

        Paytable paytable
            = new Paytable("Gates of Olympus", 6, 5, 8, 4, 15, 5, 100);
        paytable.addSymbol(new SymbolConfig(
            "CROWN",
            25,
            SymbolType.REGULAR,
            new BigDecimal("10.00"),
            new BigDecimal("25.00"),
            new BigDecimal("50.00")
        ));
        paytable.addSymbol(new SymbolConfig(
            "RING",
            30,
            SymbolType.REGULAR,
            new BigDecimal("5.00"),
            new BigDecimal("15.00"),
            new BigDecimal("25.00")
        ));
        paytable.addSymbol(new SymbolConfig(
            "CUP",
            35,
            SymbolType.REGULAR,
            new BigDecimal("4.00"),
            new BigDecimal("10.00"),
            new BigDecimal("20.00")
        ));
        paytable.addSymbol(new SymbolConfig(
            "HOURGLASS",
            40,
            SymbolType.REGULAR,
            new BigDecimal("2.50"),
            new BigDecimal("5.00"),
            new BigDecimal("10.00")
        ));
        paytable.addSymbol(new SymbolConfig(
            "GEM_BLUE",
            50,
            SymbolType.REGULAR,
            new BigDecimal("1.00"),
            new BigDecimal("2.00"),
            new BigDecimal("5.00")
        ));
        paytable.addSymbol(new SymbolConfig(
            "GEM_GREEN",
            55,
            SymbolType.REGULAR,
            new BigDecimal("0.80"),
            new BigDecimal("1.80"),
            new BigDecimal("4.00")
        ));
        paytable.addSymbol(new SymbolConfig(
            "GEM_PURPLE",
            60,
            SymbolType.REGULAR,
            new BigDecimal("0.50"),
            new BigDecimal("1.00"),
            new BigDecimal("2.50")
        ));
        paytable.addSymbol(new SymbolConfig(
            "GEM_YELLOW",
            65,
            SymbolType.REGULAR,
            new BigDecimal("0.40"),
            new BigDecimal("0.90"),
            new BigDecimal("2.00")
        ));
        paytable.addSymbol(
            new SymbolConfig("SCATTER", 8, SymbolType.SCATTER, null, null, null)
        );
        paytable.addSymbol(new SymbolConfig(
            "MULTIPLIER", 4, SymbolType.MULTIPLIER, null, null, null
        ));
        paytableRepository.save(paytable);
    }
}
