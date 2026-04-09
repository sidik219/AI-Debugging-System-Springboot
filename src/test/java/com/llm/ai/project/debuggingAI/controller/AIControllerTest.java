package com.llm.ai.project.debuggingAI.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AIController.class)
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;


@Test
@DisplayName("Test GET /api/debug/kontol")
void testObj() throws Exception {
    // Arrange
    String url = "/api/debug/kontol";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}

@Test
@DisplayName("Test GET /api/debug/generate-service-test")
void testGenerateServiceTest() throws Exception {
    // Arrange
    String url = "/api/debug/generate-service-test";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}

@Test
@DisplayName("Test GET /api/debug/test-divide")
void testTestDivide() throws Exception {
    // Arrange
    String url = "/api/debug/test-divide";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/debug/test-array")
void testTestArray() throws Exception {
    // Arrange
    String url = "/api/debug/test-array";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/debug/test-error")
void testTestError() throws Exception {
    // Arrange
    String url = "/api/debug/test-error";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().is5xxServerError());
}

@Test
@DisplayName("Test GET /api/debug/generate-model-test")
void testGenerateModelTest() throws Exception {
    // Arrange
    String url = "/api/debug/generate-model-test";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}

@Test
@DisplayName("Test GET /api/debug/test-success")
void testTestSuccess() throws Exception {
    // Arrange
    String url = "/api/debug/test-success";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}

@Test
@DisplayName("Test GET /api/debug/generate-test")
void testGenerateTest() throws Exception {
    // Arrange
    String url = "/api/debug/generate-test";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}

@Test
@DisplayName("Test GET /api/debug/generate-controller-tests")
void testGenerateAllTests() throws Exception {
    // Arrange
    String url = "/api/debug/generate-controller-tests";

    // Act & Assert
    mockMvc.perform(get(url))
            .andExpect(status().isOk());
}
}
