package org.example.springai_learn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.dto.ChatRunDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/ai/runs")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@RequiredArgsConstructor
@Slf4j
public class ChatRunController {

    private final ChatRunService chatRunService;

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return "anonymous";
    }

    @GetMapping
    public List<ChatRunDto> listActiveRuns() {
        String userId = getCurrentUserId();
        log.debug("获取活跃聊天运行: userId={}", userId);
        return chatRunService.listActiveRunsForUser(userId).stream()
                .map(ChatRunDto::from)
                .toList();
    }

    @GetMapping("/{runId}")
    public ChatRunDto getRun(@PathVariable String runId) {
        String userId = getCurrentUserId();
        log.debug("获取聊天运行详情: runId={}, userId={}", runId, userId);
        return chatRunService.getRunForUser(runId, userId)
                .map(ChatRunDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "run 不存在"));
    }
}
