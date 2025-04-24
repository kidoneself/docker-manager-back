package com.dsm.service;

import com.dsm.pojo.dto.NetworkInfoDTO;

import java.util.List;
import java.util.Map;

public interface DockerNetworkService {
    
    /**
     * 获取所有网络列表
     * @return 网络列表
     * @ 获取失败时抛出异常
     */
    List<NetworkInfoDTO> listNetworks() ;

    /**
     * 获取网络详情
     * @param networkId 网络ID
     * @return 网络详情
     * @ 获取失败时抛出异常
     */
    Map<String, Object> getNetworkDetail(String networkId) ;

    /**
     * 创建网络
     * @param networkConfig 网络配置
     * @return 创建的网络ID
     * @ 创建失败时抛出异常
     */
    String createNetwork(Map<String, Object> networkConfig) ;

    /**
     * 删除网络
     * @param networkId 网络ID
     * @ 删除失败时抛出异常
     */
    void removeNetwork(String networkId) ;

    /**
     * 连接容器到网络
     * @param networkId 网络ID
     * @param connectConfig 连接配置
     * @ 连接失败时抛出异常
     */
    void connectContainer(String networkId, Map<String, Object> connectConfig) ;

    /**
     * 断开容器与网络的连接
     * @param networkId 网络ID
     * @param disconnectConfig 断开连接配置
     * @ 断开连接失败时抛出异常
     */
    void disconnectContainer(String networkId, Map<String, Object> disconnectConfig) ;
} 