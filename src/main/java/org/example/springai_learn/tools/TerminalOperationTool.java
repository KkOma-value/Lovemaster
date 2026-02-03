package org.example.springai_learn.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TerminalOperationTool {

    /** Maximum characters returned inline. Larger outputs are truncated. */
    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final String TRUNCATION_SUFFIX = "... [truncated]";

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
//            Process process = Runtime.getRuntime().exec(command);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Early termination if output exceeds limit
                    if (output.length() > MAX_OUTPUT_CHARS) {
                        break;
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage());
        }
        String result = output.toString();
        if (result.length() > MAX_OUTPUT_CHARS) {
            return result.substring(0, MAX_OUTPUT_CHARS) + TRUNCATION_SUFFIX
                    + "\n[output truncated from " + result.length() + " chars]";
        }
        return result;
    }
}
