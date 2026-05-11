package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.TestEntity;
import com.llm.ai.project.debuggingAI.repository.TestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @InjectMocks
    private TestService testService;

    @Mock
    private TestRepository testRepository;
}
