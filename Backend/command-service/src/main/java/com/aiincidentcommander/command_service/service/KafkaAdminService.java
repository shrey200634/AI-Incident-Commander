package com.aiincidentcommander.command_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
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



    public  String backupAndClearDeadLetterQueue(String topicName , String actionId ){
        String backupTopic = topicName + ".rollack." + actionId;
        try{
            List<ConsumerRecord<byte[] , byte[]>> records = consumeAllRecords(topicName);
            if (records.isEmpty()){
                publishRecord(backupTopic , records);
                log.info("📦 [DLQ BACKUP] Copied {} message(s) from '{}' into backup topic '{}' before clearing.",
                        records.size(), topicName, backupTopic);

            }else {
                log.info("📦 [DLQ BACKUP] '{}' had no messages to back up before clearing.", topicName);
            }
            boolean cleared = clearDeadLetterQueue(topicName);
            if (!cleared){
                log.error("❌ [DLQ BACKUP] Backup succeeded but clearing '{}' failed.", topicName);
                  return null ;
            }
            return backupTopic;
        } catch (Exception e) {
            log.error("❌ [DLQ BACKUP] Failed to back up '{}' before clearing: {}", topicName, e.getMessage());
            return null;
        }
    }
}