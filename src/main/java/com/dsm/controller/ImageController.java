package com.dsm.controller;

import com.dsm.pojo.dto.ImageStatusDTO;
import com.dsm.pojo.dto.image.ImageInspectDTO;
import com.dsm.pojo.request.ImagePullRequestV2;
import com.dsm.service.ImageService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Tag(name = "镜像管理", description = "Docker镜像相关接口")
public class ImageController {

    @Resource
    ImageService imageService;


    /**
     * 拉取Docker镜像
     *
     * @param request 请求体，包含镜像名称和标签
     * @return 包含任务ID的响应
     */
    @Operation(summary = "拉取镜像", description = "拉取指定的Docker镜像")
    @PostMapping("/pull")
    public ApiResponse<Map<String, String>> pullImage(@RequestBody ImagePullRequestV2 request) {
        String taskId = imageService.pullImage(request);
        Map<String, String> result = new HashMap<>();
        result.put("taskId", taskId);
        return ApiResponse.success(result);
    }

    /**
     * 获取镜像拉取进度
     *
     * @param taskId 任务ID
     * @return 拉取进度信息
     */
    @Operation(summary = "获取拉取进度", description = "获取指定任务的镜像拉取进度")
    @GetMapping("/pull/progress/{taskId}")
    public ApiResponse<Map<String, Object>> getPullProgress(@PathVariable String taskId) {
        Map<String, Object> taskInfo = imageService.getPullProgress(taskId);
        return ApiResponse.success(taskInfo);
    }

    /**
     * 取消镜像拉取任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Operation(summary = "取消拉取任务", description = "取消指定的镜像拉取任务")
    @PostMapping("/pull/cancel/{taskId}")
    public ApiResponse<Void> cancelPullTask(@PathVariable String taskId) {
        imageService.cancelPullTask(taskId);
        return ApiResponse.success(null);
    }


    /**
     * 删除Docker镜像
     *
     * @param imageId 镜像ID
     * @return 操作结果
     */
    @Operation(summary = "删除镜像", description = "删除指定的Docker镜像")
    @DeleteMapping("/{imageId}")
    public ApiResponse<String> deleteImage(@PathVariable() String imageId, @RequestParam(defaultValue = "true") boolean removeStatus) {
        imageService.removeImage(imageId, removeStatus);
        return ApiResponse.success("删除镜像成功");
    }

    /**
     * TODO  测试代理延迟
     *
     * @return 代理延迟信息
     */
    @Operation(summary = "测试代理延迟", description = "测试Docker镜像仓库的代理延迟")
    @GetMapping("/proxy/test")
    public ApiResponse<Map<String, Long>> testProxyLatency() {
        return ApiResponse.success(imageService.testProxyLatency());
    }

    /**
     * 获取镜像状态列表
     *
     * @return 镜像状态列表，包含更新信息
     */
    @Operation(summary = "获取镜像状态列表", description = "获取所有镜像的状态列表，包含更新检查信息")
    @GetMapping("/status")
    public ApiResponse<List<ImageStatusDTO>> getImageStatusList() {
        List<ImageStatusDTO> result = imageService.listImageStatus();
        return ApiResponse.success(result);

    }

    /**
     * 更新镜像
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @Operation(summary = "更新镜像", description = "更新指定的镜像到最新版本")
    @PostMapping("/update")
    public ApiResponse<Map<String, Object>> updateImage(@RequestBody Map<String, String> request) {
        String image = request.get("image");
        String tag = request.getOrDefault("tag", "latest");
        boolean useProxy = Boolean.parseBoolean(request.getOrDefault("useProxy", "false"));
        Map<String, Object> result = imageService.updateImage(image, tag);
        return ApiResponse.success(result);
    }

    /**
     * 检查镜像更新状态
     *
     * @return 检查结果
     */
    @Operation(summary = "检查镜像更新状态", description = "检查所有镜像是否有新版本可用")
    @PostMapping("/check-updates")
    public ApiResponse<Map<String, Object>> checkImageUpdates() {
        try {
            imageService.checkAllImagesStatus();
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("检查镜像更新状态失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "检查镜像更新状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "列出镜像", description = "获取所有镜像的列表（包含更新状态信息）")
    @GetMapping()
    public ApiResponse<List<ImageStatusDTO>> listImages() {
        return ApiResponse.success(imageService.listImages());
    }

    /**
     * 获取镜像详情
     *
     * @return 镜像详情
     */
    @PostMapping("/detail")
    public ApiResponse<ImageInspectDTO> getImageDetail(@RequestBody Map<String, String> request) {
        try {
            String imageName = request.get("imageName");
            if (imageName == null || imageName.isEmpty()) {
                return ApiResponse.error("镜像名称不能为空");
            }
            ImageInspectDTO imageDetail = imageService.getImageDetail(imageName);
            return ApiResponse.success(imageDetail);
        } catch (Exception e) {
            log.error("获取镜像详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

}