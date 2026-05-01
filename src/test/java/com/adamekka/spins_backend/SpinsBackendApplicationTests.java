package com.adamekka.spins_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adamekka.spins_backend.player.model.Player;
import com.adamekka.spins_backend.player.repository.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SpinsBackendApplicationTests {
    @Autowired private MockMvc mockMvc;

    @Autowired private PlayerRepository playerRepository;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void contextLoads() {}

    @Test
    void getPlayerReturnsSeededPlayer() throws Exception {
        mockMvc.perform(get("/api/player"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("player"))
            .andExpect(jsonPath("$.balance").value("1000.00"));
    }

    @Test
    void resetPlayerRestoresInitialBalance() throws Exception {
        Player player = playerRepository.findByUsername("player").orElseThrow();
        player.setBalance(new BigDecimal("123.45"));
        playerRepository.save(player);

        mockMvc.perform(post("/api/player/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value("1000.00"));
    }

    @Test
    void getPaytablesReturnsSeededConfig() throws Exception {
        mockMvc.perform(get("/api/paytables"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Gates of Olympus"))
            .andExpect(jsonPath("$[0].reelCount").value(6))
            .andExpect(jsonPath("$[0].rowCount").value(5))
            .andExpect(jsonPath("$[0].symbols.length()").value(10))
            .andExpect(jsonPath("$[0].symbols[0].code").value("CROWN"))
            .andExpect(jsonPath("$[0].symbols[0].payouts.low").value("10.00"));
    }

    @Test
    void spinCreatesPersistedSpinAndTumbles() throws Exception {
        MvcResult result
            = mockMvc
                  .perform(post("/api/spin")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("""
                    {"paytableId":1,"bet":"1.00"}
                    """))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.spinType").value("BASE"))
                  .andExpect(jsonPath("$.bet").value("1.00"))
                  .andReturn();

        JsonNode response
            = objectMapper.readTree(result.getResponse().getContentAsString());
        long spinId = response.get("spinId").asLong();
        assertThat(response.get("tumbles").size()).isGreaterThan(0);
        assertThat(response.get("tumbles").get(0).get("grid").size())
            .isEqualTo(5);
        assertThat(response.get("tumbles").get(0).get("grid").get(0).size())
            .isEqualTo(6);

        mockMvc.perform(get("/api/spin/{id}", spinId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(spinId))
            .andExpect(jsonPath("$.spinType").value("BASE"));

        mockMvc.perform(get("/api/player/history?limit=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.spins[0].id").value(spinId))
            .andExpect(jsonPath("$.spins[0].tumbleCount")
                           .value(response.get("tumbles").size()));
    }

    @Test
    void buyFreeSpinsCreatesSessionAndFreeSpinConsumesIt() throws Exception {
        MvcResult buyResult
            = mockMvc
                  .perform(post("/api/spin/buy")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("""
                    {"paytableId":1,"bet":"1.00"}
                    """))
                  .andExpect(status().isOk())
                  .andExpect(
                      jsonPath("$.spinType").value("PURCHASED_FREE_SPINS")
                  )
                  .andExpect(jsonPath("$.cost").value("100.00"))
                  .andExpect(jsonPath("$.newBalance").value("900.00"))
                  .andExpect(jsonPath("$.freeSpinsAwarded").value(15))
                  .andReturn();

        JsonNode buyResponse = objectMapper.readTree(
            buyResult.getResponse().getContentAsString()
        );
        long parentSpinId = buyResponse.get("parentSpinId").asLong();

        MvcResult freeSpinResult
            = mockMvc
                  .perform(
                      post("/api/spin/free")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("{\"parentSpinId\":" + parentSpinId + "}")
                  )
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.spinType").value("FREE_SPIN"))
                  .andExpect(jsonPath("$.bet").value("0.00"))
                  .andReturn();

        JsonNode freeSpinResponse = objectMapper.readTree(
            freeSpinResult.getResponse().getContentAsString()
        );
        assertThat(freeSpinResponse.get("remainingFreeSpins").asInt())
            .isGreaterThanOrEqualTo(14);

        mockMvc
            .perform(
                get("/api/spin/{id}", freeSpinResponse.get("spinId").asLong())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parentSpinId").value(parentSpinId));

        mockMvc
            .perform(post("/api/spin/free")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(
                             "{\"parentSpinId\":"
                             + freeSpinResponse.get("spinId").asLong() + "}"
                         ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("INVALID_FREE_SPIN_PARENT"));
    }

    @Test
    void spinRejectsInvalidBetScale() throws Exception {
        mockMvc
            .perform(post("/api/spin")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content("""
                    {"paytableId":1,"bet":"1.001"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_BET"));
    }

    @Test
    void spinRejectsInsufficientBalance() throws Exception {
        Player player = playerRepository.findByUsername("player").orElseThrow();
        player.setBalance(new BigDecimal("0.50"));
        playerRepository.save(player);

        mockMvc
            .perform(post("/api/spin")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content("""
                    {"paytableId":1,"bet":"1.00"}
                    """))
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));
    }
}
