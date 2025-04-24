package com.dsm.controller;

import com.dsm.pojo.entity.Template;
import com.dsm.service.TemplateService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@Tag(name = "模板管理", description = "容器模板管理接口")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "获取模板列表", description = "获取所有容器模板")
    @GetMapping
    public ApiResponse<List<Template>> getTemplates() {
        return ApiResponse.success(templateService.getTemplates());
    }

    @Operation(summary = "获取模板详情", description = "根据ID获取模板详情")
    @GetMapping("/{id}")
    public ApiResponse<Template> getTemplate(@PathVariable String id) {
        return ApiResponse.success(templateService.getTemplate(id));
    }

    @Operation(summary = "创建模板", description = "创建新的容器模板")
    @PostMapping
    public ApiResponse<Void> createTemplate(@RequestBody Template template) {
        templateService.addTemplate(template);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新模板", description = "更新现有容器模板")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateTemplate(@PathVariable String id, @RequestBody Template template) {
        template.setId(id);
        templateService.updateTemplate(template);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除模板", description = "删除指定容器模板")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return ApiResponse.success(null);
    }
}