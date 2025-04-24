package com.dsm.service;

import com.dsm.pojo.entity.Template;

import java.util.List;

public interface TemplateService {

    List<Template> getTemplates();

    Template getTemplate(String id);

    void addTemplate(Template template);

    void updateTemplate(Template template);

    void deleteTemplate(String id);
} 