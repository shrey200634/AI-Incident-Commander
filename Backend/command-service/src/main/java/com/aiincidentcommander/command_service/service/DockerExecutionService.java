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
            log.info("Restarting Docker container: {}", containerName);
            dockerClient.restartContainerCmd(containerName).exec();
            log.info("Successfully restarted container: {}", containerName);
            return true;
        } catch (NotFoundException e) {
            log.error("Container not found: {}", containerName);
            return false;
        } catch (Exception e) {
            log.error("Failed to restart container {}: {}", containerName, e.getMessage());
            return false;
        }
    }


    public boolean scaleWorkerPods(String serviceName, int replicas) {
        try {
            log.info("Scaling service {} to {} replicas", serviceName, replicas);
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "compose", "up", "-d", "--scale", serviceName + "=" + replicas, "--no-recreate"
            );
            pb.directory(new java.io.File(composeProjectDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            log.info("docker compose scale output: {}", output);
            if (exitCode != 0) {
                log.error("docker compose scale exited with code {}", exitCode);
            }
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Failed to scale {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
}