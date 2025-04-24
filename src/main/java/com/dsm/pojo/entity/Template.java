package com.dsm.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docker容器模板实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    /**
     * 模板ID
     */
    private String id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 基础镜像
     */
    private String baseImage;

    /**
     * 环境变量配置
     */
    private String envConfig;

    /**
     * 端口映射配置
     */
    private String portConfig;

    /**
     * 创建时间
     */
    private String created;
} 