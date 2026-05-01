package com.adamekka.spins_backend.spin.api;

import com.adamekka.spins_backend.spin.dto.BuyFreeSpinsResponse;
import com.adamekka.spins_backend.spin.dto.FreeSpinRequest;
import com.adamekka.spins_backend.spin.dto.SpinDetailResponse;
import com.adamekka.spins_backend.spin.dto.SpinRequest;
import com.adamekka.spins_backend.spin.dto.SpinResponse;
import com.adamekka.spins_backend.spin.service.SpinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SpinController {
    private final SpinService spinService;

    @PostMapping("/spin")
    public SpinResponse spin(@Valid @RequestBody SpinRequest request) {
        return spinService.spin(request);
    }

    @PostMapping("/spin/free")
    public SpinResponse freeSpin(@Valid @RequestBody FreeSpinRequest request) {
        return spinService.freeSpin(request);
    }

    @PostMapping("/spin/buy")
    public BuyFreeSpinsResponse
    buyFreeSpins(@Valid @RequestBody SpinRequest request) {
        return spinService.buyFreeSpins(request);
    }

    @GetMapping("/spin/{id}")
    public SpinDetailResponse getSpin(@PathVariable Long id) {
        return spinService.getSpinDetail(id);
    }
}
