package com.aiincidentcommander.command_service.service;

import com.aiincidentcommander.command_service.config.ScalingProperties;
import com.github.dockerjava.api.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerClient dockerClient;
    private final ScalingProperties scalingProperties;

    @Value("${docker.compose.project-dir}")
    private String composeProjectDir;

    public boolean restartService(String containerName) {
        try {
            log.info("🐳 [DOCKER EXECUTION ENGINE] Attempting restart for serviceName='{}'...", containerName);
            String targetId = findContainerId(containerName);
            if (targetId != null) {
                log.info("🚀 [DOCKER EXECUTION ENGINE] Executing dockerClient.restartContainerCmd('{}')", targetId);
                dockerClient.restartContainerCmd(targetId).exec();
                log.info("✅ [DOCKER EXECUTION ENGINE] Successfully restarted Docker container '{}' for service: {}", targetId, containerName);
                return true;
            }
            log.warn("⚠️ [DOCKER EXECUTION ENGINE] No active container matching serviceName '{}' found in Docker daemon.", containerName);
            return false;
        } catch (Exception e) {
            log.error("❌ [DOCKER EXECUTION ENGINE] Exception restarting container for service {}: {}", containerName, e.getMessage());
            return false;
        }
    }

    private String findContainerId(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return null;
        String cleanName = serviceName.toLowerCase().trim();
        try {
            var containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            for (var c : containers) {
                if (c.getNames() != null) {
                    for (String rawName : c.getNames()) {
                        String lowerName = rawName.toLowerCase();
                        if (lowerName.contains(cleanName)) {
                            log.info("🎯 [DOCKER SEARCH] Found matching Docker container '{}' (ID: {}) for serviceName '{}'", rawName, c.getId(), serviceName);
                            return c.getId();
                        }
                    }
                }
            }
            log.warn("⚠️ [DOCKER SEARCH] No running/stopped container found containing substring '{}' in Docker daemon.", cleanName);
        } catch (Exception e) {
            log.warn("❌ [DOCKER SEARCH] Container search error for {}: {}", serviceName, e.getMessage());
        }
        return null;
    }

    private String resolveContainerPath(String hostWorkingDir) {
        String normalized = hostWorkingDir.replace("\\", "/");
        for (Map.Entry<String, String> entry : scalingProperties.getPathMappings().entrySet()) {
            if (normalized.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        log.warn("⚠️ No path mapping found for host working_dir '{}', using as-is", hostWorkingDir);
        return hostWorkingDir;
    }

    public boolean scaleWorkerPods(String serviceName, int replicas) {
        try {
            log.info("🚀 [DOCKER ENGINE] Scaling '{}' to {} replicas...", serviceName, replicas);

            String existingContainerId = findContainerId(serviceName);
            if (existingContainerId == null) {
                log.warn("⚠️ [DOCKER ENGINE] No running container found for '{}' — nothing to scale.", serviceName);
                return false;
            }

            var inspect = dockerClient.inspectContainerCmd(existingContainerId).exec();
            Map<String, String> labels = inspect.getConfig().getLabels();
            String composeProject = labels != null ? labels.get("com.docker.compose.project") : null;
            String composeService = labels != null ? labels.get("com.docker.compose.service") : null;
            String composeWorkingDir = labels != null ? labels.get("com.docker.compose.project.working_dir") : null;

            if (composeProject == null || composeService == null || composeWorkingDir == null) {
                log.warn("⚠️ [DOCKER ENGINE] Container for '{}' isn't Compose-managed (missing labels) — cannot scale.", serviceName);
                return false;
            }

            String containerPath = resolveContainerPath(composeWorkingDir);

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "compose", "-p", composeProject,
                    "up", "-d", "--scale", composeService + "=" + replicas, "--no-recreate"
            );
            pb.directory(new File(containerPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("✅ [DOCKER COMPOSE ENGINE] Scaled '{}' (project '{}') to {} replicas.\n{}",
                        composeService, composeProject, replicas, output);
                return true;
            }
            log.error("❌ [DOCKER COMPOSE ENGINE] Scale failed for '{}' (exit {}): {}", composeService, exitCode, output);
            return false;
        } catch (Exception e) {
            log.error("❌ [DOCKER ENGINE] Exception scaling {}: {}", serviceName, e.getMessage());
            return false;
        }
    }

    @jakarta.annotation.PostConstruct
    public void logPathMappings() {
        log.info("🗺️ [SCALING CONFIG] Loaded path-mappings: {}", scalingProperties.getPathMappings());
    }
}