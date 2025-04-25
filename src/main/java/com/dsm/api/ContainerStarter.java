package com.dsm.api;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;

public class ContainerStarter {

    public static String startContainer(DockerClient dockerClient, CreateContainerCmd cmd) {
        try {
            String containerId = cmd.exec().getId();
            dockerClient.startContainerCmd(containerId).exec();
            System.out.println("容器启动成功，ID: " + containerId);
            return containerId;
        } catch (Exception e) {
            throw new RuntimeException("容器启动失败: " + e.getMessage(), e);
        }
    }
}