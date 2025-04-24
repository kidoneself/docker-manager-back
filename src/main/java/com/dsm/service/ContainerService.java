package com.dsm.service;

import com.dsm.pojo.entity.ContainerDetail;
import com.dsm.pojo.request.ContainerCreateRequest;
import com.dsm.pojo.request.ContainerUpdateRequest;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;

import java.util.List;

/**
 * 容器服务接口
 * 定义容器管理的业务逻辑
 */
public interface ContainerService {
    /**
     * 获取所有容器列表
     *
     * @return 容器列表
     */
    List<Container> listContainers();


    /**
     * 删除容器
     *
     * @param containerId 容器ID
     */
    void removeContainer(String containerId);

    /**
     * 获取容器统计信息
     *
     * @param containerId 容器ID
     * @return 容器统计信息
     */
    Statistics getContainerStats(String containerId);

    /**
     * 获取容器配置信息
     *
     * @param containerId 容器ID
     * @return 容器配置信息
     */
    ContainerDetail getContainerConfig(String containerId);

    /**
     * 启动容器
     *
     * @param containerId 容器ID
     */
    void startContainer(String containerId);

    /**
     * 停止容器
     *
     * @param containerId 容器ID
     */
    void stopContainer(String containerId);


    /**
     * 更新容器配置并重新创建
     *
     * @param containerId 原容器ID
     * @param request     新的容器配置请求
     */
    void updateContainer(String containerId, ContainerCreateRequest request);

    /**
     * 根据配置创建新容器
     *
     * @param request 容器配置请求
     * @return 新容器ID
     */
    String createContainerWithConfig(ContainerUpdateRequest request);


    void restartContainer(String id);

    String getContainerLogs(String id, int tail, boolean follow, boolean timestamps);

    /**
     * 创建新容器
     *
     * @param request 容器创建请求
     * @return 容器创建响应
     */
    String createContainer(ContainerCreateRequest request);


}