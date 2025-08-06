package ctu.game.isometric.model.perform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

public class RealTimePerformanceMonitor {
    private static RealTimePerformanceMonitor instance;

    // Circular buffers for history tracking
    private final CircularBuffer<Float> fpsHistory;
    private final CircularBuffer<Long> memoryHistory;
    private final CircularBuffer<Long> heapHistory;
    private final CircularBuffer<Long> nonHeapHistory;

    // Real-time metrics
    private float currentFPS;
    private long currentMemoryMB;
    private float avgFPS;
    private long avgMemoryMB;
    private boolean showDebugOverlay;
    private boolean monitoring;

    // Performance warning thresholds
    private static final float LOW_FPS_THRESHOLD = 25f;
    private static final long HIGH_MEMORY_THRESHOLD = 600; // MB
    private static final int HISTORY_SIZE_FPS = 300; // 5 seconds at 60fps
    private static final int HISTORY_SIZE_MEMORY = 60;  // 1 minute at 1 sample/sec

    // Timer for periodic updates
    private Timer.Task memoryUpdateTask;
    private Timer.Task debugUpdateTask;

    private RealTimePerformanceMonitor() {
        this.fpsHistory = new CircularBuffer<>(HISTORY_SIZE_FPS);
        this.memoryHistory = new CircularBuffer<>(HISTORY_SIZE_MEMORY);
        this.heapHistory = new CircularBuffer<>(HISTORY_SIZE_MEMORY);
        this.nonHeapHistory = new CircularBuffer<>(HISTORY_SIZE_MEMORY);
        this.showDebugOverlay = false;
        this.monitoring = false;
    }

