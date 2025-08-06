package ctu.game.isometric.model.perform;

public class EnhancedMemoryMonitor {

    public static class MemoryUsage {
        public long heapUsedMB;
        public long heapTotalMB;
        public long heapMaxMB;
        public long nonHeapUsedMB;
        public long totalSystemMB;

        @Override
        public String toString() {
            return String.format(
                    "Heap: %d/%d/%d MB | Non-heap: %d MB | System: %d MB",
                    heapUsedMB, heapTotalMB, heapMaxMB, nonHeapUsedMB, totalSystemMB
            );
        }
    }

    public static MemoryUsage getDetailedMemoryUsage() {
        MemoryUsage usage = new MemoryUsage();

        // Heap memory
        Runtime runtime = Runtime.getRuntime();
        long heapTotal = runtime.totalMemory();
        long heapFree = runtime.freeMemory();
        long heapUsed = heapTotal - heapFree;
        long heapMax = runtime.maxMemory();

        usage.heapUsedMB = heapUsed / (1024 * 1024);
        usage.heapTotalMB = heapTotal / (1024 * 1024);
        usage.heapMaxMB = heapMax / (1024 * 1024);

        // Non-heap memory (ví dụ: direct buffers)
        try {
            java.lang.management.MemoryMXBean memoryBean =
                    java.lang.management.ManagementFactory.getMemoryMXBean();
            usage.nonHeapUsedMB = memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
        } catch (Exception e) {
            usage.nonHeapUsedMB = 0;
        }

        // Total system memory trong sử dụng bởi process
        usage.totalSystemMB = usage.heapUsedMB + usage.nonHeapUsedMB;

        return usage;
    }
}