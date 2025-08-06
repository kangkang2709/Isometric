package ctu.game.isometric.model.perform;

import java.util.Date;

public class PerformanceMetrics {
    private long startupTime;
    private float averageFps;
    private float minFps;
    private float maxFps;
    private long memoryUsageMB;
    private long loadingTime;
    private long responseTime;
    private long timestamp;
    private String testEnvironment;
    private String gameVersion;

    public PerformanceMetrics() {
        this.timestamp = System.currentTimeMillis();
        this.gameVersion = "1.0.0"; // Có thể lấy từ config
        this.testEnvironment = System.getProperty("os.name") + " " +
                              System.getProperty("java.version");
    }

    // Getters và Setters
    public long getStartupTime() { return startupTime; }
    public void setStartupTime(long startupTime) { this.startupTime = startupTime; }

    public float getAverageFps() { return averageFps; }
    public void setAverageFps(float averageFps) { this.averageFps = averageFps; }

    public float getMinFps() { return minFps; }
    public void setMinFps(float minFps) { this.minFps = minFps; }

    public float getMaxFps() { return maxFps; }
    public void setMaxFps(float maxFps) { this.maxFps = maxFps; }

    public long getMemoryUsageMB() { return memoryUsageMB; }
    public void setMemoryUsageMB(long memoryUsageMB) { this.memoryUsageMB = memoryUsageMB; }

    public long getLoadingTime() { return loadingTime; }
    public void setLoadingTime(long loadingTime) { this.loadingTime = loadingTime; }

    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

    public long getTimestamp() { return timestamp; }
    public String getTestEnvironment() { return testEnvironment; }
    public String getGameVersion() { return gameVersion; }

    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{startup=%dms, avgFPS=%.1f, memory=%dMB, loading=%dms, response=%dms, time=%s}",
            startupTime, averageFps, memoryUsageMB, loadingTime, responseTime, new Date(timestamp)
        );
    }
}