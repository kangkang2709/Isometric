package ctu.game.isometric.model.perform;

import java.util.ArrayList;
import java.util.List;

public class PerformanceEvaluator {

    public enum PerformanceLevel {
        BELOW_MINIMUM("Dưới cấu hình tối thiểu"),
        MINIMUM("Đạt cấu hình tối thiểu"),
        RECOMMENDED("Đạt cấu hình khuyến nghị"),
        EXCELLENT("Xuất sắc");

        private final String description;

        PerformanceLevel(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    public static class PerformanceResult {
        private final String metric;
        private final long actualValue;
        private final long targetValue;
        private final PerformanceLevel level;
        private final boolean passed;

        public PerformanceResult(String metric, long actualValue, long targetValue,
                               PerformanceLevel level, boolean passed) {
            this.metric = metric;
            this.actualValue = actualValue;
            this.targetValue = targetValue;
            this.level = level;
            this.passed = passed;
        }

        // Getters
        public String getMetric() { return metric; }
        public long getActualValue() { return actualValue; }
        public long getTargetValue() { return targetValue; }
        public PerformanceLevel getLevel() { return level; }
        public boolean isPassed() { return passed; }
    }

    public List<PerformanceResult> evaluatePerformance(PerformanceMetrics metrics) {
        List<PerformanceResult> results = new ArrayList<>();

        // Đánh giá thời gian khởi động
        results.add(evaluateStartupTime(metrics.getStartupTime()));

        // Đánh giá FPS
        results.add(evaluateFPS(metrics.getAverageFps()));

        // Đánh giá bộ nhớ
        results.add(evaluateMemoryUsage(metrics.getMemoryUsageMB()));

        // Đánh giá thời gian tải
        results.add(evaluateLoadingTime(metrics.getLoadingTime()));

        // Đánh giá thời gian phản hồi
        results.add(evaluateResponseTime(metrics.getResponseTime()));

        return results;
    }

    private PerformanceResult evaluateStartupTime(long actualTime) {
        boolean passed = actualTime <= PerformanceConfig.TARGET_STARTUP_TIME_MS;
        PerformanceLevel level = getPerformanceLevel(
            actualTime,
            PerformanceConfig.MIN_STARTUP_TIME_MS,
            PerformanceConfig.REC_STARTUP_TIME_MS,
            PerformanceConfig.TARGET_STARTUP_TIME_MS,
            true // lower is better
        );

        return new PerformanceResult(
            "Thời gian khởi động",
            actualTime,
            PerformanceConfig.TARGET_STARTUP_TIME_MS,
            level,
            passed
        );
    }

    private PerformanceResult evaluateFPS(float actualFps) {
        boolean passed = actualFps >= PerformanceConfig.TARGET_FPS;
        PerformanceLevel level = getPerformanceLevel(
            (long) actualFps,
            (long) PerformanceConfig.MIN_FPS,
            (long) PerformanceConfig.REC_FPS,
            (long) PerformanceConfig.TARGET_FPS,
            false // higher is better
        );

        return new PerformanceResult(
            "FPS trung bình",
            (long) actualFps,
            (long) PerformanceConfig.TARGET_FPS,
            level,
            passed
        );
    }

    private PerformanceResult evaluateMemoryUsage(long actualMemory) {
        boolean passed = actualMemory <= PerformanceConfig.TARGET_MEMORY_MB;
        PerformanceLevel level = getPerformanceLevel(
            actualMemory,
            PerformanceConfig.MIN_MEMORY_MB,
            PerformanceConfig.REC_MEMORY_MB,
            PerformanceConfig.TARGET_MEMORY_MB,
            true // lower is better
        );

        return new PerformanceResult(
            "Sử dụng bộ nhớ",
            actualMemory,
            PerformanceConfig.TARGET_MEMORY_MB,
            level,
            passed
        );
    }

    private PerformanceResult evaluateLoadingTime(long actualTime) {
        boolean passed = actualTime <= PerformanceConfig.TARGET_LOADING_TIME_MS;
        PerformanceLevel level = getPerformanceLevel(
            actualTime,
            PerformanceConfig.MIN_LOADING_TIME_MS,
            PerformanceConfig.REC_LOADING_TIME_MS,
            PerformanceConfig.TARGET_LOADING_TIME_MS,
            true // lower is better
        );

        return new PerformanceResult(
            "Thời gian tải",
            actualTime,
            PerformanceConfig.TARGET_LOADING_TIME_MS,
            level,
            passed
        );
    }

    private PerformanceResult evaluateResponseTime(long actualTime) {
        boolean passed = actualTime <= PerformanceConfig.TARGET_RESPONSE_TIME_MS;
        PerformanceLevel level = getPerformanceLevel(
            actualTime,
            PerformanceConfig.MIN_RESPONSE_TIME_MS,
            PerformanceConfig.REC_RESPONSE_TIME_MS,
            PerformanceConfig.TARGET_RESPONSE_TIME_MS,
            true // lower is better
        );

        return new PerformanceResult(
            "Thời gian phản hồi",
            actualTime,
            PerformanceConfig.TARGET_RESPONSE_TIME_MS,
            level,
            passed
        );
    }

    private PerformanceLevel getPerformanceLevel(long actual, long min, long recommended,
                                               long target, boolean lowerIsBetter) {
        if (lowerIsBetter) {
            if (actual <= recommended) return PerformanceLevel.EXCELLENT;
            if (actual <= target) return PerformanceLevel.RECOMMENDED;
            if (actual <= min) return PerformanceLevel.MINIMUM;
            return PerformanceLevel.BELOW_MINIMUM;
        } else {
            if (actual >= recommended) return PerformanceLevel.EXCELLENT;
            if (actual >= target) return PerformanceLevel.RECOMMENDED;
            if (actual >= min) return PerformanceLevel.MINIMUM;
            return PerformanceLevel.BELOW_MINIMUM;
        }
    }
}