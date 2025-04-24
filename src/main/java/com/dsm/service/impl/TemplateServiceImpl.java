package com.dsm.service.impl;

import com.dsm.pojo.entity.Template;
import com.dsm.service.TemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板服务实现类
 * 管理容器模板的创建、查询、更新和删除
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    // 模拟模板存储，实际项目中应该使用持久化存储
    private final List<Template> templates = new ArrayList<>();

    public TemplateServiceImpl() {
        // 初始化一些示例模板数据
        templates.add(new Template(
                "1",
                "Nginx Web服务器",
                "基于Nginx的Web服务器模板",
                "nginx:latest",
                "PORT=80",
                "80:80",
                "2024-04-07 10:00:00"
        ));

        templates.add(new Template(
                "2",
                "MySQL数据库",
                "MySQL数据库服务器模板",
                "mysql:8.0",
                "MYSQL_ROOT_PASSWORD=root\nMYSQL_DATABASE=test",
                "3306:3306",
                "2024-04-07 10:30:00"
        ));

        templates.add(new Template(
                "3",
                "Redis缓存",
                "Redis缓存服务器模板",
                "redis:latest",
                "REDIS_PASSWORD=redis",
                "6379:6379",
                "2024-04-07 11:00:00"
        ));
    }

    @Override
    public List<Template> getTemplates() {
        return templates;
    }

    @Override
    public Template getTemplate(String id) {
        return templates.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void addTemplate(Template template) {
        templates.add(template);
    }

    @Override
    public void updateTemplate(Template template) {
        templates.removeIf(t -> t.getId().equals(template.getId()));
        templates.add(template);
    }

    @Override
    public void deleteTemplate(String id) {
        templates.removeIf(t -> t.getId().equals(id));
    }
} 