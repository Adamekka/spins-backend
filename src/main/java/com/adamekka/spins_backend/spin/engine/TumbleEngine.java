package com.adamekka.spins_backend.spin.engine;

import com.adamekka.spins_backend.paytable.model.Paytable;
import com.adamekka.spins_backend.paytable.model.SymbolConfig;
import com.adamekka.spins_backend.spin.dto.GridCellResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TumbleEngine {
    private static final int[] MULTIPLIER_VALUES
        = {2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 50, 100, 250, 500};

    private static final int[] MULTIPLIER_WEIGHTS
        = {300, 200, 150, 100, 70, 50, 40, 30, 20, 15, 10, 7, 5, 2, 1};

    private final PayoutCalculator payoutCalculator;
    private final SecureRandom random = new SecureRandom();

    public SpinOutcome process(Paytable paytable, BigDecimal bet) {
        List<TumbleOutcome> tumbles = new ArrayList<>();
        List<List<GridCellResponse>> grid = generateGrid(paytable);
        BigDecimal baseWin = BigDecimal.ZERO;
        int maxScatterCount = 0;

        while (true) {
            PayoutResult payout
                = payoutCalculator.evaluate(grid, paytable, bet);
            int multiplierOnGrid = sumMultiplier(grid);
            maxScatterCount = Math.max(maxScatterCount, payout.scatterCount());
            baseWin = baseWin.add(payout.winAmount());

            tumbles.add(new TumbleOutcome(
                tumbles.size(),
                copyGrid(grid),
                payout.winAmount(),
                multiplierOnGrid
            ));

            if (payout.winningRegularCodes().isEmpty()) {
                break;
            }

            grid = tumble(paytable, grid, payout.winningRegularCodes());
        }

        return new SpinOutcome(
            tumbles,
            baseWin.setScale(2, RoundingMode.HALF_UP),
            sumMultiplier(grid),
            maxScatterCount
        );
    }

    private List<List<GridCellResponse>> generateGrid(Paytable paytable) {
        List<List<GridCellResponse>> grid = new ArrayList<>();
        for (int row = 0; row < paytable.getRowCount(); row++) {
            List<GridCellResponse> cells = new ArrayList<>();
            for (int column = 0; column < paytable.getReelCount(); column++) {
                cells.add(randomCell(paytable));
            }
            grid.add(cells);
        }
        return grid;
    }

    private List<List<GridCellResponse>> tumble(
        Paytable paytable,
        List<List<GridCellResponse>> grid,
        Set<String> winningRegularCodes
    ) {
        int rows = paytable.getRowCount();
        int columns = paytable.getReelCount();
        List<List<GridCellResponse>> nextGrid = emptyGrid(rows, columns);

        for (int column = 0; column < columns; column++) {
            List<GridCellResponse> fallingCells = new ArrayList<>();
            for (int row = rows - 1; row >= 0; row--) {
                GridCellResponse cell = grid.get(row).get(column);
                if (isMultiplier(cell)
                    || winningRegularCodes.contains(cell.code())) {
                    continue;
                }
                fallingCells.add(cell);
            }

            int fallingIndex = 0;
            for (int row = rows - 1; row >= 0; row--) {
                GridCellResponse currentCell = grid.get(row).get(column);
                if (isMultiplier(currentCell)) {
                    // Multipliers stay anchored across tumbles by design.
                    nextGrid.get(row).set(column, currentCell);
                } else if (fallingIndex < fallingCells.size()) {
                    nextGrid.get(row).set(
                        column, fallingCells.get(fallingIndex)
                    );
                    fallingIndex++;
                } else {
                    nextGrid.get(row).set(column, randomCell(paytable));
                }
            }
        }

        return nextGrid;
    }

    private List<List<GridCellResponse>> emptyGrid(int rows, int columns) {
        List<List<GridCellResponse>> grid = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            List<GridCellResponse> cells = new ArrayList<>();
            for (int column = 0; column < columns; column++) {
                cells.add(null);
            }
            grid.add(cells);
        }
        return grid;
    }

    private GridCellResponse randomCell(Paytable paytable) {
        int totalWeight = paytable.getSymbols()
                              .stream()
                              .mapToInt(SymbolConfig::getWeight)
                              .sum();
        int roll = random.nextInt(totalWeight) + 1;
        int cursor = 0;

        for (SymbolConfig symbol : paytable.getSymbols()) {
            cursor += symbol.getWeight();
            if (roll <= cursor) {
                Integer multiplierValue
                    = "MULTIPLIER".equals(symbol.getCode())
                        ? randomMultiplierValue()
                        : null;
                return new GridCellResponse(symbol.getCode(), multiplierValue);
            }
        }

        SymbolConfig fallback = paytable.getSymbols().getLast();
        return new GridCellResponse(fallback.getCode(), null);
    }

    private int randomMultiplierValue() {
        int totalWeight = 0;
        for (int weight : MULTIPLIER_WEIGHTS) {
            totalWeight += weight;
        }

        int roll = random.nextInt(totalWeight) + 1;
        int cursor = 0;
        for (int index = 0; index < MULTIPLIER_VALUES.length; index++) {
            cursor += MULTIPLIER_WEIGHTS[index];
            if (roll <= cursor) {
                return MULTIPLIER_VALUES[index];
            }
        }
        return MULTIPLIER_VALUES[0];
    }

    private int sumMultiplier(List<List<GridCellResponse>> grid) {
        int total = 0;
        for (List<GridCellResponse> row : grid) {
            for (GridCellResponse cell : row) {
                if (isMultiplier(cell)) {
                    total += cell.multiplierValue();
                }
            }
        }
        return total;
    }

    private boolean isMultiplier(GridCellResponse cell) {
        return cell != null && "MULTIPLIER".equals(cell.code());
    }

    private List<List<GridCellResponse>>
    copyGrid(List<List<GridCellResponse>> grid) {
        List<List<GridCellResponse>> copy = new ArrayList<>();
        for (List<GridCellResponse> row : grid) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }
}
