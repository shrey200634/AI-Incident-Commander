package com.aiincidentcommander.command_service.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class DockerConfig {

    @Value("${docker.host:}")
    private String dockerHost;

    @Bean
    public DockerClient dockerClient() {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        if (dockerHost != null && !dockerHost.isBlank() && !dockerHost.contains("localhost:2375")) {
            builder.withDockerHost(dockerHost);
        } else if (new File("/var/run/docker.sock").exists()) {
            builder.withDockerHost("unix:///var/run/docker.sock");
        }

        DockerClientConfig config = builder.build();
        log.info("🐳 Initialized DockerClient connecting to host: {}", config.getDockerHost());

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}