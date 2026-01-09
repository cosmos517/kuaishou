package com.example.kuaishou.service;

import org.json.JSONArray;
import org.json.JSONObject;
import com.example.kuaishou.data.KuaishouVideoData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
public class MiniMaxImageService {

    private static final String API_URL = "https://api.minimaxi.com/v1/image_generation";

    private final HttpClient httpClient;

    public MiniMaxImageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 生成图片
     *
     * @param bearerToken Bearer 后面的 token（不需要加 "Bearer "）
     * @param data      图片描述提示词
     * @return 接口返回的 JSON 字符串
     */
    public void generateImage(String bearerToken, KuaishouVideoData data) throws Exception {

        String requestBody = buildRequestBody(data.getImgPrompt());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "MiniMax 接口调用失败，status=" + response.statusCode() +
                            "\n响应内容：" + response.body()
            );
        }
        String ROOT = System.getProperty("user.dir");
        downloadFirstImage(response.body(),  ROOT + "\\image", data.getVideoId());
    }

    /**
     * 构造请求 JSON
     */
    private String buildRequestBody(String prompt) {
        return """
                {
                  "model": "image-01",
                  "prompt": "%s",
                  "aspect_ratio": "9:16",
                  "response_format": "url",
                  "n": 3,
                  "prompt_optimizer": true
                }
                """.formatted(escapeJson(prompt));
    }

    /**
     * 简单 JSON 转义（防止 prompt 中有引号）
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    /**
     * 从返回 JSON 中提取第一张 image_url 并下载
     */
    public String downloadFirstImage(String responseJson, String saveDir, String fileName) throws Exception {

        // 1️⃣ 解析 JSON
        JSONObject root = new JSONObject(responseJson);
        JSONObject data = root.getJSONObject("data");
        JSONArray imageUrls = data.getJSONArray("image_urls");

        if (imageUrls.isEmpty()) {
            throw new RuntimeException("image_urls 为空，无法下载");
        }

        String imageUrl = imageUrls.getString(0);

        // 2️⃣ 创建目录
        Path dirPath = Paths.get(saveDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        // 3️⃣ 生成文件名
       fileName = fileName + ".jpeg";
        Path savePath = dirPath.resolve(fileName);

        // 4️⃣ 下载图片
        downloadImage(imageUrl, savePath);

        return savePath.toAbsolutePath().toString();
    }

    private void downloadImage(String imageUrl, Path savePath)
            throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("图片下载失败，status=" + response.statusCode());
        }

        Files.write(savePath, response.body());
    }
}
