package com.aiincidentcommander.command_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KafkaAdminService {

    private final String bootstrapServers;

    public KafkaAdminService(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
    public boolean clearDeadLetterQueue(String topicName) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {
            var description = admin.describeTopics(List.of(topicName))
                    .allTopicNames().get().get(topicName);

            List<TopicPartition> partitions = description.partitions().stream()
                    .map(p -> new TopicPartition(topicName, p.partition()))
                    .toList();

            Map<TopicPartition, OffsetSpec> offsetRequest = partitions.stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            var endOffsets = admin.listOffsets(offsetRequest).all().get();

            Map<TopicPartition, RecordsToDelete> deleteMap = new HashMap<>();
            endOffsets.forEach((tp, info) ->
                    deleteMap.put(tp, RecordsToDelete.beforeOffset(info.offset())));

            admin.deleteRecords(deleteMap).all().get();
            log.info("Cleared dead-letter queue: {}", topicName);
            return true;
        } catch (Exception e) {
            log.error("Failed to clear DLQ topic {}: {}", topicName, e.getMessage());
            return false;
        }
    }
}