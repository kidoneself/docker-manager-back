package com.dsm.service;

import com.dsm.pojo.dto.ImageStatusDTO;
import com.dsm.pojo.dto.image.ImageInspectDTO;
import com.dsm.pojo.request.ImagePullRequestV2;

import java.util.List;
import java.util.Map;

/**
 * 镜像服务接口
 * 定义镜像管理的业务逻辑
 */
public interface ImageService {

    String pullImage(ImagePullRequestV2 request);

    Map<String, Object> getPullProgress(String taskId);

    Map<String, Object> cancelPullTask(String taskId);

    void removeImage(String imageId, boolean removeStatus);

    Map<String, Long> testProxyLatency();

    List<ImageStatusDTO> listImageStatus();

    void checkAllImagesStatus();

    Map<String, Object> updateImage(String image, String tag);

    List<ImageStatusDTO> listImages();

    /**
     * 获取镜像详情
     *
     * @param imageName 镜像名称（格式：name:tag）
     * @return 镜像详情信息
     */
    ImageInspectDTO getImageDetail(String imageName);
}