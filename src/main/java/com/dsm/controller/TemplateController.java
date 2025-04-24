//package com.dsm.controller;
//
//import com.dsm.pojo.common.PageResult;
//import com.dsm.pojo.entity.Template;
//import com.dsm.pojo.param.TemplateQueryParam;
//import com.dsm.pojo.response.AppStoreApp;
//import com.dsm.pojo.response.AppStoreAppDetail;
//import com.dsm.service.TemplateService;
//import com.dsm.utils.ApiResponse;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.BeanUtils;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/app-store/apps")
//@Tag(name = "应用商店", description = "Docker应用商店管理接口")
//@RequiredArgsConstructor
//public class TemplateController {
//    private final TemplateService templateService;
//
//    @Operation(summary = "获取应用商店列表", description = "分页获取应用商店列表，支持分类筛选和搜索")
//    @GetMapping
//    public ApiResponse<PageResult<AppStoreApp>> getAppList(
//            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
//            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
//            @Parameter(description = "分类") @RequestParam(required = false) String category
//    ) {
//        TemplateQueryParam param = new TemplateQueryParam();
//        param.setPage(page);
//        param.setSize(pageSize);
//        param.setCategory(category);
//        PageResult<Template> templates = templateService.getTemplates(param);
//
//        // 转换为前端需要的格式
//        List<AppStoreApp> apps = templates.getRecords().stream()
//            .map(template -> {
//                AppStoreApp app = new AppStoreApp();
//                BeanUtils.copyProperties(template, app);
//                return app;
//            })
//            .collect(Collectors.toList());
//
//        PageResult<AppStoreApp> result = new PageResult<>();
//        result.setRecords(apps);
//        result.setTotal(templates.getTotal());
//        result.setSize(templates.getSize());
//        result.setCurrent(templates.getCurrent());
//        result.setPages(templates.getPages());
//
//        return ApiResponse.success(result);
//    }
//
//    @Operation(summary = "获取应用详情", description = "根据ID获取应用详情，包含完整的模板配置")
//    @GetMapping("/{id}")
//    public ApiResponse<AppStoreAppDetail> getAppDetail(
//            @Parameter(description = "应用ID") @PathVariable String id
//    ) {
//        Template template = templateService.getTemplate(id);
//        if (template == null) {
//            return ApiResponse.error("应用不存在");
//        }
//
//        AppStoreAppDetail detail = new AppStoreAppDetail();
//        BeanUtils.copyProperties(template, detail);
//
//        // 设置服务和参数
//        var content = template.getTemplateContent();
//        if (content != null) {
//            detail.setServices(content.getServices());
//            detail.setParameters(content.getParameters());
//        }
//
//        return ApiResponse.success(detail);
//    }
//
//    @Operation(summary = "创建应用", description = "创建新的应用模板")
//    @PostMapping
//    public ApiResponse<Void> createTemplate(
//            @Parameter(description = "应用模板数据") @RequestBody @Validated Template template
//    ) {
//        templateService.addTemplate(template);
//        return ApiResponse.success(null);
//    }
//
//    @Operation(summary = "更新应用", description = "更新现有应用模板")
//    @PutMapping("/{id}")
//    public ApiResponse<Void> updateTemplate(
//            @Parameter(description = "应用ID") @PathVariable String id,
//            @Parameter(description = "应用模板数据") @RequestBody @Validated Template template
//    ) {
//        template.setId(id);
//        templateService.updateTemplate(template);
//        return ApiResponse.success(null);
//    }
//}