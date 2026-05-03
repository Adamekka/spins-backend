package com.adamekka.spins_backend.spin.service;

import com.adamekka.spins_backend.error.ApiException;
import com.adamekka.spins_backend.paytable.model.Paytable;
import com.adamekka.spins_backend.paytable.repository.PaytableRepository;
import com.adamekka.spins_backend.player.model.Player;
import com.adamekka.spins_backend.player.service.PlayerService;
import com.adamekka.spins_backend.spin.dto.BuyFreeSpinsResponse;
import com.adamekka.spins_backend.spin.dto.FreeSpinRequest;
import com.adamekka.spins_backend.spin.dto.GridCellResponse;
import com.adamekka.spins_backend.spin.dto.SpinDetailResponse;
import com.adamekka.spins_backend.spin.dto.SpinHistoryListResponse;
import com.adamekka.spins_backend.spin.dto.SpinHistoryResponse;
import com.adamekka.spins_backend.spin.dto.SpinRequest;
import com.adamekka.spins_backend.spin.dto.SpinResponse;
import com.adamekka.spins_backend.spin.dto.TumbleResponse;
import com.adamekka.spins_backend.spin.engine.SpinOutcome;
import com.adamekka.spins_backend.spin.engine.TumbleEngine;
import com.adamekka.spins_backend.spin.engine.TumbleOutcome;
import com.adamekka.spins_backend.spin.model.Spin;
import com.adamekka.spins_backend.spin.model.SpinType;
import com.adamekka.spins_backend.spin.model.Tumble;
import com.adamekka.spins_backend.spin.repository.SpinRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpinService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final int FREE_SPIN_RETRIGGER_SCATTERS = 3;

    private final PlayerService playerService;
    private final PaytableRepository paytableRepository;
    private final SpinRepository spinRepository;
    private final TumbleEngine tumbleEngine;
    private final ObjectMapper objectMapper;

    @Transactional
    public Player resetCurrentPlayer() {
        Player player = playerService.resetCurrentPlayer();
        // Reset is an app-level restart, so persisted bonus sessions must not
        // survive a balance reset.
        spinRepository.clearActiveFreeSpinSessions(player, SpinType.FREE_SPIN);
        return player;
    }

    @Transactional
    public SpinResponse spin(SpinRequest request) {
        BigDecimal bet = validateBet(request.bet());
        Player player = playerService.getCurrentPlayerForUpdate();
        Paytable paytable = getPaytable(request.paytableId());
        ensureSufficientBalance(player, bet);

        player.setBalance(scaleMoney(player.getBalance().subtract(bet)));

        SpinOutcome outcome = tumbleEngine.process(paytable, bet);
        BigDecimal totalWin = totalBaseSpinWin(outcome);
        player.setBalance(scaleMoney(player.getBalance().add(totalWin)));

        boolean freeSpinsTriggered
            = outcome.maxScatterCount() >= paytable.getScatterTriggerCount();
        int freeSpinsAwarded
            = freeSpinsTriggered ? paytable.getFreeSpinsAwarded() : 0;

        Spin spin = new Spin(
            player,
            paytable,
            bet,
            totalWin,
            SpinType.BASE,
            null,
            0,
            freeSpinsAwarded,
            LocalDateTime.now()
        );
        attachTumbles(spin, outcome.tumbles());
        spinRepository.save(spin);

        return new SpinResponse(
            spin.getId(),
            spin.getSpinType(),
            spin.getBet(),
            spin.getTotalWin(),
            player.getBalance(),
            toTumbleResponses(outcome.tumbles()),
            freeSpinsTriggered,
            freeSpinsAwarded,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Transactional
    public SpinResponse freeSpin(FreeSpinRequest request) {
        Spin parentSpin
            = spinRepository.findSessionByIdForUpdate(request.parentSpinId())
                  .orElseThrow(
                      ()
                          -> new ApiException(
                              HttpStatus.NOT_FOUND,
                              "SPIN_NOT_FOUND",
                              "Spin " + request.parentSpinId()
                                  + " does not exist"
                          )
                  );

        if (parentSpin.getSpinType() == SpinType.FREE_SPIN) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "INVALID_FREE_SPIN_PARENT",
                "Free spin " + parentSpin.getId()
                    + " cannot be used as a session parent"
            );
        }

        if (parentSpin.getRemainingFreeSpins() <= 0) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "FREE_SPINS_EXHAUSTED",
                "Free spin session " + parentSpin.getId()
                    + " has no remaining spins"
            );
        }

        Player player = parentSpin.getPlayer();
        Paytable paytable = parentSpin.getPaytable();
        SpinOutcome outcome
            = tumbleEngine.process(paytable, parentSpin.getBet());
        int accumulatedMultiplier
            = parentSpin.getAccumulatedMultiplier() + outcome.finalMultiplier();
        boolean retriggered
            = outcome.maxScatterCount() >= FREE_SPIN_RETRIGGER_SCATTERS;
        int retriggerAwarded
            = retriggered ? paytable.getFreeSpinsRetrigger() : 0;
        int remainingFreeSpins
            = parentSpin.getRemainingFreeSpins() - 1 + retriggerAwarded;

        BigDecimal totalWin = totalFreeSpinWin(outcome, accumulatedMultiplier);
        player.setBalance(scaleMoney(player.getBalance().add(totalWin)));
        parentSpin.setAccumulatedMultiplier(accumulatedMultiplier);
        parentSpin.setRemainingFreeSpins(remainingFreeSpins);

        Spin spin = new Spin(
            player,
            paytable,
            ZERO_MONEY,
            totalWin,
            SpinType.FREE_SPIN,
            parentSpin,
            accumulatedMultiplier,
            remainingFreeSpins,
            LocalDateTime.now()
        );
        attachTumbles(spin, outcome.tumbles());
        spinRepository.save(spin);

        return new SpinResponse(
            spin.getId(),
            spin.getSpinType(),
            spin.getBet(),
            spin.getTotalWin(),
            player.getBalance(),
            toTumbleResponses(outcome.tumbles()),
            null,
            null,
            accumulatedMultiplier,
            remainingFreeSpins,
            retriggered,
            retriggerAwarded,
            remainingFreeSpins == 0
        );
    }

    @Transactional
    public BuyFreeSpinsResponse buyFreeSpins(SpinRequest request) {
        BigDecimal bet = validateBet(request.bet());
        Player player = playerService.getCurrentPlayerForUpdate();
        Paytable paytable = getPaytable(request.paytableId());
        BigDecimal cost = scaleMoney(bet.multiply(
            BigDecimal.valueOf(paytable.getBuyFreeSpinsMultiplier())
        ));
        ensureSufficientBalance(player, cost);

        player.setBalance(scaleMoney(player.getBalance().subtract(cost)));

        Spin spin = new Spin(
            player,
            paytable,
            bet,
            ZERO_MONEY,
            SpinType.PURCHASED_FREE_SPINS,
            null,
            0,
            paytable.getFreeSpinsAwarded(),
            LocalDateTime.now()
        );
        spinRepository.save(spin);

        return new BuyFreeSpinsResponse(
            spin.getId(),
            spin.getSpinType(),
            cost,
            player.getBalance(),
            paytable.getFreeSpinsAwarded(),
            spin.getId()
        );
    }

    @Transactional(readOnly = true)
    public SpinHistoryListResponse getCurrentPlayerHistory(int limit) {
        Player player = playerService.getCurrentPlayer();
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<SpinHistoryResponse> spins
            = spinRepository
                  .findRecentByPlayer(
                      player, PageRequest.of(0, normalizedLimit)
                  )
                  .stream()
                  .map(
                      spin
                      -> new SpinHistoryResponse(
                          spin.getId(),
                          spin.getSpinType(),
                          spin.getBet(),
                          spin.getTotalWin(),
                          spin.getSpunAt(),
                          spin.getTumbles().size()
                      )
                  )
                  .toList();
        return new SpinHistoryListResponse(spins);
    }

    @Transactional(readOnly = true)
    public SpinDetailResponse getSpinDetail(Long spinId) {
        Spin spin = spinRepository.findByIdWithTumbles(spinId).orElseThrow(
            ()
                -> new ApiException(
                    HttpStatus.NOT_FOUND,
                    "SPIN_NOT_FOUND",
                    "Spin " + spinId + " does not exist"
                )
        );

        Long parentSpinId = spin.getParentSpin() == null
                              ? null
                              : spin.getParentSpin().getId();

        return new SpinDetailResponse(
            spin.getId(),
            spin.getSpinType(),
            spin.getBet(),
            spin.getTotalWin(),
            spin.getSpunAt(),
            parentSpinId,
            spin.getAccumulatedMultiplier(),
            spin.getRemainingFreeSpins(),
            spin.getTumbles().stream().map(this::toTumbleResponse).toList()
        );
    }

    private Paytable getPaytable(Long paytableId) {
        return paytableRepository.findByIdWithSymbols(paytableId)
            .orElseThrow(
                ()
                    -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PAYTABLE_NOT_FOUND",
                        "Paytable " + paytableId + " does not exist"
                    )
            );
    }

    private BigDecimal validateBet(BigDecimal bet) {
        if (bet == null || bet.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_BET",
                "Bet must be greater than zero"
            );
        }

        if (bet.stripTrailingZeros().scale() > 2) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_BET",
                "Bet cannot have more than 2 decimal places"
            );
        }

        return bet.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void ensureSufficientBalance(Player player, BigDecimal amount) {
        if (player.getBalance().compareTo(amount) >= 0) {
            return;
        }

        throw new ApiException(
            HttpStatus.PAYMENT_REQUIRED,
            "INSUFFICIENT_BALANCE",
            "Balance " + player.getBalance() + " is less than bet " + amount
        );
    }

    private BigDecimal totalBaseSpinWin(SpinOutcome outcome) {
        if (outcome.baseWin().compareTo(BigDecimal.ZERO) <= 0
            || outcome.finalMultiplier() <= 0) {
            return scaleMoney(outcome.baseWin());
        }
        return scaleMoney(outcome.baseWin().multiply(
            BigDecimal.valueOf(outcome.finalMultiplier())
        ));
    }

    private BigDecimal
    totalFreeSpinWin(SpinOutcome outcome, int accumulatedMultiplier) {
        if (outcome.baseWin().compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_MONEY;
        }

        int effectiveMultiplier = Math.max(accumulatedMultiplier, 1);
        return scaleMoney(
            outcome.baseWin().multiply(BigDecimal.valueOf(effectiveMultiplier))
        );
    }

    private void attachTumbles(Spin spin, List<TumbleOutcome> outcomes) {
        for (TumbleOutcome outcome : outcomes) {
            spin.addTumble(new Tumble(
                outcome.sequenceIndex(),
                writeGrid(outcome.grid()),
                outcome.winAmount(),
                outcome.multiplierOnGrid()
            ));
        }
    }

    private List<TumbleResponse>
    toTumbleResponses(List<TumbleOutcome> outcomes) {
        return outcomes.stream()
            .map(
                outcome
                -> new TumbleResponse(
                    outcome.sequenceIndex(),
                    outcome.grid(),
                    outcome.winAmount(),
                    outcome.multiplierOnGrid()
                )
            )
            .toList();
    }

    private TumbleResponse toTumbleResponse(Tumble tumble) {
        return new TumbleResponse(
            tumble.getSequenceIndex(),
            readGrid(tumble.getGridState()),
            tumble.getWinAmount(),
            tumble.getMultiplierOnGrid()
        );
    }

    private String writeGrid(List<List<GridCellResponse>> grid) {
        try {
            return objectMapper.writeValueAsString(grid);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                "Could not serialize tumble grid", exception
            );
        }
    }

    private List<List<GridCellResponse>> readGrid(String gridState) {
        try {
            return objectMapper.readValue(
                gridState, new TypeReference<List<List<GridCellResponse>>>() {}
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                "Could not deserialize tumble grid", exception
            );
        }
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
