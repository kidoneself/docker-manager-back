package com.dsm.api;

import com.dsm.exception.DockerErrorResolver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class DockerClientWrapper {
    private final DockerClient dockerClient;

    public DockerClientWrapper() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        
        ApacheDockerHttpClient.Builder httpClientBuilder = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45));

        DockerHttpClient httpClient = httpClientBuilder.build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public List<Container> listContainers() {
        return executeDockerCommandWithResult(() -> 
            dockerClient.listContainersCmd().withShowAll(true).exec(),
            "获取容器列表", "all"
        );
    }

    public List<Image> listImages() {
        return executeDockerCommandWithResult(() -> 
            dockerClient.listImagesCmd().withShowAll(true).exec(),
            "获取镜像列表", "all"
        );
    }

    public void startContainer(String containerId) {
        executeDockerCommand(() -> {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("启动容器成功: {}", containerId);
        }, "启动容器", containerId);
    }

    public void stopContainer(String containerId) {
        executeDockerCommand(() -> {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("停止容器成功: {}", containerId);
        }, "停止容器", containerId);
    }

    public void restartContainer(String containerId) {
        executeDockerCommand(() -> {
            dockerClient.restartContainerCmd(containerId).exec();
            log.info("重启容器成功: {}", containerId);
        }, "重启容器", containerId);
    }

    public void removeContainer(String containerId) {
        executeDockerCommand(() -> {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("删除容器成功: {}", containerId);
        }, "删除容器", containerId);
    }

    public Statistics getContainerStats(String containerId) {
        return executeDockerCommandWithResult(() -> {
            InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
            dockerClient.statsCmd(containerId).exec(callback);
            try {
                Statistics stats = callback.awaitResult();
                callback.close();
                return stats;
            } catch (IOException e) {
                throw new RuntimeException("Failed to close stats callback", e);
            }
        }, "获取容器统计信息", containerId);
    }

    public boolean isDockerAvailable() {
        return executeDockerCommandWithResult(() -> {
            try {
                dockerClient.pingCmd().exec();
                return true;
            } catch (Exception e) {
                log.error("Docker服务不可用: {}", e.getMessage());
                return false;
            }
        }, "检查Docker服务可用性", "system");
    }

    public String getContainerLogs(String containerId, int tail, boolean follow, boolean timestamps) {
        return executeDockerCommandWithResult(() -> {
            LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId)
                    .withTail(tail)
                    .withFollowStream(follow)
                    .withTimestamps(timestamps)
                    .withStdOut(true)
                    .withStdErr(true);

            StringBuilder logs = new StringBuilder();
            try {
                logContainerCmd.exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        logs.append(new String(frame.getPayload())).append("\n");
                    }
                }).awaitCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取容器日志被中断", e);
            }

            return logs.toString();
        }, "获取容器日志", containerId);
    }

    public void removeImage(String imageId) {
        executeDockerCommand(() -> {
            dockerClient.removeImageCmd(imageId).withForce(true).exec();
            log.info("删除镜像成功: {}", imageId);
        }, "删除镜像", imageId);
    }

    public InspectImageResponse getInspectImage(String imageId) {
        return executeDockerCommandWithResult(() -> 
            dockerClient.inspectImageCmd(imageId).exec(),
            "获取镜像详细信息", imageId
        );
    }

    public InspectContainerResponse inspectContainerCmd(String containerId) {
        return executeDockerCommandWithResult(() -> 
            dockerClient.inspectContainerCmd(containerId).exec(),
            "获取容器详细信息", containerId
        );
    }

    public void renameContainer(String containerId, String newName) {
        executeDockerCommand(() -> {
            dockerClient.renameContainerCmd(containerId).withName(newName).exec();
            log.info("重命名容器成功: {} -> {}", containerId, newName);
        }, "重命名容器", containerId);
    }

    public List<Network> listNetworks() {
        return executeDockerCommandWithResult(() -> 
            dockerClient.listNetworksCmd().exec(),
            "获取网络列表", "all"
        );
    }

    public CreateContainerResponse createContainer(CreateContainerCmd cmd) {
        return executeDockerCommandWithResult(() -> 
            cmd.exec(),
            "创建容器", cmd.getName()
        );
    }

    public CreateContainerCmd createContainerCmd(String imageName) {
        return dockerClient.createContainerCmd(imageName);
    }

    public PullImageCmd pullImageCmd(String image) {
        return dockerClient.pullImageCmd(image);
    }

    public StartContainerCmd startContainerCmd(String containerId) {
        return dockerClient.startContainerCmd(containerId);
    }

    public ListContainersCmd listContainersCmd() {
        return dockerClient.listContainersCmd();
    }

    private void executeDockerCommand(Runnable command, String action, String containerId) {
        try {
            command.run();
        } catch (Exception e) {
            log.error("{}失败: {}", action, e.getMessage());
            throw DockerErrorResolver.resolve(action, containerId, e);
        }
    }

    private <T> T executeDockerCommandWithResult(Supplier<T> supplier, String operationName, String containerId) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("{}失败: {}", operationName, e.getMessage(), e);
            throw DockerErrorResolver.resolve(operationName, containerId, e);
        }
    }
} 