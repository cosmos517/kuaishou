package com.example.kuaishou.data;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 快手视频批次数据
 */
@Data
public class KuaishouVideoBatchData {

    private LocalDateTime dateTime;
    private List<KuaishouVideoData> videoList;
}
