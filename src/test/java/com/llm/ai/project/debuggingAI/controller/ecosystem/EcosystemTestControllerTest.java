package com.llm.ai.project.debuggingAI.controller.ecosystem;

import com.llm.ai.project.debuggingAI.ecosystem.controller.EcosystemTestController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EcosystemTestController.class)
class EcosystemTestControllerTest {

    @Autowired
    private MockMvc mockMvc;


@Test
@DisplayName("Test POST /api/ekosistem/spesifik/create-orders")
void testCreateOrder() throws Exception {
    // Arrange
    String url = "/api/ekosistem/spesifik/create-orders";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}

@Test
@DisplayName("Test GET /api/ekosistem/default/repo-error")
void testTestRepoError() throws Exception {
    // Arrange
    String url = "/api/ekosistem/default/repo-error";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/ekosistem/default/calc-error")
void testTestCalcError() throws Exception {
    // Arrange
    String url = "/api/ekosistem/default/calc-error";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/ekosistem/default/delete-error")
void testTestDeleteError() throws Exception {
    // Arrange
    String url = "/api/ekosistem/default/delete-error";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/ekosistem/default/service-error")
void testTestServiceError() throws Exception {
    // Arrange
    String url = "/api/ekosistem/default/service-error";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/ekosistem/spesifik/order")
void testTestOrder() throws Exception {
    // Arrange
    String url = "/api/ekosistem/spesifik/order";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}
}
