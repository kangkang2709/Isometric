package ctu.game.isometric;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HeightmapGenerator {

    public static  void main(String[] args) {
        String outputPath = "heightmap1.png";
        int width = 512;
        int height = 512;

        generateGrassyHeightmap(outputPath, width, height);
    }

    public static void generateGrassyHeightmap(String outputPath, int width, int height) {
        BufferedImage heightmap = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int numHills = 60;
        int numValleys = 1;

        float[][] hills = new float[numHills][3];   // centerX, centerY, radius
        float[][] valleys = new float[numValleys][3];

        float centerX = width / 2f;
        float centerY = height / 2f;

        // Tạo 1 trũng ở giữa
        valleys[0][0] = centerX;
        valleys[0][1] = centerY;
        valleys[0][2] = Math.min(width, height) * 0.35f; // bán kính lớn hơn

        // Tạo các đồi nằm ở xung quanh (ngoài vùng trung tâm)
        for (int i = 0; i < numHills; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float radius = (float) (Math.random() * Math.min(width, height) * 0.4f + Math.min(width, height) * 0.3f);
            float hx = centerX + (float) Math.cos(angle) * radius;
            float hy = centerY + (float) Math.sin(angle) * radius;
            float hillRadius = 40 + (float) (Math.random() * 50);

            hills[i][0] = hx;
            hills[i][1] = hy;
            hills[i][2] = hillRadius;
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float baseHeight = 0.55f;

                float wave1 = (float) Math.sin(x * 0.025f) * 0.03f;
                float wave2 = (float) Math.cos(y * 0.025f) * 0.03f;
                float noise = (float) (pseudoRandom(x * 0.1f, y * 0.1f) - 0.5f) * 0.025f;

                float hillsHeight = 0;
                for (float[] hill : hills) {
                    float dx = x - hill[0];
                    float dy = y - hill[1];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float radius = hill[2];
                    if (dist < radius) {
                        hillsHeight += 0.2f * (1.0f - dist / radius);
                    }
                }

                float valleyDepth = 0;
                for (float[] valley : valleys) {
                    float dx = x - valley[0];
                    float dy = y - valley[1];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float radius = valley[2];
                    if (dist < radius) {
                        valleyDepth -= 0.25f * (1.0f - dist / radius);
                    }
                }

                float finalHeight = baseHeight + wave1 + wave2 + noise + hillsHeight + valleyDepth;
                finalHeight = Math.max(0, Math.min(1, finalHeight));

                int gray = (int) (finalHeight * 255);
                int rgb = (gray << 16) | (gray << 8) | gray;
                heightmap.setRGB(x, y, rgb);
            }
        }

        try {
            ImageIO.write(heightmap, "PNG", new File(outputPath));
            System.out.println("Đã tạo heightmap trũng giữa, núi bao quanh: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Giả Perlin noise
    private static float pseudoRandom(float x, float y) {
        return (float) ((Math.sin(x * 12.9898 + y * 78.233) * 43758.5453) % 1.0);
    }

}