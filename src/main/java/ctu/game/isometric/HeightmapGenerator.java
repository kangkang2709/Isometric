package ctu.game.isometric;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HeightmapGenerator {

    public static  void main(String[] args) {
        String outputPath = "heightmap_grassy.png";
        int width = 512;
        int height = 512;

        generateGrassyHeightmap(outputPath, width, height);
    }

    public static void generateGrassyHeightmap(String outputPath, int width, int height) {
        BufferedImage heightmap = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Tạo địa hình cơ bản với độ cao trung bình
                float baseHeight = 0.5f;

                // Thêm sóng nhẹ cho độ cao
                float wave1 = (float) Math.sin(x * 0.02f) * 0.1f;
                float wave2 = (float) Math.cos(y * 0.015f) * 0.08f;

                // Thêm nhiễu Perlin đơn giản
                float noise = (float) (Math.random() - 0.5) * 0.05f;

                // Tạo một số đồi nhỏ
                float centerX = width * 0.3f;
                float centerY = height * 0.7f;
                float distanceFromHill = (float) Math.sqrt(
                        Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)
                );
                float hillHeight = Math.max(0, 0.15f * (1.0f - distanceFromHill / 80.0f));

                float finalHeight = baseHeight + wave1 + wave2 + noise + hillHeight;
                finalHeight = Math.max(0, Math.min(1, finalHeight));

                int grayValue = (int) (finalHeight * 255);
                int rgb = (grayValue << 16) | (grayValue << 8) | grayValue;
                heightmap.setRGB(x, y, rgb);
            }
        }

        try {
            ImageIO.write(heightmap, "PNG", new File(outputPath));
            System.out.println("Heightmap đã được tạo: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}