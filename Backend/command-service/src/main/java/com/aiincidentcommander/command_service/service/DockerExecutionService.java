package com.aiincidentcommander.command_service.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerClient dockerClient;

    @Value("${docker.compose.project-dir}")
    private String composeProjectDir;


    public boolean restartService(String containerName) {
        try {
            log.info("🐳 [DOCKER EXECUTION ENGINE] Attempting restart for serviceName='{}'...", containerName);
            String targetId = findContainerId(containerName);
            if (targetId != null) {
                log.info("🚀 [DOCKER EXECUTION ENGINE] Executing dockerClient.restartContainerCmd('{}') [Target ID/Name: {}]", serviceNameOrTarget(containerName, targetId), targetId);
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

    private String serviceNameOrTarget(String serviceName, String targetId) {
        return targetId != null ? targetId : serviceName;
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


    public boolean scaleWorkerPods(String serviceName, int replicas) {
        try {
            log.info("🚀 [DOCKER ENGINE] Executing scale for service='{}' to {} replicas...", serviceName, replicas);
            
            java.util.List<String> candidates = java.util.List.of(
                serviceName,
                serviceName.replace("-service", ""),
                "distributedjobforge-" + serviceName,
                "djf-" + serviceName,
                "aic-" + serviceName
            );

            // 1. Try Docker Compose CLI first
            for (String candidate : candidates) {
                ProcessBuilder pb = new ProcessBuilder(
                        "docker", "compose", "up", "-d", "--scale", candidate + "=" + replicas, "--no-recreate"
                );
                pb.directory(new java.io.File(composeProjectDir));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();
                if (exitCode == 0 && !output.contains("no such service")) {
                    log.info("✅ [DOCKER COMPOSE ENGINE] Successfully scaled compose service '{}' to {} replicas!\n{}", candidate, replicas, output);
                    return true;
                }
            }

            // 2. Fallback: Scale via Direct Docker Engine API
            log.info("ℹ️ [DOCKER API ENGINE] Compose file mismatch. Attempting direct container replication via Docker API for service '{}'...", serviceName);
            String existingContainerId = findContainerId(serviceName);
            if (existingContainerId != null) {
                var inspect = dockerClient.inspectContainerCmd(existingContainerId).exec();
                String imageName = inspect.getConfig().getImage();
                String replicaName = serviceName + "-replica-" + (System.currentTimeMillis() % 1000);
                
                log.info("🚀 [DOCKER API ENGINE] Creating scaled replica container '{}' using image '{}'", replicaName, imageName);
                var createCmd = dockerClient.createContainerCmd(imageName).withName(replicaName);
                if (inspect.getConfig().getEnv() != null) {
                    createCmd.withEnv(inspect.getConfig().getEnv());
                }
                var container = createCmd.exec();
                dockerClient.startContainerCmd(container.getId()).exec();
                log.info("✅ [DOCKER API ENGINE] Successfully created and started scaled replica container '{}' (ID: {})", replicaName, container.getId());
                return true;
            }

            log.warn("⚠️ [DOCKER ENGINE] Could not find compose service or active container matching '{}' to scale.", serviceName);
            return false;
        } catch (Exception e) {
            log.error("❌ [DOCKER ENGINE] Exception scaling {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
}