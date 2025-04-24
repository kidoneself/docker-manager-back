package com.dsm.controller;

import com.dsm.pojo.dto.NetworkInfoDTO;
import com.dsm.service.DockerNetworkService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Tag(name = "Docker网络管理", description = "Docker网络相关的API接口")
@RestController
@RequestMapping("/networks")
public class DockerNetworkController {

    @Resource
    private DockerNetworkService dockerNetworkService;

    @Operation(summary = "获取网络列表", description = "获取所有Docker网络的列表")
    @GetMapping("/list")
    public ApiResponse<List<NetworkInfoDTO>> listNetworks() {
        List<NetworkInfoDTO> networks = dockerNetworkService.listNetworks();
        return ApiResponse.success(networks);
    }

    @Operation(summary = "获取网络详情", description = "根据网络ID获取网络的详细信息")
    @GetMapping("/{networkId}")
    public ApiResponse<Map<String, Object>> getNetworkDetail(@PathVariable String networkId) {
        Map<String, Object> network = dockerNetworkService.getNetworkDetail(networkId);
        return ApiResponse.success(network);
    }

    @Operation(summary = "创建网络", description = "创建一个新的Docker网络")
    @PostMapping("/create")
    public ApiResponse<String> createNetwork(@RequestBody Map<String, Object> networkConfig) {
        String networkId = dockerNetworkService.createNetwork(networkConfig);
        return ApiResponse.success(networkId);
    }

    @Operation(summary = "删除网络", description = "根据网络ID删除指定的Docker网络")
    @DeleteMapping("/{networkId}")
    public ApiResponse<Void> removeNetwork(@PathVariable String networkId) {
        dockerNetworkService.removeNetwork(networkId);
        return ApiResponse.success();
    }

    @Operation(summary = "连接容器到网络", description = "将指定容器连接到指定的网络")
    @PostMapping("/{networkId}/connect")
    public ApiResponse<Void> connectContainer(@PathVariable String networkId, @RequestBody Map<String, Object> connectConfig) {
        dockerNetworkService.connectContainer(networkId, connectConfig);
        return ApiResponse.success();
    }

    @Operation(summary = "断开容器与网络的连接", description = "将指定容器从指定网络中断开连接")
    @PostMapping("/{networkId}/disconnect")
    public ApiResponse<Void> disconnectContainer(@PathVariable String networkId, @RequestBody Map<String, Object> disconnectConfig) {
        dockerNetworkService.disconnectContainer(networkId, disconnectConfig);
        return ApiResponse.success();
    }
} 