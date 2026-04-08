package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

@Service
public class ClipboardService {

    @Value("${debug.clipboard.enabled:true}")
    private boolean clipboardEnabled;

    public void copySolutionToClipboard(AIDebugResponse response) {
        if (!clipboardEnabled) return;
        if (response == null || response.getSuggestedFix() == null) return;

        try {
            String content = formatForClipboard(response);
            StringSelection selection = new StringSelection(content);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);

            System.out.println(ConsoleColors.GREEN + "📋 Solusi sudah disalin ke clipboard!" + ConsoleColors.RESET);
        } catch (HeadlessException e) {
            System.out.println(ConsoleColors.YELLOW + "💡 Environment tidak support clipboard, copy manual dari console aja ya..." + ConsoleColors.RESET);
        } catch (Exception e) {
            System.err.println("❌ Gagal copy ke clipboard: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            System.out.println(ConsoleColors.YELLOW + "💡 Copy manual dari console aja ya..." + ConsoleColors.RESET);
        }
    }

    private String formatForClipboard(AIDebugResponse response) {
        StringBuilder sb = new StringBuilder();

        if (response.getSuggestedFix() != null) {
            sb.append("SOLUSI:\n");
            sb.append(response.getSuggestedFix()).append("\n\n");
        }

        if (response.getCodeExample() != null) {
            sb.append("KODE:\n");
            sb.append(response.getCodeExample());
        }

        return sb.toString();
    }
}
