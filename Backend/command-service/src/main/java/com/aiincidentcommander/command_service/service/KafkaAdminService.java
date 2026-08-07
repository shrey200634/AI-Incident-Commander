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
            boolean exists = admin.listTopics().names().get().contains(topicName);
            if (!exists) {
                log.info("DLQ topic '{}' does not exist yet — nothing to clear.", topicName);
                return true;
            }

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
            if (!records.isEmpty()){
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



    public  boolean restoreDeadLetterQueueFromBackup(String backupTopicName  , String originalTopic){
        try{
            List<ConsumerRecord<byte[], byte[]>> records = consumeAllRecords(backupTopicName);
            if (records.isEmpty()){
                log.warn("⚠️ [DLQ RESTORE] No backed-up messages found in '{}' — nothing to restore into '{}'.",
                        backupTopicName, originalTopic);
                return true;
            }
            publishRecord(originalTopic, records);
            log.info("♻️ [DLQ RESTORE] Replayed {} message(s) from '{}' back into '{}'.",
                    records.size(), backupTopicName, originalTopic);
            return true ;
        }catch (Exception e){
            log.error("❌ [DLQ RESTORE] Failed to restore '{}' from backup '{}': {}",
                    originalTopic, backupTopicName, e.getMessage());
            return false ;
        }
    }


    private List<ConsumerRecord<byte[],byte[]>> consumeAllRecords(String topicName ){
        List<ConsumerRecord<byte[],byte[]>> collected = new ArrayList<>();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-rollback-scanner-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(props)) {
            var partitionInfos = consumer.partitionsFor(topicName);
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return collected;
            }
            List<TopicPartition> partitions = partitionInfos.stream()
                    .map(p -> new TopicPartition(topicName, p.partition()))
                    .collect(Collectors.toList());

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            long remaining = endOffsets.values().stream().mapToLong(Long::longValue).sum()
                    - partitions.stream().mapToLong(consumer::position).sum();

            int emptyPolls = 0;
            while (remaining > 0 && emptyPolls < 5) {
                ConsumerRecords<byte[], byte[]> batch = consumer.poll(Duration.ofSeconds(2));
                if (batch.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                batch.forEach(collected::add);
                remaining = endOffsets.entrySet().stream()
                        .mapToLong(e -> e.getValue() - consumer.position(e.getKey()))
                        .filter(n -> n > 0)
                        .sum();
            }
        }
        return collected;
    }


    private void publishRecord(String topicName, List<ConsumerRecord<byte[], byte[]>> records) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(props)) {
            for (ConsumerRecord<byte[], byte[]> record : records) {
                producer.send(new ProducerRecord<>(topicName, record.key(), record.value()));
            }
            producer.flush();
        }
    }

}