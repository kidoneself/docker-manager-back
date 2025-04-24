package com.dsm.service.impl;

import com.dsm.pojo.entity.*;
import com.dsm.pojo.entity.template.TemplateXml;
import com.dsm.service.AppStoreService;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 应用商店服务实现类
 * 实现应用商店相关的业务逻辑
 */
@Service
public class AppStoreServiceImpl implements AppStoreService {
    
    /**
     * 当前运行环境
     * 用于判断是开发环境还是生产环境
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 开发环境下的文件下载路径
     * 从配置文件中读取
     */
    @Value("${file.download.path.dev}")
    private String devDownloadPath;

    /**
     * 生产环境下的文件下载路径
     * 从配置文件中读取
     */
    @Value("${file.download.path.prod}")
    private String prodDownloadPath;

    private final Map<String, AppInstallTask> taskMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public Map<String, Object> getAppList(int page, int pageSize, String search, String category) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 根据环境获取目录路径
            String downloadPath = activeProfile.equals("prod") ? prodDownloadPath : devDownloadPath;
            Path targetDir = Paths.get(downloadPath);
            
            // 确保目录存在
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 获取目录下所有XML文件
            List<Path> xmlFiles = Files.walk(targetDir)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .collect(Collectors.toList());

            // 转换为Map列表并解析XML
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (Path xmlFile : xmlFiles) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", xmlFile.getFileName().toString());
                fileInfo.put("path", xmlFile.toString());
                fileInfo.put("size", Files.size(xmlFile));
                fileInfo.put("lastModified", Files.getLastModifiedTime(xmlFile).toMillis());

                // 解析XML文件
                try {
                    String xmlContent = Files.readString(xmlFile);
                    TemplateXml template = xmlMapper.readValue(xmlContent, TemplateXml.class);
                    fileInfo.put("template", template);
                } catch (Exception e) {
                    System.err.println("解析XML文件失败: " + xmlFile);
                    e.printStackTrace();
                }

