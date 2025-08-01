package ctu.game.isometric.animation;

import java.util.Random;

public class SimplexNoise {
    private static int grad3[][] = {
            {1,1,0}, {-1,1,0}, {1,-1,0}, {-1,-1,0},
            {1,0,1}, {-1,0,1}, {1,0,-1}, {-1,0,-1},
            {0,1,1}, {0,-1,1}, {0,1,-1}, {0,-1,-1}
    };

    private static int p[] = new int[512];
    private static int perm[] = new int[512];

    static {
        for (int i = 0; i < 256; i++) p[i] = i;
        // Shuffle
        for (int i = 255; i > 0; i--) {
            int j = (int)(Math.random() * (i + 1));
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    /**
     * Tạo lại các bảng hoán vị với một seed cụ thể
     * @param seed Giá trị seed cho bộ sinh số ngẫu nhiên
     */
    public static void reseed(long seed) {
        Random random = new Random(seed);

        // Khởi tạo lại mảng p
        for (int i = 0; i < 256; i++) p[i] = i;

        // Xáo trộn với seed đã cho
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        // Cập nhật bảng hoán vị
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    private static float dot(int[] g, float x, float y) {
        return g[0]*x + g[1]*y;
    }
    public static float noise(float xin, float yin) {
        final float F2 = 0.5f * (float)(Math.sqrt(3.0) - 1.0);
        float s = (xin + yin) * F2;
        int i = (int)Math.floor(xin + s);
        int j = (int)Math.floor(yin + s);
        final float G2 = (3.0f - (float)Math.sqrt(3.0)) / 6.0f;
        float t = (i + j) * G2;
        float X0 = i - t;
        float Y0 = j - t;
        float x0 = xin - X0;
        float y0 = yin - Y0;

        int i1, j1;
        if (x0 > y0) { i1 = 1; j1 = 0; }
        else { i1 = 0; j1 = 1; }

        float x1 = x0 - i1 + G2;
        float y1 = y0 - j1 + G2;
        float x2 = x0 - 1.0f + 2.0f * G2;
        float y2 = y0 - 1.0f + 2.0f * G2;

        int ii = i & 255;
        int jj = j & 255;
        int gi0 = perm[ii + perm[jj]] % 12;
        int gi1 = perm[ii + i1 + perm[jj + j1]] % 12;
        int gi2 = perm[ii + 1 + perm[jj + 1]] % 12;

        float n0, n1, n2;

        float t0 = 0.5f - x0*x0 - y0*y0;
        if (t0 < 0) n0 = 0.0f;
        else {
            t0 *= t0;
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0);
        }

        float t1 = 0.5f - x1*x1 - y1*y1;
        if (t1 < 0) n1 = 0.0f;
        else {
            t1 *= t1;
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1);
        }

        float t2 = 0.5f - x2*x2 - y2*y2;
        if (t2 < 0) n2 = 0.0f;
        else {
            t2 *= t2;
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2);
        }

        return 70.0f * (n0 + n1 + n2);
    }
}
