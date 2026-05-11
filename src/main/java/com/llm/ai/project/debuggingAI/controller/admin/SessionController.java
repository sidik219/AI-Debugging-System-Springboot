package com.llm.ai.project.debuggingAI.controller.admin;

import com.llm.ai.core.component.DebugSession;
import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug-session")
public class SessionController {

    @Autowired
    private DebugSession debugSession;

    @GetMapping("/session-stats")
    public Map<String, Object> sessionStats() {
        return debugSession.getStats();
    }

    @GetMapping("/clear-session")
    public String clearSession() {
        debugSession.clearAll();
        return "✅ Session cleared";
    }

    @GetMapping("/mark-success")
    public String markSuccess() {
        ErrorContext ctx = new ErrorContext();
        ctx.setExceptionType("java.lang.NullPointerException");
        ctx.setClassName("com.llm.ai.project.debuggingAI.controller.debug.AIController");
        ctx.setMethodName("testError");
        ctx.setLineNumber(58);

        AIDebugResponse resp = new AIDebugResponse();
        resp.setSuggestedFix("return nullString != null ? nullString.toUpperCase() : \"\";");
        resp.setConfidence("HIGH");

        debugSession.recordSuccess(ctx, resp);
        return "✅ Marked as success";
    }
}
