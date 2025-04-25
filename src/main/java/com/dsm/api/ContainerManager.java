package com.dsm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContainerManager {

    public static void main(String[] args) throws Exception {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        JsonNode root = new ObjectMapper().readTree(new File("cmd.json"));

        List<String> startedIds = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode cmdJson : root) {
                startedIds.add(startOne(dockerClient, cmdJson));
            }
        } else {
            startedIds.add(startOne(dockerClient, root));
        }

        System.out.println("全部容器已启动：" + startedIds);
    }

    private static String startOne(DockerClient dockerClient, JsonNode cmdJson) {
        var cmd = ContainerCmdFactory.fromJson(dockerClient, cmdJson);
        return ContainerStarter.startContainer(dockerClient, cmd);
    }
}