package com.dsm.service;

import com.dsm.pojo.common.PageResult;
import com.dsm.pojo.entity.Template;
import com.dsm.pojo.param.TemplateQueryParam;

public interface AppStoreService {

    PageResult<Template> getTemplates(TemplateQueryParam param);

    Template getTemplate(String id);

    Template getTemplateInstallConfig(String id);

    void addTemplate(Template template);

    void updateTemplate(Template template);

    void deleteTemplate(String id);
} 