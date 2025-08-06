package ctu.game.isometric.model.perform;

import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PerformanceMonitor {
    private static PerformanceMonitor instance;
    private final Runtime runtime;
    private final List<Float> fpsHistory;
    private final List<Long> responseTimeHistory;
    private long startTime;
    private long gameStartTime;
    private boolean monitoring;
    private boolean gameStarted;

    private PerformanceMonitor() {
        this.runtime = Runtime.getRuntime();
        this.fpsHistory = new CopyOnWriteArrayList<>();
        this.responseTimeHistory = new ArrayList<>();
        this.monitoring = false;
        this.gameStarted = false;
    }

    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }

    public void markGameStart() {
        this.gameStartTime = System.currentTimeMillis();
        this.gameStarted = true;
    }

    public long getGameStartupTime() {
        return gameStarted ? (System.currentTimeMillis() - gameStartTime) : 0;
    }

    public void startMonitoring() {
        this.startTime = System.currentTimeMillis();
        this.monitoring = true;
        this.fpsHistory.clear();
        this.responseTimeHistory.clear();
    }

    public void recordFPS(float fps) {
        if (monitoring && fps > 0) {
            fpsHistory.add(fps);
        }
    }

    public void recordFPSFromGdx() {
        if (monitoring && Gdx.graphics != null) {
            recordFPS(Gdx.graphics.getFramesPerSecond());
        }
    }

    public void recordResponseTime(long responseTimeMs) {
        if (monitoring) {
            responseTimeHistory.add(responseTimeMs);
        }
    }

    public long getMemoryUsageMB() {
        // Force garbage collection để có kết quả chính xác hơn
        System.gc();
        try {
            Thread.sleep(10); // Cho GC hoàn thành
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }

    public float getAverageFPS() {
        if (fpsHistory.isEmpty()) return 0f;
        return (float) fpsHistory.stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);
    }

    public float getMinFPS() {
        if (fpsHistory.isEmpty()) return 0f;
        return Collections.min(fpsHistory);
    }

    public float getMaxFPS() {
        if (fpsHistory.isEmpty()) return 0f;
        return Collections.max(fpsHistory);
    }

    public long getAverageResponseTime() {
        if (responseTimeHistory.isEmpty()) return 0L;
        return (long) responseTimeHistory.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    public int getFPSSampleCount() {
        return fpsHistory.size();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public void stopMonitoring() {
        this.monitoring = false;
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    public void reset() {
        fpsHistory.clear();
        responseTimeHistory.clear();
        monitoring = false;
        gameStarted = false;
    }
}