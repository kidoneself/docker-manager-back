package com.dsm.pojo.response;

import com.dsm.pojo.entity.ServiceConfig;
import com.dsm.pojo.entity.ParameterConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AppStoreAppDetail {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("iconUrl")
    private String iconUrl;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    
    @JsonProperty("services")
    private List<ServiceConfig> services;
    
    @JsonProperty("parameters")
    private List<ParameterConfig> parameters;
} 