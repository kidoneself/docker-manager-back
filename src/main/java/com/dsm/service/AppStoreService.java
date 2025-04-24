package com.dsm.service;

import com.dsm.pojo.entity.AppStoreApp;
import java.util.List;
import java.util.Map;

/**
 * 应用商店服务接口
 * 提供应用商店相关的业务逻辑
 */
public interface AppStoreService {
    /**
     * 获取应用列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param search 搜索关键词
     * @param category 分类
     * @return 应用列表和总数
     */
    Map<String, Object> getAppList(int page, int pageSize, String search, String category);

    /**
     * 获取应用详情
     * @param id 应用ID
     * @return 应用详情
     */
    AppStoreApp getAppDetail(String id);

    /**
     * 安装应用
     * @param id 应用ID
     * @param selectedServices 选中的服务
     * @param envValues 环境变量值
     * @return 安装任务ID
     */
    String installApp(String id, List<String> selectedServices, Map<String, String> envValues);

    /**
     * 获取安装状态
     * @param taskId 任务ID
     * @return 安装状态信息
     */
    Map<String, Object> getInstallStatus(String taskId);

    /**
     * 取消安装
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelInstall(String taskId);

    /**
     * 下载文件
     * 根据提供的文件URL下载文件到指定目录
     * 下载路径根据当前环境自动选择：
     * - 开发环境：下载到 app 目录
     * - 生产环境：下载到 /app/template 目录
     *
     * @param fileUrl 要下载的文件URL
     * @return 下载结果，包含以下字段：
     *         - success: 是否成功
     *         - message: 结果消息
     *         - path: 下载后的文件路径（仅在成功时返回）
     */
    Map<String, Object> downloadFile(String fileUrl);
} 