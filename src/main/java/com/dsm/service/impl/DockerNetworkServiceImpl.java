package com.dsm.service.impl;

import com.dsm.api.DockerService;
import com.dsm.pojo.dto.IPAMConfigFlatDTO;
import com.dsm.pojo.dto.NetworkInfoDTO;
import com.dsm.service.DockerNetworkService;
import com.github.dockerjava.api.model.Network;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DockerNetworkServiceImpl implements DockerNetworkService {

    @Resource
    private DockerService dockerService;

    @Override
    public List<NetworkInfoDTO> listNetworks() {
        List<Network> networks = dockerService.listNetworks();

        List<NetworkInfoDTO> result = new ArrayList<>();
        for (Network network : networks) {
            NetworkInfoDTO dto = new NetworkInfoDTO();

            // 基本字段
            dto.setId(network.getId());
            dto.setName(network.getName());
            dto.setDriver(network.getDriver());
            dto.setScope(network.getScope());
            dto.setEnableIPv6(network.getEnableIPv6());
            dto.setInternal(network.getInternal());
            dto.setAttachable(network.isAttachable());
            dto.setLabels(network.getLabels());
            dto.setOptions(network.getOptions());

            // IPAM 字段
            Network.Ipam ipam = network.getIpam();
            if (ipam != null) {
                dto.setIpamDriver(ipam.getDriver());
                dto.setIpamOptions(ipam.getOptions());

                // IPAMConfig 转换
                List<IPAMConfigFlatDTO> ipamConfigList = new ArrayList<>();
                if (ipamConfigList.size() > 0) {
                    for (Network.Ipam.Config ipamConfig : ipam.getConfig()) {
                        IPAMConfigFlatDTO ipamConfigDTO = new IPAMConfigFlatDTO();
                        ipamConfigDTO.setSubnet(ipamConfig.getSubnet());
                        ipamConfigDTO.setIpRange(ipamConfig.getIpRange());
                        ipamConfigDTO.setGateway(ipamConfig.getGateway());
                        ipamConfigList.add(ipamConfigDTO);
                    }
                }
                dto.setIpamConfig(ipamConfigList);
            }
            // 添加到结果列表
            result.add(dto);
        }
        return result;
    }

    @Override
    public Map<String, Object> getNetworkDetail(String networkId) {
        Network network = dockerService.inspectNetwork(networkId);
        Map<String, Object> networkInfo = new HashMap<>();
        networkInfo.put("id", network.getId());
        networkInfo.put("name", network.getName());
        networkInfo.put("driver", network.getDriver());
        networkInfo.put("scope", network.getScope());
        networkInfo.put("ipam", network.getIpam());
        networkInfo.put("containers", network.getContainers());

        return networkInfo;
    }

    @Override
    public String createNetwork(Map<String, Object> networkConfig) {
        String name = (String) networkConfig.get("name");
        String driver = (String) networkConfig.get("driver");
        Map<String, Object> ipamConfig = (Map<String, Object>) networkConfig.get("ipam");
        return dockerService.createNetwork(name, driver, ipamConfig);
    }

    @Override
    public void removeNetwork(String networkId) {
        dockerService.removeNetwork(networkId);
    }

    @Override
    public void connectContainer(String networkId, Map<String, Object> connectConfig) {
        String containerId = (String) connectConfig.get("containerId");
        String ipAddress = (String) connectConfig.get("ipAddress");
        String[] aliases = (String[]) connectConfig.get("aliases");
        dockerService.connectContainerToNetwork(containerId, networkId, ipAddress, aliases);
    }

    @Override
    public void disconnectContainer(String networkId, Map<String, Object> disconnectConfig) {
        String containerId = (String) disconnectConfig.get("containerId");
        boolean force = (boolean) disconnectConfig.getOrDefault("force", false);
        dockerService.disconnectContainerFromNetwork(containerId, networkId, force);
    }
} 