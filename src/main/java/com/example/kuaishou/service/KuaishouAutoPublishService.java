package com.example.kuaishou.service;

import com.example.kuaishou.data.KuaishouVideoBatchData;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class KuaishouAutoPublishService {
    private KuaishouVideoBatchData batchData;

    private final KuaishouDownloadService downloadService;
    private final KuaishouVideoFileService videoFileService;
    private final MiniMaxImageService miniMaxImageService;
    private final ImageTextTemplateService imageTextTemplateService;
    private final VideoProcessService videoProcessService;

    String bearerToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiJjb3Ntb3MiLCJVc2VyTmFtZSI6ImNvc21vcyIsIkFjY291bnQiOiIiLCJTdWJqZWN0SUQiOiIxOTk1ODAzNzk2NDc4ODkwNTQ3IiwiUGhvbmUiOiIxODEwMTQwOTY5NSIsIkdyb3VwSUQiOiIxOTk1ODAzNzk2NDY2MzA3NjM1IiwiUGFnZU5hbWUiOiIiLCJNYWlsIjoiIiwiQ3JlYXRlVGltZSI6IjIwMjUtMTItMDMgMTY6MDY6NDQiLCJUb2tlblR5cGUiOjEsImlzcyI6Im1pbmltYXgifQ.MtGSILTbrXXuD2LFvdQTAne3oONyGv92YdBbwar1xQcXpmTADAabaH50jOe4a01pHrPsWZmOSpePQ7VECcyddaliHL051y-ajaKbfiNEjzFOhbx5XOCoAHFvyH2ZadrYawdgMrBURXvtVWWQLIyRjiXBHmI_RIJcPjlXmbQ3I9sbN5X9rLG0eIBt3qTRRnTzPYZY7opgvPEzCsnOKyqhkhmjkLTnKcV_HqwXFGR4OFT3k6h04UVUSQUWflATnB_wYwPo-dE27onI9UAzZw4piJaJ9G0EWbj55_oFeY8B4icggIXqR8tblpjmozK_FcgUHofJ24-YX17AljkipkfTJA";
    public KuaishouAutoPublishService(KuaishouDownloadService downloadService, KuaishouVideoFileService videoFileService, MiniMaxImageService miniMaxImageService, ImageTextTemplateService imageTextTemplateService, VideoProcessService videoProcessService) throws IOException {
        this.downloadService = downloadService;
        this.videoFileService = videoFileService;
        this.miniMaxImageService = miniMaxImageService;
        this.imageTextTemplateService = imageTextTemplateService;
        this.videoProcessService = videoProcessService;

        init();
    }

    void init() throws IOException {
        String root = System.getProperty("user.dir");

        createDir(root, "kuaishouDownload");
        createDir(root, "image");
        createDir(root, "cover");
    }

    private void createDir(String root, String dirName) throws IOException {
        Path path = Paths.get(root, dirName);
        Files.createDirectories(path);
        System.out.println("确保目录存在：" + path.toAbsolutePath());
    }


    public void run() throws Exception {
        // 1. 读取当天文件
        videoFileService.loadTodayFile();
        batchData = videoFileService.getBatchData();
        if (batchData == null || batchData.getVideoList() == null) {
            System.out.println("当天没有需要处理的视频数据");
            return;
        }
        System.out.println(batchData);
        // 2. 遍历批次数据
        for (var videoData : batchData.getVideoList()) {
            String videoUrl = videoData.getVideoUrl();

            try {
                // 3. 调用处理方法
                String videoId = downloadService.processVideo(videoUrl);

                // 4. 回写 videoId
                videoData.setVideoId(videoId);

                imageTextTemplateService.runImageText(videoData);

                miniMaxImageService.generateImage(bearerToken, videoData);

                imageTextTemplateService.runImageText(videoData);

                videoProcessService.runFfmpeg(videoData);

                System.out.println("处理完成：" + videoUrl + " -> videoId=" + videoId);
            } catch (Exception e) {
                // 单条失败不影响整体
                System.err.println("处理失败：" + videoUrl);
                e.printStackTrace();
            }
        }
    }
    @PostConstruct
    public void autoRun() {
        try {
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
