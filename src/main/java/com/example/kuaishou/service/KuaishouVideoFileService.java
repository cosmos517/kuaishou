package com.example.kuaishou.service;

import com.example.kuaishou.data.KuaishouVideoBatchData;
import com.example.kuaishou.data.KuaishouVideoData;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class KuaishouVideoFileService {

    /**
     * 按当天日期读取 txt 文件，封装为 KuaishouVideoBatchData
     *
     * @param baseDir txt 文件所在目录
     */
    private KuaishouVideoBatchData batchData;

    public void loadTodayFile() throws Exception {
        String currentDir = System.getProperty("user.dir");
        batchData = readTodayFileInternal(currentDir);
    }


    public KuaishouVideoBatchData getBatchData() {
        return batchData;
    }

    public KuaishouVideoBatchData readTodayFileInternal(String baseDir) throws Exception {

        // 1️⃣ 当天文件名
        String fileName = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + ".txt";

        File file = new File(baseDir, fileName);
        if (!file.exists()) {
            System.out.println("⚠ 未找到当天视频文件：" + file.getAbsolutePath());
            System.out.println("⏹ 今日不执行自动发布任务");
            return emptyBatch();
        }

        List<KuaishouVideoData> videoList = new ArrayList<>();

        // 2️⃣ 逐行读取
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // 按空格分割（最多分成 4 段）
                String[] parts = line.split("\\s+", -1);
                if (parts.length < 2) {
                    continue;
                }

                KuaishouVideoData videoData = new KuaishouVideoData();
                videoData.setVideoUrl(parts[0].trim());
                videoData.setProductUrl(parts[1].trim());

                if (parts.length >= 3) {
                    videoData.setImgPrompt(parts[2].trim());
                }
                if (parts.length >= 4) {
                    videoData.setCoverImg(parts[3].trim());
                }
                if (parts.length >= 5) {
                    videoData.setTitle(parts[4].trim());
                }

                videoList.add(videoData);
            }
        }

        // 3️⃣ 组装 BatchData
        KuaishouVideoBatchData batchData = new KuaishouVideoBatchData();
        batchData.setDateTime(LocalDateTime.now());
        batchData.setVideoList(videoList);

        return batchData;
    }

    private KuaishouVideoBatchData emptyBatch() {
        KuaishouVideoBatchData batch = new KuaishouVideoBatchData();
        batch.setDateTime(LocalDateTime.now());
        batch.setVideoList(new ArrayList<>());
        return batch;
    }
}
