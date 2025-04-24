package com.dsm.api.impl;

import com.dsm.api.DockerService;
import com.dsm.websocket.callback.PullImageCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.function.Consumer;

@Slf4j
@Service
public class DockerServiceImpl extends DockerService {
    
    @Resource
    private DockerClient dockerClient;
    
    @Override
    public void pullImage(String image, String tag, PullImageCallback callback) {
        String imageName = tag != null && !tag.isEmpty() ? image + ":" + tag : image;
        
        try {
            callback.onProgress(0, "开始拉取镜像");
            
            dockerClient.pullImageCmd(imageName)
                .exec(new PullImageResultCallback() {
                    @Override
                    public void onNext(PullResponseItem item) {
                        // 计算进度
                        int progress = calculateProgress(item);
                        callback.onProgress(progress, item.getStatus());
                    }
                    
                    @Override
                    public void onComplete() {
                        callback.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        callback.onError(throwable.getMessage());
                    }
                });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
    
    private int calculateProgress(PullResponseItem item) {
        if (item.getProgressDetail() != null) {
            return (int) ((item.getProgressDetail().getCurrent() * 100.0) / item.getProgressDetail().getTotal());
        }
        return 0;
    }
    
    // ... 保留其他方法 ...
} 