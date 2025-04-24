package com.dsm.api;

import com.dsm.config.AppConfig;
import com.dsm.pojo.request.ContainerCreateRequest;
import com.dsm.websocket.callback.PullImageCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Docker服务类，提供与Docker引擎交互的各种操作
 * 包括容器管理、镜像管理、日志查看等功能
 */
@Slf4j
@Service
public class DockerService {
    @Resource
    private DockerClientWrapper dockerClientWrapper;

    @Resource
    private AppConfig appConfig;

    /**
     * 获取所有容器列表
     *
     * @return 容器列表，包括运行中和已停止的容器
     */
    public List<Container> listContainers() {
        return dockerClientWrapper.listContainers();
    }

    /**
     * 获取所有镜像列表
     *
     * @return 镜像列表，包括所有本地镜像
     */
    public List<Image> listImages() {
        return dockerClientWrapper.listImages();
    }


    /**
     * 启动指定容器
     *
     * @param containerId 容器ID
     * @throws RuntimeException 如果启动失败
     */
    public void startContainer(String containerId) {
        dockerClientWrapper.startContainer(containerId);
    }

    /**
     * 停止指定容器
     *
     * @param containerId 容器ID
     * @throws RuntimeException 如果停止失败
     */
    public void stopContainer(String containerId) {
        dockerClientWrapper.stopContainer(containerId);
    }

    /**
     * 重启指定容器
     *
     * @param containerId 容器ID
     */
    public void restartContainer(String containerId) {
        dockerClientWrapper.restartContainer(containerId);
    }


    /**
     * 删除指定容器
     *
     * @param containerId 容器ID
     */
    public void removeContainer(String containerId) {
        dockerClientWrapper.removeContainer(containerId);
    }

    /**
     * 获取容器的统计信息
     *
     * @param containerId 容器ID
     * @return 容器统计信息对象
     */
    public Statistics getContainerStats(String containerId) {
        return dockerClientWrapper.getContainerStats(containerId);
    }


    /**
     * 检查Docker服务是否可用
     *
     * @return true如果可用，false如果不可用
     */
    public boolean isDockerAvailable() {
        return dockerClientWrapper.isDockerAvailable();
    }

    /**
     * 获取容器日志
     *
     * @param containerId 容器ID
     * @param tail        要获取的日志行数
     * @param follow      是否持续获取新日志
     * @param timestamps  是否包含时间戳
     * @return 日志内容
     */
    public String getContainerLogs(String containerId, int tail, boolean follow, boolean timestamps) {
        return dockerClientWrapper.getContainerLogs(containerId, tail, follow, timestamps);
    }


    /**
     * 删除Docker镜像
     *
     * @param imageId 镜像ID
     */
    public void removeImage(String imageId) {
        dockerClientWrapper.removeImage(imageId);
    }

    /**
     * 获取镜像详细信息
     *
     * @param imageId 镜像ID
     * @return 镜像详细信息
     */
    public InspectImageResponse getInspectImage(String imageId) {
        return dockerClientWrapper.getInspectImage(imageId);
    }

    /**
     * 获取镜像详细信息
     *
     * @return 镜像详细信息
     */
    public InspectContainerResponse inspectContainerCmd(String containerId) {
        return dockerClientWrapper.inspectContainerCmd(containerId);
    }


    /**
     * 重命名容器
     *
     * @param containerId 容器ID
     * @param newName     新的容器名称
     */
    public void renameContainer(String containerId, String newName) {
        dockerClientWrapper.renameContainer(containerId, newName);
    }


    /**
     * 获取docker的网络信息
     *
     * @return List<Network>
     */
    public List<Network> listNetworks() {
        return dockerClientWrapper.listNetworks();
    }


