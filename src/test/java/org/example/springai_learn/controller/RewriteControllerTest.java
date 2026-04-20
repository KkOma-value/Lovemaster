package org.example.springai_learn.controller;

import org.example.springai_learn.ai.dto.RewriteRequest;
import org.example.springai_learn.ai.dto.RewriteResponse;
import org.example.springai_learn.ai.exception.RateLimitExceededException;
import org.example.springai_learn.ai.exception.RewriteException;
import org.example.springai_learn.ai.service.RewriteAgentService;
import org.example.springai_learn.ai.service.RewriteRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewriteControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rewrite_shouldReturnOptimizedText_whenAllowed() {
        RewriteAgentService rewriteAgentService = mock(RewriteAgentService.class);
        RewriteRateLimiter rateLimiter = mock(RewriteRateLimiter.class);
        RewriteController controller = new RewriteController(rewriteAgentService, rateLimiter);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "N/A")
        );

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        RewriteRequest request = new RewriteRequest("帮我把提问写得更清楚", null, "love");

        when(rateLimiter.tryAcquire("user-1", "127.0.0.1")).thenReturn(true);
        when(rewriteAgentService.optimize(request, "user-1")).thenReturn(new RewriteResponse("优化后的提问"));

        RewriteResponse response = controller.rewrite(request, servletRequest);

        assertEquals("优化后的提问", response.optimizedText());
        verify(rateLimiter).tryAcquire("user-1", "127.0.0.1");
        verify(rewriteAgentService).optimize(request, "user-1");
    }

    @Test
    void rewrite_shouldThrowRateLimitExceeded_whenDenied() {
        RewriteAgentService rewriteAgentService = mock(RewriteAgentService.class);
        RewriteRateLimiter rateLimiter = mock(RewriteRateLimiter.class);
        RewriteController controller = new RewriteController(rewriteAgentService, rateLimiter);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "N/A")
        );

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        RewriteRequest request = new RewriteRequest("测试", null, "coach");

        when(rateLimiter.tryAcquire("user-1", "127.0.0.1")).thenReturn(false);

        assertThrows(RateLimitExceededException.class, () -> controller.rewrite(request, servletRequest));
    }

    @Test
    void rewrite_shouldBubbleRewriteException_whenServiceFails() {
        RewriteAgentService rewriteAgentService = mock(RewriteAgentService.class);
        RewriteRateLimiter rateLimiter = mock(RewriteRateLimiter.class);
        RewriteController controller = new RewriteController(rewriteAgentService, rateLimiter);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "N/A")
        );

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        RewriteRequest request = new RewriteRequest("测试", null, "coach");

        when(rateLimiter.tryAcquire("user-1", "127.0.0.1")).thenReturn(true);
        when(rewriteAgentService.optimize(request, "user-1")).thenThrow(new RewriteException("rewrite failed"));

        assertThrows(RewriteException.class, () -> controller.rewrite(request, servletRequest));
    }
}
