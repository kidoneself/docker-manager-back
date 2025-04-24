package com.dsm.pojo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ServiceConfig {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("template")
    private ServiceTemplate template;
}

@Data
class ServiceTemplate {
    @JsonProperty("Image")
    private String image;
    
    @JsonProperty("Env")
    private List<String> env;
    
    @JsonProperty("ExposedPorts")
    private Map<String, Object> exposedPorts;
    
    @JsonProperty("HostConfig")
    private HostConfig hostConfig;
}

@Data
class HostConfig {
    @JsonProperty("PortBindings")
    private Map<String, String> portBindings;
    
    @JsonProperty("Binds")
    private List<String> binds;
    
    @JsonProperty("RestartPolicy")
    private String restartPolicy;
    
    @JsonProperty("Privileged")
    private Boolean privileged;
    
    @JsonProperty("NetworkMode")
    private String networkMode;
} 