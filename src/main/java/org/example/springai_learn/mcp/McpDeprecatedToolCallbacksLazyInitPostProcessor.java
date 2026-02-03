package org.example.springai_learn.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI MCP Client autoconfigure 会注册 deprecated 的 ToolCallback 列表 bean。
 * 该 bean 在创建时会同步触发 MCP 工具发现（listTools），当 MCP server 尚未就绪时可能导致启动失败。
 *
 * 这里将 deprecated bean 标记为 lazy-init，确保主程序启动不被阻塞。
 */
@Component
@Slf4j
public class McpDeprecatedToolCallbacksLazyInitPostProcessor
        implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private static final List<String> TARGET_BEAN_NAMES = List.of(
            "toolCallbacksDeprecated",
            "asyncToolCallbacksDeprecated"
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : TARGET_BEAN_NAMES) {
            if (!registry.containsBeanDefinition(beanName)) {
                continue;
            }

            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (!beanDefinition.isLazyInit()) {
                beanDefinition.setLazyInit(true);
                log.info("MCP: marked bean '{}' as lazy-init to avoid startup blocking", beanName);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }
}
