package com.example.kuaishou.data;

import lombok.Data;

/**
 * 快手视频数据实体类
 */
@Data
public class KuaishouVideoData {

    /**
     * 视频页面链接
     */
    private String videoUrl;

    /**
     * 视频 ID（photoId）
     */
    private String videoId;

    /**
     * 商品链接
     */
    private String productUrl;

    private String imgPrompt;

    private String coverImg;

    private String title;
}
