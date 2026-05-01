package com.adamekka.spins_backend.spin.engine;

import com.adamekka.spins_backend.paytable.model.Paytable;
import com.adamekka.spins_backend.paytable.model.SymbolConfig;
import com.adamekka.spins_backend.paytable.model.SymbolType;
import com.adamekka.spins_backend.spin.dto.GridCellResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PayoutCalculator {
    public PayoutResult evaluate(
        List<List<GridCellResponse>> grid, Paytable paytable, BigDecimal bet
    ) {
        Map<String, Long> counts = new HashMap<>();
        for (List<GridCellResponse> row : grid) {
            for (GridCellResponse cell : row) {
                counts.merge(cell.code(), 1l, Long::sum);
            }
        }

        BigDecimal winAmount = BigDecimal.ZERO;
        Set<String> winningRegularCodes = new HashSet<>();

        for (SymbolConfig symbol : paytable.getSymbols()) {
            if (symbol.getSymbolType() != SymbolType.REGULAR) {
                continue;
            }

            int count = counts.getOrDefault(symbol.getCode(), 0l).intValue();
            if (count < paytable.getMinMatchCount()) {
                continue;
            }

            winningRegularCodes.add(symbol.getCode());
            winAmount
                = winAmount.add(bet.multiply(payoutMultiplier(symbol, count)));
        }

        int scatterCount
            = paytable.getSymbols()
                  .stream()
                  .filter(
                      symbol -> symbol.getSymbolType() == SymbolType.SCATTER
                  )
                  .findFirst()
                  .map(
                      symbol
                      -> counts.getOrDefault(symbol.getCode(), 0l).intValue()
                  )
                  .orElse(0);

        winAmount = winAmount.add(scatterWin(scatterCount, bet));

        return new PayoutResult(
            winAmount.setScale(2, RoundingMode.HALF_UP),
            winningRegularCodes,
            scatterCount
        );
    }

    private BigDecimal payoutMultiplier(SymbolConfig symbol, int count) {
        if (count >= 12) {
            return symbol.getPayoutHigh();
        }
        if (count >= 10) {
            return symbol.getPayoutMid();
        }
        return symbol.getPayoutLow();
    }

    private BigDecimal scatterWin(int scatterCount, BigDecimal bet) {
        if (scatterCount >= 6) {
            return bet.multiply(new BigDecimal("100.00"));
        }
        if (scatterCount == 5) {
            return bet.multiply(new BigDecimal("5.00"));
        }
        if (scatterCount == 4) {
            return bet.multiply(new BigDecimal("2.00"));
        }
        return BigDecimal.ZERO;
    }
}
