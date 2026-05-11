package com.llm.ai.project.debuggingAI.controller.dashboard;

import com.llm.ai.core.component.DebugSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/learning")
@CrossOrigin(origins = "*")
public class LearningController {

    @Autowired
    private DebugSession debugSession;

    @GetMapping("/learning-stats")
    public Map<String, Object> getLearningStats() {
        return debugSession.getLearningStats();
    }

    @GetMapping("/clear-learning")
    public String clearLearning() {
        debugSession.clearLearning();
        return "✅ AI Learning cache cleared";
    }
}