                fileList.add(fileInfo);
            }

            // 打印文件信息
            System.out.println("当前目录下的XML文件：");
            for (Map<String, Object> fileInfo : fileList) {
                System.out.println("文件名: " + fileInfo.get("name"));
                System.out.println("路径: " + fileInfo.get("path"));
                System.out.println("大小: " + fileInfo.get("size") + " bytes");
                System.out.println("最后修改时间: " + new Date((Long)fileInfo.get("lastModified")));
                if (fileInfo.get("template") != null) {
                    TemplateXml template = (TemplateXml) fileInfo.get("template");
                    System.out.println("模板名称: " + template.getMeta().getName());
                    System.out.println("版本: " + template.getMeta().getVersion());
                    System.out.println("描述: " + template.getMeta().getDescription());
                    System.out.println("作者: " + template.getMeta().getAuthor());
                    System.out.println("更新日期: " + template.getMeta().getUpdated());
                    System.out.println("配置字段数量: " + template.getFields().size());
                    System.out.println("服务数量: " + template.getServices().size());
                    
                    // 打印配置字段
                    System.out.println("\n配置字段:");
                    for (TemplateXml.Field field : template.getFields()) {
                        System.out.println("  - " + field.getKey() + ": " + field.getLabel() + " (默认值: " + field.getDefaultValue() + ")");
                    }
                    
                    // 打印服务信息
                    System.out.println("\n服务列表:");
                    for (TemplateXml.Service service : template.getServices()) {
                        System.out.println("  - " + service.getName() + " (" + service.getImage() + ")");
                    }
                }
                System.out.println("------------------------");
            }

            result.put("total", fileList.size());
            result.put("list", fileList);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("total", 0);
            result.put("list", new ArrayList<>());
        }
        return result;
    }

    @Override
    public AppStoreApp getAppDetail(String id) {
        // TODO: 实现获取应用详情逻辑
        return null;
    }

    @Override
    public String installApp(String id, List<String> selectedServices, Map<String, String> envValues) {
        String taskId = UUID.randomUUID().toString();
        AppInstallTask task = new AppInstallTask();
        task.setTaskId(taskId);
        task.setAppId(id);
        task.setStatus("waiting");
        task.setSelectedServices(selectedServices);
        task.setEnvValues(envValues);
        task.setServiceStatuses(new ArrayList<>());
        task.setLogs(new ArrayList<>());
        task.setCreatedResources(new ArrayList<>());
        
        // 初始化服务状态
        for (String service : selectedServices) {
            ServiceInstallStatus status = new ServiceInstallStatus();
            status.setServiceName(service);
            status.setStatus("waiting");
            status.setProgress(0);
            task.getServiceStatuses().add(status);
        }
        
        taskMap.put(taskId, task);
        
        // 异步执行安装任务
        executorService.submit(() -> executeInstallTask(task));
        
        return taskId;
    }

    private void executeInstallTask(AppInstallTask task) {
        try {
            task.setStatus("running");
            addLog(task, "info", "开始安装应用...");
            
            // 按顺序安装每个服务
            for (ServiceInstallStatus serviceStatus : task.getServiceStatuses()) {
                if (task.getStatus().equals("cancelled")) {
                    break;
                }
                
                serviceStatus.setStatus("running");
                addLog(task, "info", "开始安装服务: " + serviceStatus.getServiceName());
                
                // 1. 创建网络（如果需要）
                String networkId = createNetwork(task);
                if (networkId != null) {
                    addDockerResource(task, "network", networkId, "app-network");
                }
                
                // 2. 创建卷（如果需要）
                String volumeId = createVolume(task);
                if (volumeId != null) {
                    addDockerResource(task, "volume", volumeId, "app-volume");
                }
                
                // 3. 创建并启动容器
                String containerId = createContainer(task, serviceStatus);
                if (containerId != null) {
                    serviceStatus.setContainerId(containerId);
                    addDockerResource(task, "container", containerId, serviceStatus.getServiceName());
                }
                
                serviceStatus.setStatus("completed");
                serviceStatus.setProgress(100);
                addLog(task, "success", "服务安装完成: " + serviceStatus.getServiceName());
            }
            
            if (task.getStatus().equals("cancelled")) {
                addLog(task, "warning", "安装任务已被取消");
            } else {
                task.setStatus("completed");
                addLog(task, "success", "应用安装完成");
            }
        } catch (Exception e) {
            task.setStatus("failed");
            addLog(task, "error", "安装失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getInstallStatus(String taskId) {
        AppInstallTask task = taskMap.get(taskId);
        if (task == null) {
            return Map.of(
                "status", "not_found",
                "logs", new ArrayList<>(),
                "services", new ArrayList<>()
            );
        }
        
        return Map.of(
            "status", task.getStatus(),
            "logs", task.getLogs(),
            "services", task.getServiceStatuses()
        );
    }

    @Override
    public boolean cancelInstall(String taskId) {
        AppInstallTask task = taskMap.get(taskId);
        if (task == null || !task.getStatus().equals("running")) {
            return false;
        }
        
        task.setStatus("cancelled");
        addLog(task, "info", "正在取消安装...");
        
        // 按创建顺序的逆序清理资源
        List<DockerResource> resources = new ArrayList<>(task.getCreatedResources());
        Collections.reverse(resources);
        
        for (DockerResource resource : resources) {
            try {
                switch (resource.getType()) {
                    case "container":
                        stopAndRemoveContainer(resource.getId());
                        break;
                    case "network":
                        removeNetwork(resource.getId());
                        break;
                    case "volume":
                        removeVolume(resource.getId());
                        break;
                }
                addLog(task, "info", "已清理资源: " + resource.getType() + " - " + resource.getName());
            } catch (Exception e) {
                addLog(task, "error", "清理资源失败: " + resource.getType() + " - " + resource.getName() + ": " + e.getMessage());
            }
        }
        
        return true;
    }
    
    private void addLog(AppInstallTask task, String type, String message) {
        InstallLog log = new InstallLog();
        log.setType(type);
        log.setMessage(message);
        log.setTime(new Date().toString());
        task.getLogs().add(log);
    }
    
    private void addDockerResource(AppInstallTask task, String type, String id, String name) {
        DockerResource resource = new DockerResource();
        resource.setType(type);
        resource.setId(id);
        resource.setName(name);
        task.getCreatedResources().add(resource);
    }
    
    // 以下方法需要实现具体的Docker操作
    private String createNetwork(AppInstallTask task) {
        // TODO: 实现创建网络
        return null;
    }
    
    private String createVolume(AppInstallTask task) {
        // TODO: 实现创建卷
        return null;
    }
    
    private String createContainer(AppInstallTask task, ServiceInstallStatus serviceStatus) {
        // TODO: 实现创建容器
        return null;
    }
    
    private void stopAndRemoveContainer(String containerId) {
        // TODO: 实现停止和删除容器
    }
    
    private void removeNetwork(String networkId) {
        // TODO: 实现删除网络
    }
    
    private void removeVolume(String volumeId) {
        // TODO: 实现删除卷
    }

    /**
     * 读取指定目录下的所有XML文件
     * @return 包含所有XML文件信息的列表
     */
    public List<Map<String, Object>> listXmlFiles() {
        try {
            // 根据环境获取目录路径
            String downloadPath = activeProfile.equals("prod") ? prodDownloadPath : devDownloadPath;
            Path targetDir = Paths.get(downloadPath);
            
            // 确保目录存在
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 获取目录下所有XML文件
            List<Path> xmlFiles = Files.walk(targetDir)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .collect(Collectors.toList());

            // 转换为Map列表
            List<Map<String, Object>> result = new ArrayList<>();
            for (Path xmlFile : xmlFiles) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", xmlFile.getFileName().toString());
                fileInfo.put("path", xmlFile.toString());
                fileInfo.put("size", Files.size(xmlFile));
                fileInfo.put("lastModified", Files.getLastModifiedTime(xmlFile).toMillis());
                result.add(fileInfo);
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 下载文件实现
     * 根据当前环境选择下载路径，并下载文件到指定目录
     *
     * @param fileUrl 要下载的文件URL
     * @return 下载结果，包含以下字段：
     *         - success: 是否成功
     *         - message: 结果消息
     *         - path: 下载后的文件路径（仅在成功时返回）
     */
    @Override
    public Map<String, Object> downloadFile(String fileUrl) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 根据环境获取下载路径
            String downloadPath = activeProfile.equals("prod") ? prodDownloadPath : devDownloadPath;
            Path targetDir = Paths.get(downloadPath);
            
            // 确保目录存在
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 从URL获取文件名
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path targetPath = targetDir.resolve(fileName);

            // 使用 HttpURLConnection 下载文件
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                connection.disconnect();
            }

            // 下载完成后，列出目录下的所有XML文件
            List<Map<String, Object>> xmlFiles = listXmlFiles();
            System.out.println("当前目录下的XML文件：");
            for (Map<String, Object> fileInfo : xmlFiles) {
                System.out.println("文件名: " + fileInfo.get("name"));
                System.out.println("路径: " + fileInfo.get("path"));
                System.out.println("大小: " + fileInfo.get("size") + " bytes");
                System.out.println("最后修改时间: " + new Date((Long)fileInfo.get("lastModified")));
                System.out.println("------------------------");
            }

            result.put("success", true);
            result.put("message", "文件下载成功");
            result.put("path", targetPath.toString());
            result.put("xmlFiles", xmlFiles);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "文件下载失败: " + e.getMessage());
        }
        return result;
    }
} 