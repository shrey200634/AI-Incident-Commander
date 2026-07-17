package com.aiincidentcommander.command_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_INCIDENT_CREATED = "incident.created";
    public static final String TOPIC_ACTION_PROPOSED = "action.proposed";
    public static final String TOPIC_ACTION_APPROVED = "action.approved";
    public static final String TOPIC_ACTION_EXECUTED = "action.executed";
    public static final String TOPIC_ACTION_ROLLED_BACK = "action.rolled_back";
    public static final String TOPIC_INCIDENT_ESCALATED = "incident.escalated";
    public static final String TOPIC_INCIDENT_RESOLVED = "incident.resolved";

    @Bean
    public NewTopic incidentCreatedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic actionProposedTopic() {
        return TopicBuilder.name(TOPIC_ACTION_PROPOSED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic actionApprovedTopic() {
        return TopicBuilder.name(TOPIC_ACTION_APPROVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic actionExecutedTopic() {
        return TopicBuilder.name(TOPIC_ACTION_EXECUTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic actionRolledBackTopic() {
        return TopicBuilder.name(TOPIC_ACTION_ROLLED_BACK)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic incidentEscalatedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_ESCALATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic incidentResolvedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_RESOLVED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
