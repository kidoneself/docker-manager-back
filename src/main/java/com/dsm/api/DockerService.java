package com.dsm.api;

import com.dsm.config.AppConfig;
import com.dsm.exception.BusinessException;
import com.dsm.pojo.request.ContainerCreateRequest;
import com.dsm.utils.DockerComposeUtils;
import com.dsm.websocket.callback.PullImageCallback;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Docker服务类，提供与Docker引擎交互的各种操作
 * 包括容器管理、镜像管理、日志查看等功能
 */
@Slf4j
@Service
public class DockerService {
    private final DockerClient dockerClient;

    @Autowired
    private AppConfig appConfig;

    /**
     * 构造函数，初始化Docker客户端连接
     * 使用Unix socket连接到本地Docker守护进程
     */
    public DockerService() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("unix:///var/run/docker.sock").build();

        // 创建带代理的 HTTP 客户端
        ApacheDockerHttpClient.Builder httpClientBuilder = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).maxConnections(100).connectionTimeout(Duration.ofSeconds(30)).responseTimeout(Duration.ofSeconds(45));


        DockerHttpClient httpClient = httpClientBuilder.build();

        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * 获取所有容器列表
     *
     * @return 容器列表，包括运行中和已停止的容器
     */
    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().withShowAll(true).exec();
    }

    /**
     * 获取所有镜像列表
     *
     * @return 镜像列表，包括所有本地镜像
     */
    public List<Image> listImages() {
        return dockerClient.listImagesCmd().withShowAll(true).exec();
    }


    /**
     * 启动指定容器
     *
     * @param containerId 容器ID
     * @throws RuntimeException 如果启动失败
     */
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.error("启动容器失败: {}", e.getMessage());
            throw new BusinessException("启动容器失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定容器
     *
     * @param containerId 容器ID
     * @throws RuntimeException 如果停止失败
     */
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("停止容器成功: {}", containerId);
        } catch (Exception e) {
            // 检查是否为304状态码（Not Modified），表示容器已经停止
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Status 304")) {
                log.info("容器已经处于停止状态: {}", containerId);
                return; // 容器已经停止，视为成功
            }

            // 其他错误则正常抛出
            log.error("停止容器失败: {}", e.getMessage());
            throw new RuntimeException("Failed to stop container", e);
        }
    }

    /**
     * 重启指定容器
     *
     * @param containerId 容器ID
     */
    public void restartContainer(String containerId) {
        executeDockerCommand(() -> {
            dockerClient.restartContainerCmd(containerId).exec();
            log.info("重启容器成功: {}", containerId);
        }, "重启容器", containerId);
    }

    /**
     * 通过ID或名称查找容器
     *
     * @param idOrName 容器ID或名称
     * @return 找到的容器对象，如果未找到则返回null
     */
    private Container findContainerByIdOrName(String idOrName) {
        // 尝试通过ID查找
        log.debug("尝试通过ID查找容器: {}", idOrName);
        List<Container> containersById = dockerClient.listContainersCmd().withShowAll(true).withIdFilter(List.of(idOrName)).exec();

        if (!containersById.isEmpty()) {
            Container container = containersById.get(0);
            log.info("通过ID找到容器: {}", container.getId());
            return container;
        }

        // 如果通过ID未找到，尝试通过名称查找
        log.debug("通过ID未找到容器，尝试通过名称查找: {}", idOrName);

        // Docker API容器名称带有前缀斜杠
        String nameWithSlash = "/" + idOrName;

        List<Container> allContainers = dockerClient.listContainersCmd().withShowAll(true).exec();

        Container container = allContainers.stream().filter(c -> c.getNames() != null && java.util.Arrays.stream(c.getNames()).anyMatch(name -> name.equals(nameWithSlash))).findFirst().orElse(null);

        if (container != null) {
            log.info("通过名称找到容器: {} (ID: {})", idOrName, container.getId());
        }

        return container;
    }

    /**
     * 删除指定容器
     *
     * @param containerId 容器ID
     */
    public void removeContainer(String containerId) {
        executeDockerCommand(() -> {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("删除容器成功: {}", containerId);
        }, "删除容器", containerId);
    }

    /**
     * 获取容器的统计信息
     *
     * @param containerId 容器ID
     * @return 容器统计信息对象
     */
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

    /**
     * 执行Docker命令并处理异常
     *
     * @param command       要执行的命令
     * @param operationName 操作名称（用于日志）
     * @param containerId   容器ID或名称
     */
    private void executeDockerCommand(Runnable command, String operationName, String containerId) {
        try {
            command.run();
        } catch (Exception e) {
            log.error("{}失败: {}", operationName, e.getMessage());
            throw new RuntimeException("Failed to " + operationName.toLowerCase(), e);
        }
    }

    /**
     * 执行Docker命令并处理异常，带返回值
     *
     * @param supplier      要执行的命令
     * @param operationName 操作名称（用于日志）
     * @param containerId   容器ID或名称
     * @param <T>           返回值类型
     * @return 命令执行结果
     */
    private <T> T executeDockerCommandWithResult(Supplier<T> supplier, String operationName, String containerId) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("{}失败: {}", operationName, e.getMessage());
            throw new RuntimeException("Failed to " + operationName.toLowerCase(), e);
        }
    }

    /**
     * 根据模板ID获取镜像名称
     *
     * @param templateId 模板ID
     * @return 镜像名称
     */
    private String getImageNameByTemplateId(String templateId) {
        // 这里应该根据模板ID从数据库或其他存储中获取对应的镜像名称
        // 目前先返回一个默认值
        return "nginx:latest";
    }

    /**
     * 检查Docker服务是否可用
     *
     * @return true如果可用，false如果不可用
     */
    public boolean isDockerAvailable() {
        try {
            // 尝试ping Docker服务
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            log.error("Docker服务不可用: {}", e.getMessage());
            return false;
        }
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
        try {
            LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId).withTail(tail).withFollowStream(follow).withTimestamps(timestamps).withStdOut(true).withStdErr(true);

            StringBuilder logs = new StringBuilder();
            logContainerCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    logs.append(new String(frame.getPayload())).append("\n");
                }
            }).awaitCompletion();

            return logs.toString();
        } catch (Exception e) {
            throw new RuntimeException("获取容器日志失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取宿主机架构信息
     *
     * @return 架构信息，如 "linux/amd64" 或 "linux/arm64"
     */
    private String getHostArchitecture() {
        try {
            // 执行 uname -m 命令获取架构信息
            Process process = Runtime.getRuntime().exec("uname -m");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String arch = reader.readLine().trim().toLowerCase();
            // 标准化架构名称
            String platform = "linux/";
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                platform += "arm64";
            } else if (arch.contains("x86_64") || arch.contains("amd64")) {
                platform += "amd64";
            } else {
                platform += arch;
            }
            log.info("检测到宿主机架构: {}", platform);
            return platform;
        } catch (Exception e) {
            log.error("获取宿主机架构失败: {}", e.getMessage());
            // 如果获取失败，使用默认的 amd64 架构
            return "linux/amd64";
        }
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
//                System.out.println("当前代理为：" + proxyUrl);
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
        // 使用 skopeo 替代原来的实现
        pullImageWithSkopeo(image, tag, callback);
    }

    /**
     * 删除Docker镜像
     *
     * @param imageId 镜像ID
     */
    public void removeImage(String imageId) {
        executeDockerCommand(() -> {
            dockerClient.removeImageCmd(imageId).withForce(true).exec();
            log.info("删除镜像成功: {}", imageId);
        }, "删除镜像", imageId);
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
     * 获取镜像详细信息
     *
     * @param imageId 镜像ID
     * @return 镜像详细信息
     */
    public InspectImageResponse getInspectImage(String imageId) {
        return dockerClient.inspectImageCmd(imageId).exec();
    }

    /**
     * 获取镜像详细信息
     *
     * @return 镜像详细信息
     */
    public InspectContainerResponse inspectContainerCmd(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).exec();
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
     * 比较本地和远程镜像的创建时间
     *
     * @param imageName 镜像名称
     * @param tag       镜像标签
     * @return 如果远程镜像更新则返回true，否则返回false
     */
    public boolean isRemoteImageNewer(String imageName, String tag) {
        try {
            String localTime = getLocalImageCreateTime(imageName, tag);
            String remoteTime = getRemoteImageCreateTime(imageName, tag);

            // 将时间字符串转换为Instant对象进行比较
            Instant localInstant = Instant.parse(localTime);
            Instant remoteInstant = Instant.parse(remoteTime);

            // 如果远程时间晚于本地时间，说明需要更新
            return remoteInstant.isAfter(localInstant);
        } catch (Exception e) {
            log.error("比较镜像时间失败: {}", e.getMessage());
            throw new RuntimeException("比较镜像时间失败: " + e.getMessage());
        }
    }

    /**
     * 重命名容器
     *
     * @param containerId 容器ID
     * @param newName     新的容器名称
     */
    public void renameContainer(String containerId, String newName) {
        executeDockerCommand(() -> {
            dockerClient.renameContainerCmd(containerId).withName(newName).exec();
            log.info("重命名容器成功: {} -> {}", containerId, newName);
        }, "重命名容器", containerId);
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
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageName).withName(request.getName()).withHostConfig(hostConfig);
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

    public List<Network> listNetworks() {
        return dockerClient.listNetworksCmd().exec();
    }

    public Network inspectNetwork(String networkId) {
        return null;
    }

    public String createNetwork(String name, String driver, Map<String, Object> ipamConfig) {
        return null;
    }

    public void removeNetwork(String networkId) {

    }

    public void connectContainerToNetwork(String containerId, String networkId, String ipAddress, String[] aliases) {

    }

    public void disconnectContainerFromNetwork(String containerId, String networkId, boolean force) {

    }

    /**
     * 执行Docker Compose文件
     *
     * @param composeFilePath Docker Compose文件路径
     * @throws FileNotFoundException 如果文件不存在
     */
    public void executeDockerCompose(String composeFilePath) throws FileNotFoundException {
        DockerComposeUtils composeUtils = new DockerComposeUtils();
        Map<String, DockerComposeUtils.ServiceConfig> serviceConfigs = composeUtils.parseDockerCompose(composeFilePath);

        for (Map.Entry<String, DockerComposeUtils.ServiceConfig> entry : serviceConfigs.entrySet()) {
            String serviceName = entry.getKey();
            DockerComposeUtils.ServiceConfig config = entry.getValue();

            try {
                // 拉取镜像
                dockerClient.pullImageCmd(config.getImage()).start().awaitCompletion();

                // 创建容器
                CreateContainerResponse container = dockerClient.createContainerCmd(config.getImage())
                        .withName(config.getContainerName())
                        .withPortBindings(config.getPortBindings().toArray(new PortBinding[0]))
                        .withVolumes(config.getVolumes().toArray(new Volume[0]))
                        .exec();

                // 启动容器
                dockerClient.startContainerCmd(container.getId()).exec();

                log.info("Service {} (Container {}) started successfully", serviceName, config.getContainerName());
            } catch (Exception e) {
                log.error("Error starting service {} (Container {}): {}",
                        serviceName, config.getContainerName(), e.getMessage());
                throw new RuntimeException("Failed to start service " + serviceName, e);
            }
        }
    }

    /**
     * 执行Docker Compose文件（带环境变量文件）
     *
     * @param composeFilePath Docker Compose文件路径
     * @param envFilePath     环境变量文件路径
     * @throws FileNotFoundException 如果文件不存在
     */
    public void executeDockerCompose(String composeFilePath, String envFilePath) throws FileNotFoundException {
        DockerComposeUtils composeUtils = new DockerComposeUtils();
        composeUtils.loadEnvFile(envFilePath);
        executeDockerCompose(composeFilePath);
    }
}



