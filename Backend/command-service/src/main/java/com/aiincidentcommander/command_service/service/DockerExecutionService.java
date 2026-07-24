package com.aiincidentcommander.command_service.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerClient dockerClient;


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
}