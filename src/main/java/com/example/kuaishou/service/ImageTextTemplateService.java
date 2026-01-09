package com.example.kuaishou.service;

import com.example.kuaishou.data.KuaishouVideoData;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.awt.RenderingHints;

@Service
public class ImageTextTemplateService {

    /* =========================
       基础路径
       ========================= */
    private static final String ROOT = System.getProperty("user.dir");

    /* =========================
       模板样式配置（固定）
       ========================= */

    /** 字体文件路径 */
    private static final String FONT_PATH = ROOT + "/font/wryh.ttf";

    /** 字号 */
    private static final float FONT_SIZE = 148f;

    /** 文字颜色 */
    private static final Color TEXT_COLOR = Color.ORANGE;

    /** 描边颜色 */
    private static final Color STROKE_COLOR = Color.BLACK;

    /** 描边粗细 */
    private static final float STROKE_WIDTH = 4.0f;

    /** 透明度（0.0 ~ 1.0） */
    private static final float ALPHA = 1.0f;

    /** 左右边距（用于自动换行） */
    private static final int H_PADDING = 60;

    /** 行间距（像素） */
    private static final int LINE_SPACING = 14;

    /**
     * 对外唯一方法：
     * 按模板样式在图片上绘制文字（自动换行 + 描边）
     */
    public void drawText(String inputImagePath,
                         String outputImagePath,
                         String text) throws Exception {

        BufferedImage image = ImageIO.read(new File(inputImagePath));
        Graphics2D g = image.createGraphics();

        // ===== 抗锯齿 =====
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ===== 字体 =====
        Font baseFont = Font.createFont(
                Font.TRUETYPE_FONT, new File(FONT_PATH));
        Font font = baseFont.deriveFont(Font.BOLD, FONT_SIZE);
        g.setFont(font);

        // ===== 透明度 =====
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, ALPHA));

        FontMetrics fm = g.getFontMetrics(font);

        // ===== 自动换行 =====
        int maxTextWidth = image.getWidth() - H_PADDING * 2;
        List<String> lines = wrapText(text, fm, maxTextWidth);

        // ===== 计算整体高度（用于垂直居中）=====
        int lineHeight = fm.getHeight() + LINE_SPACING;
        int totalTextHeight = lines.size() * lineHeight;

        int startY = (image.getHeight() - totalTextHeight) / 2 + fm.getAscent();

        // ===== 逐行绘制 =====
        for (String line : lines) {

            int lineWidth = fm.stringWidth(line);
            int x = (image.getWidth() - lineWidth) / 2;

            // 转为形状（为了描边）
            GlyphVector gv = font.createGlyphVector(
                    g.getFontRenderContext(), line);
            Shape textShape = gv.getOutline(x, startY);

            // 描边
            g.setColor(STROKE_COLOR);
            g.setStroke(new BasicStroke(
                    STROKE_WIDTH,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(textShape);

            // 填充文字
            g.setColor(TEXT_COLOR);
            g.fill(textShape);

            startY += lineHeight;
        }

        g.dispose();

        ImageIO.write(image, "png", new File(outputImagePath));
    }

    /* =========================
       文本自动换行工具方法
       ========================= */

    private List<String> wrapText(String text,
                                  FontMetrics fm,
                                  int maxWidth) {

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (char c : text.toCharArray()) {
            line.append(c);
            if (fm.stringWidth(line.toString()) > maxWidth) {
                line.deleteCharAt(line.length() - 1);
                lines.add(line.toString());
                line = new StringBuilder();
                line.append(c);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
    public  void runImageText(KuaishouVideoData data) throws Exception {
        ImageTextTemplateService service = new ImageTextTemplateService();
        String ROOT = System.getProperty("user.dir");
        service.drawText(ROOT + "/cover/" + data.getCoverImg(), ROOT + "/cover/" + data.getVideoId() + "_cover.jpg", data.getTitle());
    }
}
