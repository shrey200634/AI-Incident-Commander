package com.aiincidentcommander.command_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusAlertPayload {

    private String version;
    private String status;
    private List<PrometheusAlert> alerts;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrometheusAlert {
        private String status;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private String startsAt;

        public String getServiceName() {
            if (labels == null) return "external-service";
            if (labels.containsKey("service")) return labels.get("service");
            if (labels.containsKey("job")) return labels.get("job");
            if (labels.containsKey("application")) return labels.get("application");
            if (labels.containsKey("app")) return labels.get("app");
            if (labels.containsKey("instance")) return labels.get("instance");
            if (labels.containsKey("alertname")) return labels.get("alertname");
            return "external-service";
        }

        public String getSeverity() {
            if (labels == null) return "CRITICAL";
            String sev = labels.getOrDefault("severity", "CRITICAL").toUpperCase();
            return switch (sev) {
                case "CRITICAL", "P1" -> "CRITICAL";
                case "HIGH", "P2" -> "HIGH";
                case "MEDIUM", "P3", "WARNING" -> "MEDIUM";
                default -> "LOW";
            };
        }
    }
}
