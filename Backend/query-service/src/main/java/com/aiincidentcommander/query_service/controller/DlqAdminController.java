package com.aiincidentcommander.query_service.controller;

import com.aiincidentcommander.query_service.exception.DlqRecordNotFoundException;
import com.aiincidentcommander.query_service.model.DlqRecord;
import com.aiincidentcommander.query_service.repo.DlqRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dlq")
@Slf4j
public class DlqAdminController {

    private final DlqRecordRepository dlqRecordRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * List all unreplayed DLQ records.
     */
    @GetMapping
    public List<DlqRecord> listUnreplayed() {
        return dlqRecordRepository.findByReplayedFalse();
    }

    /**
     * List all DLQ records (including already replayed ones).
     */
    @GetMapping("/all")
    public List<DlqRecord> listAll() {
        return dlqRecordRepository.findAll();
    }

    /**
     * Replay a DLQ record by its numeric ID.
     * Deserializes the stored JSON payload back into a Map and sends it
     * to the original Kafka topic so consumers can re-process it.
     */
    @PostMapping("/{id}/replay")
    public ResponseEntity<DlqRecord> replay(@PathVariable Long id) {
        DlqRecord record = dlqRecordRepository.findById(id)
                .orElseThrow(() -> new DlqRecordNotFoundException("DLQ record not found with id: " + id));

        if (record.isReplayed()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Deserialize the stored JSON payload back into a Map so the Kafka
            // JsonSerializer sends it as proper JSON (not a double-encoded string).
            Object payload;
            try {
                payload = objectMapper.readValue(record.getPayload(), Map.class);
            } catch (Exception e) {
                // Fallback: send as raw string if it's not valid JSON
                payload = record.getPayload();
            }

            kafkaTemplate.send(record.getOriginalTopic(), payload);

            record.setReplayed(true);
            record.setReplayedAt(LocalDateTime.now());
            DlqRecord saved = dlqRecordRepository.save(record);
            log.info("Replayed DLQ record id={} to topic={}", id, record.getOriginalTopic());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to replay DLQ record id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}