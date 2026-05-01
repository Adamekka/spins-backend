package com.adamekka.spins_backend.paytable.api;

import com.adamekka.spins_backend.paytable.dto.PayoutsResponse;
import com.adamekka.spins_backend.paytable.dto.PaytableResponse;
import com.adamekka.spins_backend.paytable.dto.PaytableSymbolResponse;
import com.adamekka.spins_backend.paytable.repository.PaytableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paytables")
@RequiredArgsConstructor
public class PaytableController {
    private final PaytableRepository paytableRepository;

    @GetMapping
    public List<PaytableResponse> getPaytables() {
        return paytableRepository.findAllByOrderByIdAsc()
            .stream()
            .map(
                paytable
                -> new PaytableResponse(
                    paytable.getId(),
                    paytable.getName(),
                    paytable.getReelCount(),
                    paytable.getRowCount(),
                    paytable.getSymbols()
                        .stream()
                        .map(
                            symbol
                            -> new PaytableSymbolResponse(
                                symbol.getCode(),
                                symbol.getSymbolType(),
                                new PayoutsResponse(
                                    symbol.getPayoutLow(),
                                    symbol.getPayoutMid(),
                                    symbol.getPayoutHigh()
                                )
                            )
                        )
                        .toList()
                )
            )
            .toList();
    }
}
