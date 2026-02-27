package com.gestion.qnt.controllers;

import com.gestion.qnt.model.entity.Pilot;
import com.gestion.qnt.model.enums.PilotStatus;
import com.gestion.qnt.model.persistence.PilotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DronesAvailabilityRestControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Autowired
    private PilotRepository pilotRepository;

    private static final String TELEGRAM_ID = "test-telegram-123";

    @BeforeEach
    void setUp() {
        pilotRepository.deleteAll();
    }

    @Test
    void availability_withoutHeader_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drones/availability"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void availability_withInvalidTelegramId_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drones/availability")
                        .header("X-Telegram-Id", "nonexistent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void availability_withValidPilot_returns200() throws Exception {
        Pilot pilot = Pilot.builder()
                .name("Test Pilot")
                .telegramId(TELEGRAM_ID)
                .missionPassword("hashed")
                .status(PilotStatus.active)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        pilotRepository.save(pilot);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drones/availability")
                        .header("X-Telegram-Id", TELEGRAM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
