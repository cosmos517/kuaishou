package com.example.kuaishou.service;

import com.example.kuaishou.data.KuaishouVideoData;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@Service
public class VideoProcessService {

    /** 项目根目录 */
    private static final String ROOT = System.getProperty("user.dir");
    private static final String FFMPEG = ROOT + "/ffmpeg/bin/ffmpeg.exe";
    private static final String FFPROBE = ROOT + "/ffmpeg/bin/ffprobe.exe";
    /**
     * 方法一：在视频首帧插入一张图片
     *
     * @param inputVideo  原视频文件名（data/input）
     * @param image       图片文件名（data/image）
     * @param outputVideo 输出视频文件名（data/output）
     */
    public void insertImageAtFirstFrame(String inputVideo,
                                        String image,
                                        String outputVideo) throws Exception {

        String inputPath = ROOT + "/kuaishouDownload/" + inputVideo;
        String imagePath = ROOT + "/cover/" + image;
        String outputPath = ROOT + "/kuaishouDownload/" + outputVideo;

        // 先获取视频分辨率
        int[] resolution = getVideoResolution(inputPath);
        int width = resolution[0];
        int height = resolution[1];

        // FFmpeg 命令
        List<String> command = Arrays.asList(
                FFMPEG,
                "-y",                      // 覆盖输出文件
                "-loop", "1", "-i", imagePath,
                "-i", inputPath,
                "-filter_complex",
                String.format(
                        "[0:v]scale='if(gt(a,%f),-1,%d)':'if(gt(a,%f),%d,-1)'," +  // 按比例缩放
                                "crop=%d:%d," +                                              // 裁剪到视频分辨率
                                "format=yuv420p,trim=0:0.04,setpts=PTS-STARTPTS[v0];" +
                                "[1:v]format=yuv420p,setpts=PTS-STARTPTS[v1];" +
                                "[v0][v1]concat=n=2:v=1:a=0[v]",
                        (double) width / height, width, (double) width / height, height,
                        width, height
                ),
                "-map", "[v]",
                "-map", "1:a?",           // 保留原视频音频，如果有
                "-c:v", "libx264",
                "-crf", "23",
                "-preset", "fast",
                outputPath
        );

        execute(command);
    }

    private int[] getVideoResolution(String videoPath) throws Exception {
        List<String> command = Arrays.asList(
                FFPROBE,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0:s=x",
                videoPath
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = br.readLine();
        p.waitFor();

        if (line == null || !line.contains("x")) {
            throw new RuntimeException("无法获取视频分辨率");
        }

        String[] parts = line.split("x");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    /**
     * 方法二：在视频上叠加图片，并将图片透明度设置为 0
     *
     * @param inputVideo  原视频文件名（data/input）
     * @param image       图片文件名（data/image）
     * @param outputVideo 输出视频文件名（data/output）
     */
    public void overlayImageWithAlphaZero(String inputVideo,
                                          String image,
                                          String outputVideo) throws Exception {

        String inputPath = ROOT + "/kuaishouDownload/" + inputVideo;
        String imagePath = ROOT + "/image/" + image;
        String outputPath = ROOT + "/kuaishouDownload/" + outputVideo;

        String filter =
                "[1:v]format=rgba,colorchannelmixer=aa=0.0[img]" +
                        ";[0:v][img]overlay=0:0";

        List<String> command = Arrays.asList(
                FFMPEG,
                "-i", inputPath,
                "-i", imagePath,
                "-filter_complex", filter,
                "-c:a", "copy",
                outputPath
        );

        execute(command);
    }


    /**
     * 执行 FFmpeg 命令（公共方法）
     */
    private void execute(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);

            // 合并 stdout + stderr（FFmpeg 错误都在 stderr）
            pb.redirectErrorStream(true);

            Process p = pb.start();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 执行失败，exitCode=" + exitCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg 执行异常", e);
        }
    }

    public void runFfmpeg(KuaishouVideoData data) throws Exception {
        VideoProcessService service = new VideoProcessService();
        String ROOT = System.getProperty("user.dir");
        service.overlayImageWithAlphaZero(data.getVideoId() + ".mp4",data.getVideoId() + ".jpeg",data.getVideoId() + "_1.mp4");
        service.insertImageAtFirstFrame(data.getVideoId() + "_1.mp4",data.getVideoId() + "_cover.jpg",data.getVideoId() + "_upload.mp4");
    }
}