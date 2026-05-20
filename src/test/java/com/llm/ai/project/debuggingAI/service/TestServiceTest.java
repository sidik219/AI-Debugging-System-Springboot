package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.debug.service.TestService;
import com.llm.ai.project.debuggingAI.debug.repository.TestRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @InjectMocks
    private TestService testService;

    @Mock
    private TestRepository testRepository;
}
