package com.dsm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;

public class ContainerCmdFactory {

    public static CreateContainerCmd fromJson(DockerClient dockerClient, JsonNode cmdJson) {
        CreateContainerCmd cmd = dockerClient
                .createContainerCmd(cmdJson.get("Image").asText())
                .withEnv(DockerJsonParser.parseEnv(cmdJson.get("Env")))
                .withExposedPorts(DockerJsonParser.parseExposedPorts(cmdJson.get("ExposedPorts")));

        JsonNode hostConfig = cmdJson.get("HostConfig");
        cmd.withHostConfig(new HostConfig()
                .withPortBindings(DockerJsonParser.parsePortBindings(hostConfig.get("PortBindings")))
                .withBinds(DockerJsonParser.parseBinds(hostConfig.get("Binds")))
                .withPrivileged(hostConfig.get("Privileged").asBoolean())
                .withNetworkMode(hostConfig.get("NetworkMode").asText())
                .withRestartPolicy(RestartPolicy.parse(hostConfig.get("RestartPolicy").asText()))
        );

        return cmd;
    }
}