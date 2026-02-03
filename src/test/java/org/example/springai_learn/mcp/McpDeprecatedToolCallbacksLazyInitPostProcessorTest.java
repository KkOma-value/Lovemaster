package org.example.springai_learn.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.junit.jupiter.api.Assertions.assertTrue;

class McpDeprecatedToolCallbacksLazyInitPostProcessorTest {

    @Test
    void marksDeprecatedToolCallbackBeansAsLazyInit() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("toolCallbacksDeprecated", new RootBeanDefinition(Object.class));
        factory.registerBeanDefinition("asyncToolCallbacksDeprecated", new RootBeanDefinition(Object.class));

        McpDeprecatedToolCallbacksLazyInitPostProcessor postProcessor = new McpDeprecatedToolCallbacksLazyInitPostProcessor();
        postProcessor.postProcessBeanDefinitionRegistry(factory);

        assertTrue(factory.getBeanDefinition("toolCallbacksDeprecated").isLazyInit());
        assertTrue(factory.getBeanDefinition("asyncToolCallbacksDeprecated").isLazyInit());
    }

    @Test
    void noOpWhenBeansAbsent() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        McpDeprecatedToolCallbacksLazyInitPostProcessor postProcessor = new McpDeprecatedToolCallbacksLazyInitPostProcessor();
        postProcessor.postProcessBeanDefinitionRegistry(factory);
    }
}
