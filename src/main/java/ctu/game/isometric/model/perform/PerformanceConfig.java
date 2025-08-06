package ctu.game.isometric.model.perform;

public class PerformanceConfig {
    // Cấu hình tối thiểu
    public static final long MIN_STARTUP_TIME_MS = 8200;
    public static final float MIN_FPS = 28f;
    public static final long MIN_MEMORY_MB = 485;
    public static final long MIN_LOADING_TIME_MS = 12000;
    public static final long MIN_RESPONSE_TIME_MS = 150;

    // Cấu hình khuyến nghị
    public static final long REC_STARTUP_TIME_MS = 4100;
    public static final float REC_FPS = 45f;
    public static final long REC_MEMORY_MB = 320;
    public static final long REC_LOADING_TIME_MS = 6000;
    public static final long REC_RESPONSE_TIME_MS = 80;

    // Mục tiêu
    public static final long TARGET_STARTUP_TIME_MS = 5000;
    public static final float TARGET_FPS = 30f;
    public static final long TARGET_MEMORY_MB = 512;
    public static final long TARGET_LOADING_TIME_MS = 10000;
    public static final long TARGET_RESPONSE_TIME_MS = 100;

    // Test duration settings
    public static final long FPS_TEST_DURATION_MS = 30000; // 30 giây
    public static final int RESPONSE_TIME_SAMPLES = 100;
}