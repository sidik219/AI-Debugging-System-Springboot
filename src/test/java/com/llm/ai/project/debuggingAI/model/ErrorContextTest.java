package com.llm.ai.project.debuggingAI.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ErrorContextTest {

    private ErrorContext obj;

    @BeforeEach
    void setUp() {
        obj = new ErrorContext();
    }


@Test
void testGetExceptionType() {
    // Arrange
    String expected = "Test exceptionType";
    obj.setExceptionType(expected);

    // Act
    String actual = obj.getExceptionType();

    // Assert
    assertEquals(expected, actual);
}

@Test
void testSetExceptionType() {
    // Arrange
    String expected = "Test exceptionType";

    // Act
    obj.setExceptionType(expected);

    // Assert
    assertEquals(expected, obj.getExceptionType());
}

@Test
void testGetMessage() {
    // Arrange
    String expected = "Test message";
    obj.setMessage(expected);

    // Act
    String actual = obj.getMessage();

    // Assert
    assertEquals(expected, actual);
}

@Test
void testSetMessage() {
    // Arrange
    String expected = "Test message";

    // Act
    obj.setMessage(expected);

    // Assert
    assertEquals(expected, obj.getMessage());
}

@Test
void testGetClassName() {
    // Arrange
    String expected = "Test className";
    obj.setClassName(expected);

    // Act
    String actual = obj.getClassName();

    // Assert
    assertEquals(expected, actual);
}

@Test
void testSetClassName() {
    // Arrange
    String expected = "Test className";

    // Act
    obj.setClassName(expected);

    // Assert
    assertEquals(expected, obj.getClassName());
}

@Test
void testGetMethodName() {
    // Arrange
    String expected = "Test methodName";
    obj.setMethodName(expected);

    // Act
    String actual = obj.getMethodName();

    // Assert
    assertEquals(expected, actual);
}

@Test
void testSetMethodName() {
    // Arrange
    String expected = "Test methodName";

    // Act
    obj.setMethodName(expected);

    // Assert
    assertEquals(expected, obj.getMethodName());
}

@Test
void testGetLineNumber() {
    // Arrange
    int expected = 42;
    obj.setLineNumber(expected);

    // Act
    int actual = obj.getLineNumber();

    // Assert
    assertEquals(expected, actual);
}

@Test
void testSetLineNumber() {
    // Arrange
    int expected = 42;

    // Act
    obj.setLineNumber(expected);

    // Assert
    assertEquals(expected, obj.getLineNumber());
}

@Test
void testGetFileName() {
    // Arrange
    String expected = "Test fileName";
    obj.setFileName(expected);

    // Act
    String actual = obj.getFileName();

    // Assert
    assertEquals(expected, actual);
}

@Test
void testSetFileName() {
    // Arrange
    String expected = "Test fileName";

    // Act
    obj.setFileName(expected);

    // Assert
    assertEquals(expected, obj.getFileName());
}
}