    /**
     * 取消镜像拉取操作
     *
     * @param imageWithTag 镜像名称和标签
     */
    public void cancelPullImage(String imageWithTag) {
        try {
            // 获取当前正在执行的拉取操作
            List<Thread> threads = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().startsWith("docker-pull-")).collect(Collectors.toList());

            if (threads.isEmpty()) {
                log.warn("未找到正在执行的镜像拉取线程");
                return;
            }

            // 中断所有相关的拉取线程
            for (Thread thread : threads) {
                log.info("正在中断镜像拉取线程: {}", thread.getName());
                thread.interrupt();
            }

            // 查找并终止 skopeo 进程
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("pkill", "-f", "skopeo copy.*" + imageWithTag);
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    log.info("成功终止 skopeo 进程");
                } else {
                    log.warn("终止 skopeo 进程失败，退出码: {}", exitCode);
                }
            } catch (Exception e) {
                log.error("终止 skopeo 进程时出错: {}", e.getMessage());
            }

            log.info("已取消镜像拉取操作: {}", imageWithTag);
        } catch (Exception e) {
            log.error("取消镜像拉取操作失败: {}", e.getMessage(), e);
            throw new RuntimeException("取消镜像拉取操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取本地镜像的创建时间
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @return 镜像创建时间字符串
     */
    public String getLocalImageCreateTime(String imageName, String tag) {
        try {
            String fullImageName = tag != null && !tag.isEmpty() ? imageName + ":" + tag : imageName;
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("inspect");
            command.add("--format");
            command.add("{{ .Created }}");
            command.add(fullImageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("获取本地镜像创建时间失败，退出码: " + exitCode);
            }

            return output.toString().trim();
        } catch (Exception e) {
            log.error("获取本地镜像创建时间失败: {}", e.getMessage());
            throw new RuntimeException("获取本地镜像创建时间失败: " + e.getMessage());
        }
    }

    /**
     * 获取远程镜像的创建时间
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @return 镜像创建时间字符串
     */
    public String getRemoteImageCreateTime(String imageName, String tag) {
        String fullImageName = tag != null && !tag.isEmpty() ? imageName + ":" + tag : imageName;

        // 方法1：使用 skopeo
        try {
            List<String> command = new ArrayList<>();
            command.add("skopeo");
            command.add("inspect");

            // 检查当前系统架构
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
                log.info("检测到Mac ARM架构，强制指定arm64/linux架构参数");
                command.add("--override-arch");
                command.add("arm64");
                command.add("--override-os");
                command.add("linux");
            }

            command.add("--insecure-policy");
            command.add("--tls-verify=false");
            command.add("docker://" + fullImageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 如果代理配置不为空就使用代理
            String proxyUrl = appConfig.getProxyUrl();
            if (proxyUrl != null && !proxyUrl.isEmpty()) {
                processBuilder.environment().put("HTTP_PROXY", proxyUrl);
                processBuilder.environment().put("HTTPS_PROXY", proxyUrl);
            }
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // 使用 jq 解析 JSON 输出
                List<String> jqCommand = new ArrayList<>();
                jqCommand.add("jq");
                jqCommand.add("-r");
                jqCommand.add(".Created");

                ProcessBuilder jqProcessBuilder = new ProcessBuilder(jqCommand);
                Process jqProcess = jqProcessBuilder.start();

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(jqProcess.getOutputStream()))) {
                    writer.write(output.toString());
                    writer.flush();
                }

                StringBuilder jqOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jqProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jqOutput.append(line);
                    }
                }

                int jqExitCode = jqProcess.waitFor();
                if (jqExitCode == 0) {
                    String result = jqOutput.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("使用 skopeo 获取远程镜像时间失败: {}", e.getMessage());
        }

        // 如果 skopeo 失败，尝试使用 regctl
        try {
            List<String> command = new ArrayList<>();
            command.add("regctl");
            command.add("image");
            command.add("inspect");
            command.add(fullImageName);
            command.add("--format");
            command.add("{{.Created}}");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 如果代理配置不为空就使用代理
            String proxyUrl = appConfig.getProxyUrl();
            if (proxyUrl != null && !proxyUrl.isEmpty()) {
                processBuilder.environment().put("HTTPS_PROXY", proxyUrl);
            }
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String result = output.toString().trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("使用 regctl 获取远程镜像时间失败: {}", e.getMessage());
        }

        // 如果两种方法都失败，返回本地镜像时间
        log.warn("获取远程镜像时间失败，返回本地镜像时间");
        return getLocalImageCreateTime(imageName, tag);
    }

    /**
     * 配置容器创建命令
     *
     * @param request 容器创建请求
     * @return 配置好的容器创建命令
     */
    public CreateContainerResponse configureContainerCmd(ContainerCreateRequest request) {
        String imageName = request.getImage();
        // 4. 构建HostConfig
        HostConfig hostConfig = new HostConfig();

        // 设置自动删除暂时没必要
//            hostConfig.withAutoRemove(request.isAutoRemove());

        // 设置重启策略
        if (request.getRestartPolicy() != null) {
            hostConfig.withRestartPolicy(request.getRestartPolicy());
        }

        // 设置端口映射

        if (request.getPortBindings() != null) {
            hostConfig.withPortBindings(request.getPortBindings());
        }

        // 设置卷挂载
        if (request.getBinds() != null) {
            hostConfig.withBinds(request.getBinds());
        }

        // 设置其他HostConfig配置
        if (request.getNetworkMode() != null) {
            hostConfig.withNetworkMode(request.getNetworkMode());
        }

        if (request.getMemory() != null) {
            hostConfig.withMemory(request.getMemory());
        }
        if (request.getMemorySwap() != null) {
            hostConfig.withMemorySwap(request.getMemorySwap());
        }
        if (request.getCpuShares() != null) {
            hostConfig.withCpuShares(request.getCpuShares());
        }
        if (request.getCpusetCpus() != null) {
            hostConfig.withCpusetCpus(request.getCpusetCpus());
        }
        if (request.getCpuPeriod() != null) {
            hostConfig.withCpuPeriod(request.getCpuPeriod());
        }
        if (request.getCpuQuota() != null) {
            hostConfig.withCpuQuota(request.getCpuQuota());
        }

        if (request.getShmSize() != null) {
            hostConfig.withShmSize(Long.valueOf(request.getShmSize()));
        }

        if (request.getDevices() != null) {
            hostConfig.withDevices(request.getDevices());
        }
        if (request.getUlimits() != null) {
            hostConfig.withUlimits(request.getUlimits());
        }
        if (request.getExtraHosts() != null && !request.getExtraHosts().isEmpty()) {
            hostConfig.withExtraHosts(String.valueOf(request.getExtraHosts()));
        }
        if (request.getDns() != null && !request.getDns().isEmpty()) {
            hostConfig.withDns(request.getDns());
        }
        if (request.getDnsSearch() != null && !request.getDnsSearch().isEmpty()) {
            hostConfig.withDnsSearch(request.getDnsSearch());
        }
        CreateContainerCmd createContainerCmd = dockerClientWrapper.createContainerCmd(imageName).withName(request.getName()).withHostConfig(hostConfig);
        // 设置环境变量
        if (request.getEnv() != null && !request.getEnv().isEmpty()) {
            createContainerCmd.withEnv(request.getEnv());
        }
        // 设置其他配置
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            createContainerCmd.withLabels(request.getLabels());
        }
        if (request.getCmd() != null && !request.getCmd().isEmpty()) {
            createContainerCmd.withCmd(request.getCmd());
        }
        // 如果指定了Entrypoint，则使用；否则保留镜像的默认Entrypoint
        if (request.getEntrypoint() != null && !request.getEntrypoint().isEmpty()) {
            createContainerCmd.withEntrypoint(request.getEntrypoint());
        }
        if (request.getWorkingDir() != null) {
            createContainerCmd.withWorkingDir(request.getWorkingDir());
        }
        if (request.getUser() != null) {
            createContainerCmd.withUser(request.getUser());
        }
        createContainerCmd.withPrivileged(request.getPrivileged());

        // 6. 创建容器
        return createContainerCmd.exec();
    }


    // FIXME: 功能尚未完成，暂不启用
    public Network inspectNetwork(String networkId) {
        return null;
    }

    // FIXME: 功能尚未完成，暂不启用
    public String createNetwork(String name, String driver, Map<String, Object> ipamConfig) {
        return null;
    }

    // FIXME: 功能尚未完成，暂不启用
    public void removeNetwork(String networkId) {

    }

    // FIXME: 功能尚未完成，暂不启用
    public void connectContainerToNetwork(String containerId, String networkId, String ipAddress, String[] aliases) {

    }

    // FIXME: 功能尚未完成，暂不启用
    public void disconnectContainerFromNetwork(String containerId, String networkId, boolean force) {

    }




    /**
     * 使用skopeo从远程仓库拉取镜像到宿主机Docker
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 进度回调
     */
    public void pullImageWithSkopeo(String image, String tag, PullImageCallback callback) {
        log.info("开始使用 skopeo 拉取镜像: {}:{} ", image, tag);
        try {
            // 构建完整的镜像名称
            String fullImageName = tag != null && !tag.isEmpty() ? image + ":" + tag : image;
            // 构建 skopeo 命令
            List<String> command = new ArrayList<>();
            command.add("skopeo");
            command.add("copy");
            // 添加源和目标
            // 检查当前系统架构
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            // 只有在Mac的ARM架构(M系列芯片)上才需要指定架构参数
            if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
                log.info("检测到Mac ARM架构，强制指定arm64/linux架构参数");
                // 强制指定为amd64架构和linux系统，解决在Mac ARM芯片上的兼容性问题
                command.add("--override-arch");
                command.add("arm64");
                command.add("--override-os");
                command.add("linux");
            }
            command.add("docker://" + fullImageName);
            command.add("docker-daemon:" + fullImageName);
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 设置代理（如果启用）
            String proxyUrl = appConfig.getProxyUrl();
            boolean isProxy = proxyUrl != null && !proxyUrl.isEmpty();
            if (isProxy) {
                processBuilder.redirectErrorStream(true);
                processBuilder.environment().put("HTTP_PROXY", proxyUrl);
                processBuilder.environment().put("HTTPS_PROXY", proxyUrl);
            }
            processBuilder.redirectErrorStream(true);
            // 打印完整命令行
            log.info("执行命令: {},是否使用代理 {}", String.join(" ", command), isProxy);
            Process process = processBuilder.start();
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int progress = 0;
                while ((line = reader.readLine()) != null) {
                    log.info("skopeo: {}", line);
                    if (callback != null) {
                        // 解析进度
                        if (line.contains("Getting image source signatures")) {
                            progress = 10;
                        } else if (line.contains("Copying blob")) {
                            progress = 30;
                        } else if (line.contains("Copying config")) {
                            progress = 70;
                        } else if (line.contains("Writing manifest")) {
                            progress = 90;
                        } else if (line.contains("Storing signatures")) {
                            progress = 100;
                        }
                        callback.onProgress(progress, line);
                    }
                }
            }
            // 等待命令完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = "skopeo 命令执行失败，退出码: " + exitCode;
                if (callback != null) {
                    callback.onError(error);
                }
                throw new RuntimeException(error);
            }
            log.info("镜像拉取完成: {}", fullImageName);
            if (callback != null) {
                callback.onComplete();
            }
        } catch (Exception e) {
            log.error("使用 skopeo 拉取镜像失败: {}", e.getMessage(), e);
            if (callback != null) {
                callback.onError(e.getMessage());
            }
            throw new RuntimeException("使用 skopeo 拉取镜像失败: " + e.getMessage());
        }
    }

    /**
     * 拉取Docker镜像（支持WebSocket回调）
     *
     * @param image    镜像名称
     * @param tag      镜像标签
     * @param callback 进度回调
     */
    public void pullImage(String image, String tag, PullImageCallback callback) {
        /**
         * 这里其实需要多种返回，使用代理，使用镜像加速，什么都不用
         */
        pullImageWithSkopeo(image, tag, callback);
    }


}



