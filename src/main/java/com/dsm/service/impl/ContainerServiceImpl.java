package com.dsm.service.impl;

import com.dsm.api.DockerService;
import com.dsm.pojo.dto.image.ImageInspectDTO;
import com.dsm.pojo.entity.ContainerDetail;
import com.dsm.pojo.request.ContainerCreateRequest;
import com.dsm.pojo.request.ContainerUpdateRequest;
import com.dsm.service.ContainerService;
import com.dsm.utils.LogUtil;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Statistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 容器服务实现类
 * 实现容器管理的具体业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerServiceImpl implements ContainerService {
    private final DockerService dockerService;

    private static void printDockerRunCmd(ContainerCreateRequest request, String imageName) {
        // Implementation of the method
    }

    /**
     * 获取容器列表
     *
     * @return
     */
    @Override
    public List<Container> listContainers() {
        try {
            return dockerService.listContainers();
        } catch (RuntimeException e) {
            throw new RuntimeException("获取容器列表失败: " + e.getMessage());
        }
    }

    @Override
    public void removeContainer(String containerId) {
        dockerService.removeContainer(containerId);
    }

    @Override
    public Statistics getContainerStats(String containerId) {
        return dockerService.getContainerStats(containerId);

    }

    @Override
    public ContainerDetail getContainerConfig(String containerId) {
        // 获取容器详细信息
        InspectContainerResponse inspectResponse = dockerService.inspectContainerCmd(containerId);

        // 创建并填充 ContainerDetail2 对象
        ContainerDetail containerDetail = new ContainerDetail();

        // 基本信息
        containerDetail.setImage(inspectResponse.getConfig().getImage());
        containerDetail.setName(inspectResponse.getName().replaceFirst("/", ""));

        // 重启策略
        RestartPolicy restartPolicy = inspectResponse.getHostConfig().getRestartPolicy();
        if (restartPolicy != null) {
            containerDetail.setRestartPolicy(restartPolicy.getName());
        }

        // 网络模式
        containerDetail.setNetworkMode(inspectResponse.getHostConfig().getNetworkMode());

        // 端口映射
        List<ContainerDetail.PortMapping> portMappings = new ArrayList<>();
        if (inspectResponse.getNetworkSettings().getPorts() != null) {
            inspectResponse.getNetworkSettings().getPorts().getBindings().forEach((containerPort, hostPorts) -> {
                if (hostPorts != null && hostPorts.length > 0) {
                    ContainerDetail.PortMapping mapping = new ContainerDetail.PortMapping();
                    mapping.setContainerPort(containerPort.getPort());
                    mapping.setProtocol(containerPort.getProtocol().toString());
                    mapping.setHostPort(Integer.parseInt(hostPorts[0].getHostPortSpec()));
                    portMappings.add(mapping);
                }
            });
        }
        containerDetail.setPortMappings(portMappings);

        // 卷映射
        List<ContainerDetail.VolumeMapping> volumeMappings = new ArrayList<>();
        if (inspectResponse.getMounts() != null) {
            for (InspectContainerResponse.Mount mount : inspectResponse.getMounts()) {
                ContainerDetail.VolumeMapping mapping = new ContainerDetail.VolumeMapping();
                mapping.setHostPath(mount.getSource());
                mapping.setContainerPath(Objects.requireNonNull(mount.getDestination()).getPath());
                mapping.setReadOnly(Boolean.FALSE.equals(mount.getRW()));
                volumeMappings.add(mapping);
            }
        }
        containerDetail.setVolumeMappings(volumeMappings);

        // 环境变量
        List<ContainerDetail.EnvironmentVariable> envVars = new ArrayList<>();
        if (inspectResponse.getConfig().getEnv() != null) {
            for (String env : inspectResponse.getConfig().getEnv()) {
                String[] parts = env.split("=", 2);
                if (parts.length == 2) {
                    ContainerDetail.EnvironmentVariable envVar = new ContainerDetail.EnvironmentVariable();
                    envVar.setKey(parts[0]);
                    envVar.setValue(parts[1]);
                    envVars.add(envVar);
                }
            }
        }
        containerDetail.setEnvironmentVariables(envVars);
        // 特权模式
        containerDetail.setPrivileged(inspectResponse.getHostConfig().getPrivileged());
        ImageInspectDTO dto = new ImageInspectDTO();
        return containerDetail;
    }

    @Override
    public void startContainer(String containerId) {
        try {
            dockerService.startContainer(containerId);
        } catch (Exception e) {
            throw new RuntimeException("启动容器失败: " + e.getMessage());
        }
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            dockerService.stopContainer(containerId);
        } catch (Exception e) {
            LogUtil.logSysError("容器停止失败: " + e.getMessage());
            throw new RuntimeException("停止容器失败: " + e.getMessage());
        }
    }

    @Override
    public void updateContainer(String containerId, ContainerCreateRequest request) {
        try {
            // 1. 停止原容器
            stopContainer(containerId);
            LogUtil.logSysInfo("原容器已停止: " + containerId);

            // 2. 备份原容器配置
            ContainerDetail originalConfig = getContainerConfig(containerId);
            dockerService.renameContainer(containerId, originalConfig.getName() + "_backup");
            LogUtil.logSysInfo("已备份原容器配置: " + containerId);

            // 3. 创建新容器
            String newContainerId = createContainer(request);
            LogUtil.logSysInfo("新容器创建成功: " + newContainerId);


            // 5. 验证新容器状态
            if (!isContainerRunning(newContainerId)) {
                removeContainer(newContainerId);
                dockerService.renameContainer(containerId, originalConfig.getName());
                startContainer(containerId);
                throw new RuntimeException("新容器启动失败");
            }

            // 6. 删除原容器
            removeContainer(containerId);
            LogUtil.logSysInfo("原容器已删除: " + containerId);

        } catch (Exception e) {
            LogUtil.logSysError("更新容器失败: " + e.getMessage());
            throw new RuntimeException("更新容器失败: " + e.getMessage());
        }
    }

    @Override
    public String createContainerWithConfig(ContainerUpdateRequest request) {
        return null;
    }

    /**
     * 根据容器ID或名称查找容器
     *
     * @param containerIdOrName 容器ID或名称
     * @return 容器对象，如果未找到则返回null
     */
    private Container findContainerByIdOrName(String containerIdOrName) {
        List<Container> containers = dockerService.listContainers();
        return containers.stream().filter(container -> container.getId().equals(containerIdOrName) || Arrays.asList(container.getNames()).contains(containerIdOrName) || Arrays.asList(container.getNames()).contains("/" + containerIdOrName)).findFirst().orElse(null);
    }

    @Override
    public void restartContainer(String id) {
        dockerService.restartContainer(id);
    }

    @Override
    public String getContainerLogs(String containerId, int tail, boolean follow, boolean timestamps) {
        return dockerService.getContainerLogs(containerId, tail, follow, timestamps);

    }

    @Override
    public String createContainer(ContainerCreateRequest request) {
        CreateContainerResponse createContainerResponse = dockerService.configureContainerCmd(request);
        String containerId = createContainerResponse.getId();
        dockerService.startContainer(containerId);
        LogUtil.logSysInfo("新容器启动成功: " + containerId);
        return containerId;

    }


    public boolean isContainerRunning(String containerId) {
        Container container = findContainerByIdOrName(containerId);
        return container != null && "running".equals(container.getState());

    }
}