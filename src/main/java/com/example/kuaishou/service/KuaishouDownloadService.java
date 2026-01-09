package com.example.kuaishou.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.edge.EdgeOptions;

@Service
public class KuaishouDownloadService {

    public String processVideo(String url) throws Exception {
        System.setProperty("webdriver.edge.driver", "G:\\data\\java\\demo\\msedgedriver.exe");
        EdgeOptions options = new EdgeOptions();
        options.addArguments("user-data-dir=C:\\Users\\29805\\AppData\\Local\\Microsoft\\Edge\\User Data\\SeleniumProfile");
        options.addArguments("profile-directory=Default");
        WebDriver driver = new EdgeDriver(options);
        driver.get(url);  // 快手短视频链接

        Thread.sleep(5000);

        Set<Cookie> seleniumCookies = driver.manage().getCookies();
        String cookieHeader = seleniumCookies.stream()
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(Collectors.joining("; "));
//        System.out.println("Cookies: " + cookieHeader);

        driver.quit();

        String finalUrl = getUrl(url);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(finalUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Cookie", cookieHeader)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//        System.out.println("状态码: " + response.statusCode());
//        System.out.println("Body长度: " + response.body().length());
        // 提取视频信息
        String photoId = extractPhotoId(finalUrl);
        extractKuaishouVideoInfo(response.body(), photoId);
        return photoId;
    }
    public static String extractPhotoId(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery(); // 获取 ? 后面的查询字符串
            if (query == null) return null;

            String[] params = query.split("&");
            Map<String, String> paramMap = new HashMap<>();
            for (String param : params) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    paramMap.put(kv[0], kv[1]);
                }
            }
            return paramMap.get("photoId"); // 返回 photoId
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
    public String getUrl(String url) throws Exception {

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER) // 手动处理重定向
                .build();

        // 获取最终 URL
        String finalUrl = followRedirects(client, url);
        return finalUrl;
    }

    private String followRedirects(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/128.0.0.0 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();

        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            Optional<String> location = response.headers().firstValue("location");
            if (location.isPresent()) {
                String nextUrl = location.get();
//                System.out.println("重定向到: " + nextUrl);
                return followRedirects(client, nextUrl);
            } else {
                throw new RuntimeException("重定向响应中没有 Location");
            }
        } else {
            return url; // 没有重定向，返回最终 URL
        }
    }

    public void extractKuaishouVideoInfo(String html, String photoId) {
        Document doc = Jsoup.parse(html);
        Elements scripts = doc.getElementsByTag("script");

        if (scripts.isEmpty()) {
            System.out.println("未找到 script 标签");
            return;
        }

        String scriptContent = null;
        for (Element script : scripts) {
            if (script.html().contains("window.__APOLLO_STATE__")) {
                scriptContent = script.html();
                break;
            }
        }

        if (scriptContent == null) {
            System.out.println("未找到 window.__APOLLO_STATE__");
            return;
        }

        String prefix = "window.__APOLLO_STATE__=";
        int startIndex = scriptContent.indexOf(prefix);
        String jsonStr = scriptContent.substring(startIndex + prefix.length());

        if (jsonStr.endsWith(";")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 1);
        }

        JSONObject json = new JSONObject(jsonStr);

        try {
            JSONObject defaultClient = json.getJSONObject("defaultClient");

            // 先找到 ROOT_QUERY 下的视频详情 key
            JSONObject rootQuery = defaultClient.getJSONObject("ROOT_QUERY");
            String videoQueryKey = "VisionVideoDetailPhoto:" + photoId;
            // 再去 defaultClient 或者顶层对象中获取视频信息
            JSONObject photo = defaultClient.getJSONObject(videoQueryKey);
             String ROOT = System.getProperty("user.dir");
            String fPath = ROOT + "\\kuaishouDownload";


//            System.out.println("视频 URL: " + photo.getString("photoUrl"));
            downloadVideo(photo.getString("photoUrl"), fPath + "\\" + photoId + ".mp4");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("解析视频信息失败");
        }
    }
    public void downloadVideo(String videoUrl, String savePath) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(videoUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/128.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                try (InputStream in = response.body();
                     FileOutputStream out = new FileOutputStream(savePath)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    System.out.println("下载完成: " + savePath);
                }
            } else {
                System.out.println("下载失败，状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("下载视频出错");
        }
    }

}