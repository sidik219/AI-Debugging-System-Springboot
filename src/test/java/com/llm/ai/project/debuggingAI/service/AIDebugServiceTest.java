package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.core.common.UnitTestGenerator;
import com.llm.ai.core.component.DebugSession;
import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIDebugServiceTest {

    @InjectMocks
    private AIDebugService aIDebugService;

    @Mock
    private GroqService groqService;
    @Mock
    private OpenAIService openAIService;
    @Mock
    private GeminiService geminiService;
    @Mock
    private DebugSession debugSession;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UnitTestGenerator unitTestGenerator;
    @Mock
    private AutoFixService autoFixService;

    @Test
    void testApplyAutoFix() {
        // Arrange
        ErrorContext context = mock(ErrorContext.class);
        AIDebugResponse response = mock(AIDebugResponse.class);
        boolean expected = true;
        when(autoFixService.applyFix(any(ErrorContext.class), any(AIDebugResponse.class))).thenReturn(expected);

        // Act
        boolean result = aIDebugService.applyAutoFix(context, response);

        // Assert
        assertNotNull(result);
        verify(autoFixService).applyFix(any(ErrorContext.class), any(AIDebugResponse.class));
    }

@Test
void testAnalyzeError() {
    // Arrange
    ErrorContext errorContext = new ErrorContext();
    errorContext.setExceptionType("NullPointerException");
    errorContext.setMessage("Test error");

    // Act
    AIDebugResponse result = aIDebugService.analyzeError(errorContext);

    // Assert
    assertNotNull(result);
}

    @Test
    void testGenerateAllTests() {
        // Arrange
        String className = "test";
        String expected = "test";
        when(unitTestGenerator.generateAllUnitTests(any(String.class))).thenReturn(expected);

        // Act
        String result = aIDebugService.generateAllTests(className);

        // Assert
        assertNotNull(result);
        verify(unitTestGenerator).generateAllUnitTests(any(String.class));
    }

    @Test
    void testMarkAsFixed() {
        // Arrange
        ErrorContext context = mock(ErrorContext.class);
        AIDebugResponse response = mock(AIDebugResponse.class);

        // Act
        aIDebugService.markAsFixed(context, response);

        // Assert
        verify(debugSession).recordSuccess(any(ErrorContext.class), any(AIDebugResponse.class));
    }

    @Test
    void testClearSession() {
        // Arrange
        ErrorContext context = mock(ErrorContext.class);

        // Act
        aIDebugService.clearSession(context);

        // Assert
        verify(debugSession).clearSession(any(ErrorContext.class));
    }

    @Test
    void testGenerateTest() {
        // Arrange
        ErrorContext context = mock(ErrorContext.class);
        String expected = "test";
        when(unitTestGenerator.generateUnitTest(any(ErrorContext.class))).thenReturn(expected);

        // Act
        String result = aIDebugService.generateTest(context);

        // Assert
        assertNotNull(result);
        verify(unitTestGenerator).generateUnitTest(any(ErrorContext.class));
    }

    @Test
    void testSendNotification() {
        // Arrange
        ErrorContext context = mock(ErrorContext.class);
        AIDebugResponse response = mock(AIDebugResponse.class);

        // Act
        aIDebugService.sendNotification(context, response);

        // Assert
        verify(notificationService).sendErrorNotification(any(ErrorContext.class), any(AIDebugResponse.class), any(String.class));
    }
}
