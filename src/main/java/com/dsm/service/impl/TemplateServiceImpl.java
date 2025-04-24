package com.dsm.service.impl;

import com.dsm.mapper.TemplateMapper;
import com.dsm.pojo.common.PageResult;
import com.dsm.pojo.entity.Template;
import com.dsm.pojo.param.TemplateQueryParam;
import com.dsm.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模板服务实现类
 * 管理容器模板的创建、查询、更新和删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final TemplateMapper templateMapper;

    @Override
    public PageResult<Template> getTemplates(TemplateQueryParam param) {
        // 查询数据
        var records = templateMapper.selectTemplates(param);
        // 查询总数
        var total = templateMapper.countTemplates(param);
        
        // 构建分页结果
        var result = new PageResult<Template>();
        result.setRecords(records);
        result.setTotal(total);
        result.setSize(param.getSize());
        result.setCurrent(param.getPage());
        result.setPages((total + param.getSize() - 1) / param.getSize());
        
        return result;
    }

    @Override
    public Template getTemplate(String id) {
        return templateMapper.selectTemplateById(id);
    }

    @Override
    public Template getTemplateInstallConfig(String id) {
        // 获取模板详情
        var template = getTemplate(id);
        if (template == null) {
            return null;
        }
        // 这里可以添加额外的安装配置处理逻辑
        return template;
    }

    @Override
    public void addTemplate(Template template) {
        templateMapper.insertTemplate(template);
    }

    @Override
    public void updateTemplate(Template template) {
        templateMapper.updateTemplate(template);
    }

    @Override
    public void deleteTemplate(String id) {
        // 实现删除逻辑
    }
} 