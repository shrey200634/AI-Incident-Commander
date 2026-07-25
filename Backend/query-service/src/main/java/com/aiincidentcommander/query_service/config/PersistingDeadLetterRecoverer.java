package com.aiincidentcommander.query_service.config;

import com.aiincidentcommander.query_service.model.DlqRecord;
import com.aiincidentcommander.query_service.repo.DlqRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

@Slf4j
public class PersistingDeadLetterRecoverer extends DeadLetterPublishingRecoverer {

    private final DlqRecordRepository dlqRecordRepository;
    private final ObjectMapper objectMapper;

    public PersistingDeadLetterRecoverer(KafkaTemplate<Object, Object> template,
                                         DlqRecordRepository dlqRecordRepository,
                                         ObjectMapper objectMapper) {
        super(template, (record, ex) ->
                new TopicPartition(record.topic() + ".dlq", record.partition()));
        this.dlqRecordRepository = dlqRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Override 3-arg accept method — invoked by DefaultErrorHandler in Spring Kafka.
     */
    @Override
    public void accept(ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, Exception exception) {
        persistToDatabase(record, exception);
        try {
            super.accept(record, consumer, exception);
        } catch (Exception kafkaEx) {
            log.error("Failed to publish to Kafka DLQ topic for topic={}, offset={}: {}",
                    record.topic(), record.offset(), kafkaEx.getMessage());
        }
    }

    /**
     * Override 2-arg accept method — for direct callers.
     */
    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        accept(record, null, exception);
    }

    private void persistToDatabase(ConsumerRecord<?, ?> record, Exception exception) {
        try {
            String payloadStr;
            if (record.value() instanceof String str) {
                payloadStr = str;
            } else if (record.value() != null) {
                payloadStr = objectMapper.writeValueAsString(record.value());
            } else {
                payloadStr = "";
            }

            String errorMsg = null;
            if (exception != null) {
                errorMsg = exception.getMessage();
                if (errorMsg != null && errorMsg.length() > 60000) {
                    errorMsg = errorMsg.substring(0, 60000);
                }
            }

            dlqRecordRepository.save(DlqRecord.builder()
                    .originalTopic(record.topic())
                    .partition(record.partition())
                    .offset(record.offset())
                    .payload(payloadStr)
                    .errorMessage(errorMsg)
                    .build());
            log.warn("Persisted DLQ record to DB for topic={}, offset={}", record.topic(), record.offset());
        } catch (Exception dbEx) {
            log.error("Failed to persist DLQ record to DB for topic={}, offset={}: {}",
                    record.topic(), record.offset(), dbEx.getMessage(), dbEx);
        }
    }
}