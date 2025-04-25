package com.dsm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DockerJsonParser {

    // 解析环境变量数组
    public static String[] parseEnv(JsonNode envNode) {
        if (envNode == null || !envNode.isArray()) {
            return new String[0];
        }
        List<String> envList = new ArrayList<>();
        envNode.forEach(node -> envList.add(node.asText()));
        return envList.toArray(new String[0]);
    }

    // 解析卷绑定
    public static Bind[] parseBinds(JsonNode bindsNode) {
        if (bindsNode == null || !bindsNode.isArray()) {
            return new Bind[0];
        }
        List<Bind> binds = new ArrayList<>();
        bindsNode.forEach(node -> binds.add(Bind.parse(node.asText())));
        return binds.toArray(new Bind[0]);
    }

    // 解析暴露端口（ExposedPorts 字段）
    public static ExposedPort[] parseExposedPorts(JsonNode exposedPortsNode) {
        if (exposedPortsNode == null || !exposedPortsNode.isObject()) {
            return new ExposedPort[0];
        }
        List<ExposedPort> exposedPorts = new ArrayList<>();
        Iterator<String> fieldNames = exposedPortsNode.fieldNames();
        while (fieldNames.hasNext()) {
            String portSpec = fieldNames.next(); // e.g. "8080/tcp"
            exposedPorts.add(ExposedPort.parse(portSpec));
        }
        return exposedPorts.toArray(new ExposedPort[0]);
    }

    // 解析端口绑定（HostConfig.PortBindings）
    public static Ports parsePortBindings(JsonNode portBindingsNode) {
        Ports ports = new Ports();
        if (portBindingsNode == null || !portBindingsNode.isObject()) {
            return ports;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = portBindingsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String containerPortSpec = entry.getKey(); // e.g. "8080/tcp"
            ExposedPort exposedPort = ExposedPort.parse(containerPortSpec);

            JsonNode bindings = entry.getValue();
            if (bindings.isArray()) {
                bindings.forEach(bindingNode -> {
                    String hostPort = bindingNode.get("HostPort").asText();
                    ports.bind(exposedPort, Ports.Binding.bindPort(Integer.parseInt(hostPort)));
                });
            }
        }

        return ports;
    }
}