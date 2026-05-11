package com.llm.ai.project.debuggingAI.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestRepository testRepository;

}