    public static RealTimePerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new RealTimePerformanceMonitor();
        }
        return instance;
    }

    /**
     * Bắt đầu monitoring real-time
     * Gọi trong IsometricGame.create() sau khi render loop đã active
     */
    public void startMonitoring() {
        if (monitoring) return;

        monitoring = true;
        System.out.println("🔄 Bắt đầu Real-time Performance Monitoring...");

        // Schedule memory monitoring mỗi giây
        memoryUpdateTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                updateMemoryMetrics();
            }
        }, 1f, 1f); // delay 1s, repeat every 1s

        // Schedule debug info update mỗi 0.5 giây
        debugUpdateTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                updateAverages();
                checkPerformanceWarnings();
            }
        }, 0.5f, 0.5f);
    }

    /**
     * Gọi trong IsometricGame.render() mỗi frame
     */
    public void updateFrame() {
        if (!monitoring) return;

        // Record FPS mỗi frame
        currentFPS = Gdx.graphics.getFramesPerSecond();
        if (currentFPS > 0) {
            fpsHistory.add(currentFPS);
        }
    }

    /**
     * Update memory metrics (chạy mỗi giây)
     */
    private void updateMemoryMetrics() {
        EnhancedMemoryMonitor.MemoryUsage usage = EnhancedMemoryMonitor.getDetailedMemoryUsage();

        currentMemoryMB = usage.totalSystemMB;
        memoryHistory.add(currentMemoryMB);
        heapHistory.add(usage.heapUsedMB);
        nonHeapHistory.add(usage.nonHeapUsedMB);
    }

    /**
     * Tính toán averages
     */
    private void updateAverages() {
        // Calculate average FPS
        if (!fpsHistory.isEmpty()) {
            float sum = 0;
            for (Float fps : fpsHistory.getItems()) {
                sum += fps;
            }
            avgFPS = sum / fpsHistory.size();
        }

        // Calculate average memory
        if (!memoryHistory.isEmpty()) {
            long sum = 0;
            for (Long memory : memoryHistory.getItems()) {
                sum += memory;
            }
            avgMemoryMB = sum / memoryHistory.size();
        }
    }

    /**
     * Kiểm tra warnings về hiệu năng
     */
    private void checkPerformanceWarnings() {
        if (avgFPS < LOW_FPS_THRESHOLD) {
            System.out.println("⚠️ WARNING: Low FPS detected - " + String.format("%.1f", avgFPS));
        }

        if (currentMemoryMB > HIGH_MEMORY_THRESHOLD) {
            System.out.println("⚠️ WARNING: High memory usage - " + currentMemoryMB + " MB");
        }
    }

    /**
     * Render debug overlay lên screen
     * Gọi sau khi render game content
     */
    public void renderDebugOverlay(SpriteBatch batch, BitmapFont font) {
        if (!showDebugOverlay || !monitoring) return;

        batch.begin();

        // Background với transparency
        // Có thể dùng NinePatch hoặc simple colored rectangle

        int y = Gdx.graphics.getHeight() - 20;
        int lineHeight = 25;

        // FPS info với color coding
        Color fpsColor = currentFPS >= LOW_FPS_THRESHOLD ? Color.GREEN : Color.RED;
        font.setColor(fpsColor);
        font.draw(batch, String.format("FPS: %.1f (Avg: %.1f)", currentFPS, avgFPS), 10, y);
        y -= lineHeight;

        // Memory info với detail breakdown
        font.setColor(currentMemoryMB > HIGH_MEMORY_THRESHOLD ? Color.RED : Color.WHITE);
        font.draw(batch, String.format("Memory: %d MB", currentMemoryMB), 10, y);
        y -= lineHeight;

        // Detailed memory breakdown
        if (!heapHistory.isEmpty() && !nonHeapHistory.isEmpty()) {
            font.setColor(Color.CYAN);
            font.draw(batch, String.format("Heap: %d MB | Non-heap: %d MB",
                    heapHistory.getLast(), nonHeapHistory.getLast()), 10, y);
            y -= lineHeight;
        }

        // Performance level indicator
        String perfLevel = getPerformanceLevel();
        Color levelColor = getPerfLevelColor(perfLevel);
        font.setColor(levelColor);
        font.draw(batch, "Performance: " + perfLevel, 10, y);

        font.setColor(Color.WHITE); // Reset color
        batch.end();
    }

    /**
     * Đánh giá mức độ hiệu năng hiện tại
     */
    private String getPerformanceLevel() {
        if (avgFPS >= 55 && currentMemoryMB <= 400) return "EXCELLENT";
        if (avgFPS >= 45 && currentMemoryMB <= 600) return "GOOD";
        if (avgFPS >= 30 && currentMemoryMB <= 800) return "ACCEPTABLE";
        if (avgFPS >= 20) return "POOR";
        return "CRITICAL";
    }

    private Color getPerfLevelColor(String level) {
        switch (level) {
            case "EXCELLENT": return Color.GREEN;
            case "GOOD": return Color.CYAN;
            case "ACCEPTABLE": return Color.YELLOW;
            case "POOR": return Color.ORANGE;
            case "CRITICAL": return Color.RED;
            default: return Color.WHITE;
        }
    }

    /**
     * Lấy báo cáo hiệu năng chi tiết
     */
    public PerformanceReport generateReport() {
        updateAverages();

        return new PerformanceReport(
                avgFPS, currentFPS, fpsHistory.getMin(), fpsHistory.getMax(),
                avgMemoryMB, currentMemoryMB,
                heapHistory.isEmpty() ? 0 : heapHistory.getLast(),
                nonHeapHistory.isEmpty() ? 0 : nonHeapHistory.getLast(),
                getPerformanceLevel()
        );
    }

    /**
     * Toggle debug overlay on/off
     */
    public void toggleDebugOverlay() {
        showDebugOverlay = !showDebugOverlay;
        System.out.println("🎯 Debug overlay: " + (showDebugOverlay ? "ON" : "OFF"));
    }

    /**
     * Export performance data to console
     */
    public void exportPerformanceLog() {
        System.out.println("\n📊 === PERFORMANCE LOG EXPORT ===");
        System.out.println("FPS - Current: " + String.format("%.1f", currentFPS) +
                " | Average: " + String.format("%.1f", avgFPS));
        System.out.println("Memory - Current: " + currentMemoryMB + " MB" +
                " | Average: " + avgMemoryMB + " MB");
        System.out.println("Performance Level: " + getPerformanceLevel());
        System.out.println("Samples - FPS: " + fpsHistory.size() +
                " | Memory: " + memoryHistory.size());
        System.out.println("=====================================\n");
    }

    /**
     * Stop monitoring và cleanup
     */
    public void stopMonitoring() {
        if (!monitoring) return;

        monitoring = false;

        if (memoryUpdateTask != null) {
            memoryUpdateTask.cancel();
        }
        if (debugUpdateTask != null) {
            debugUpdateTask.cancel();
        }

        System.out.println("🛑 Real-time Performance Monitoring stopped");
        exportPerformanceLog(); // Final report
    }

    // Getters
    public float getCurrentFPS() { return currentFPS; }
    public long getCurrentMemoryMB() { return currentMemoryMB; }
    public float getAverageFPS() { return avgFPS; }
    public long getAverageMemoryMB() { return avgMemoryMB; }
    public boolean isShowingDebugOverlay() { return showDebugOverlay; }
    public boolean isMonitoring() { return monitoring; }

    /**
     * Performance Report class
     */
    public static class PerformanceReport {
        public final float avgFPS, currentFPS, minFPS, maxFPS;
        public final long avgMemory, currentMemory, heapMemory, nonHeapMemory;
        public final String performanceLevel;

        public PerformanceReport(float avgFPS, float currentFPS, float minFPS, float maxFPS,
                                 long avgMemory, long currentMemory, long heapMemory, long nonHeapMemory,
                                 String performanceLevel) {
            this.avgFPS = avgFPS;
            this.currentFPS = currentFPS;
            this.minFPS = minFPS;
            this.maxFPS = maxFPS;
            this.avgMemory = avgMemory;
            this.currentMemory = currentMemory;
            this.heapMemory = heapMemory;
            this.nonHeapMemory = nonHeapMemory;
            this.performanceLevel = performanceLevel;
        }

        @Override
        public String toString() {
            return String.format(
                    "Performance Report: FPS=%.1f (%.1f-%.1f), Memory=%dMB (%dH+%dNH), Level=%s",
                    avgFPS, minFPS, maxFPS, currentMemory, heapMemory, nonHeapMemory, performanceLevel
            );
        }
    }
}