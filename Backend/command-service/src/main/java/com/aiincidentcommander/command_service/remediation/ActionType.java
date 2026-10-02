package com.aiincidentcommander.command_service.remediation;

import com.aiincidentcommander.command_service.exception.UnsupportedActionTypeException;

import java.util.Optional;


public enum ActionType {
    RESTART_SERVICE(RiskLevel.MEDIUM),
    SCALE_SERVICE(RiskLevel.LOW),
    ROLLBACK_DEPLOYMENT(RiskLevel.HIGH),
    CLEAR_KAFKA_DLQ(RiskLevel.HIGH);

    private final RiskLevel defaultRisk;

    ActionType(RiskLevel defaultRisk) {
        this.defaultRisk = defaultRisk;
    }

    public RiskLevel defaultRisk() {
        return defaultRisk;
    }

    public static ActionType parse(String raw) {
        return tryParse(raw).orElseThrow(() -> new UnsupportedActionTypeException(raw));
    }

    public static Optional<ActionType> tryParse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String normalized = raw.trim().toUpperCase();
        if ("SCALE_WORKER_PODS".equals(normalized)) {
            return Optional.of(SCALE_SERVICE);
        }
        for (ActionType t : values()) {
            if (t.name().equals(normalized)) return Optional.of(t);
        }
        return Optional.empty();
    }
}