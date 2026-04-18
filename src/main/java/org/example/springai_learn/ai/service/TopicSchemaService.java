package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicSchemaService {

    private static final TopicSchema FALLBACK_SCHEMA = new TopicSchema(
            "v1-fallback",
            List.of("破冰期", "升温期", "冷淡期", "挽回期"),
            List.of("聊天", "邀约", "情绪处理", "关系推进"),
            List.of("不回复", "冷淡", "拒绝", "暧昧不清")
    );

    private final KnowledgeProperties knowledgeProperties;

    private volatile CachedSchema cachedSchema;

    public TopicClassification classify(String question, String answer) {
        TopicSchema schema = getCurrentSchema();
        String unknownLabel = unknownLabel();
        String text = normalize(question) + " " + normalize(answer);

        String stage = matchOrUnknown(schema.stages(), text, unknownLabel);
        String intent = matchOrUnknown(schema.intents(), text, unknownLabel);
        String problem = matchOrUnknown(schema.problems(), text, unknownLabel);

        boolean unknownTopic = isUnknown(stage, unknownLabel)
                || isUnknown(intent, unknownLabel)
                || isUnknown(problem, unknownLabel);

        return new TopicClassification(stage, intent, problem, schema.version(), unknownTopic);
    }

    TopicSchema getCurrentSchema() {
        Path schemaPath = Paths.get(knowledgeProperties.getSink().getTopicSchemaPath()).toAbsolutePath().normalize();
        long modifiedAt = Files.exists(schemaPath) ? modifiedAt(schemaPath) : -1L;
        CachedSchema current = cachedSchema;
        if (current != null && current.path().equals(schemaPath) && current.modifiedAt() == modifiedAt) {
            return current.schema();
        }

        if (modifiedAt < 0) {
            log.warn("Topic schema file not found: {}, fallback schema will be used", schemaPath);
            cachedSchema = new CachedSchema(schemaPath, modifiedAt, FALLBACK_SCHEMA);
            return FALLBACK_SCHEMA;
        }

        TopicSchema loaded = loadSchema(schemaPath);
        cachedSchema = new CachedSchema(schemaPath, modifiedAt, loaded);
        log.info("Topic schema loaded: path={}, version={}, stages={}, intents={}, problems={}",
                schemaPath,
                loaded.version(),
                loaded.stages().size(),
                loaded.intents().size(),
                loaded.problems().size());
        return loaded;
    }

    private TopicSchema loadSchema(Path schemaPath) {
        try {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new FileSystemResource(schemaPath.toFile()));
            Properties properties = yaml.getObject();
            if (properties == null) {
                return FALLBACK_SCHEMA;
            }

            String version = readValue(properties, "version", FALLBACK_SCHEMA.version());
            List<String> stages = readList(properties, "stages", FALLBACK_SCHEMA.stages());
            List<String> intents = readList(properties, "intents", FALLBACK_SCHEMA.intents());
            List<String> problems = readList(properties, "problems", FALLBACK_SCHEMA.problems());
            return new TopicSchema(version, stages, intents, problems);
        } catch (Exception ex) {
            log.error("Failed to load topic schema: path={}, error={}", schemaPath, ex.getMessage());
            return FALLBACK_SCHEMA;
        }
    }

    private long modifiedAt(Path schemaPath) {
        try {
            return Files.getLastModifiedTime(schemaPath).toMillis();
        } catch (Exception ex) {
            return -1L;
        }
    }

    private String readValue(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private List<String> readList(Properties properties, String key, List<String> fallback) {
        List<String> values = new ArrayList<>();
        for (int index = 0; ; index++) {
            String value = properties.getProperty(key + "[" + index + "]");
            if (value == null) {
                break;
            }
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }
        if (values.isEmpty()) {
            return fallback;
        }
        return values;
    }

    private String matchOrUnknown(List<String> labels, String normalizedText, String unknownLabel) {
        for (String label : labels) {
            String normalizedLabel = normalize(label);
            if (!normalizedLabel.isBlank() && normalizedText.contains(normalizedLabel)) {
                return label;
            }
        }
        return unknownLabel;
    }

    private boolean isUnknown(String value, String unknownLabel) {
        return normalize(value).equals(normalize(unknownLabel));
    }

    private String unknownLabel() {
        String configured = knowledgeProperties.getSink().getUnknownLabel();
        if (configured == null || configured.isBlank()) {
            return "unknown";
        }
        return configured;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private record CachedSchema(Path path, long modifiedAt, TopicSchema schema) {
    }

    public record TopicSchema(String version, List<String> stages, List<String> intents, List<String> problems) {
    }

    public record TopicClassification(
            String stage,
            String intent,
            String problem,
            String schemaVersion,
            boolean unknownTopic
    ) {
    }
}
