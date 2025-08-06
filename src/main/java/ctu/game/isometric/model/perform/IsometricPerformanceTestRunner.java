package ctu.game.isometric.model.perform;


import ctu.game.isometric.IsometricGame;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class IsometricPerformanceTestRunner {
    private final IsometricPerformanceTestSuite testSuite;
    private final PerformanceEvaluator evaluator;
    private final IsometricGame game;

    public IsometricPerformanceTestRunner(IsometricGame game) {
        this.game = game;
        this.testSuite = new IsometricPerformanceTestSuite(game);
        this.evaluator = new PerformanceEvaluator();
    }

    public void runPerformanceTests() {
        System.out.println("🎮 === KIỂM THỬ HIỆU NĂNG LABYRINTH OF WISDOM ===");
        System.out.println("⏰ Thời gian: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        System.out.println("💻 Môi trường: " + System.getProperty("os.name") +
                " | Java " + System.getProperty("java.version"));
        System.out.println();

        long overallStartTime = System.currentTimeMillis();

        try {
            // Chạy test suite
            PerformanceMetrics metrics = testSuite.runFullPerformanceTest();

            // Đánh giá kết quả
            List<PerformanceEvaluator.PerformanceResult> results =
                    evaluator.evaluatePerformance(metrics);

            // Tạo báo cáo
            generateDetailedReport(metrics, results);

            long totalTestTime = System.currentTimeMillis() - overallStartTime;
            System.out.println("\n⌚ Tổng thời gian test: " + (totalTestTime / 1000.0) + " giây");

        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng trong quá trình testing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateDetailedReport(PerformanceMetrics metrics,
                                        List<PerformanceEvaluator.PerformanceResult> results) {
        System.out.println("\n🏆 === BÁO CÁO CHI TIẾT HIỆU NĂNG ===");

        // Header bảng
        System.out.printf("%-25s %-15s %-15s %-20s %-10s%n",
                "Chỉ số", "Kết quả", "Mục tiêu", "Mức độ", "Trạng thái");
        System.out.println("=".repeat(85));

        // Chi tiết từng metric
        for (PerformanceEvaluator.PerformanceResult result : results) {
            String status = result.isPassed() ? "✅ ĐẠT" : "❌ CHƯA ĐẠT";
            String level = getPerformanceLevelEmoji(result.getLevel()) + " " +
                    result.getLevel().getDescription();

            System.out.printf("%-25s %-15s %-15s %-20s %-10s%n",
                    result.getMetric(),
                    formatValue(result.getMetric(), result.getActualValue()),
                    formatValue(result.getMetric(), result.getTargetValue()),
                    level,
                    status
            );
        }

        System.out.println("=".repeat(85));

        // Thống kê chi tiết
        printDetailedStats(metrics, results);

        // Tổng kết và khuyến nghị
        printSummaryAndRecommendations(results);
    }

    private void printDetailedStats(PerformanceMetrics metrics,
                                    List<PerformanceEvaluator.PerformanceResult> results) {
        System.out.println("\n📊 THỐNG KÊ CHI TIẾT:");

        if (metrics.getMinFps() > 0 && metrics.getMaxFps() > 0) {
            System.out.printf("   🎯 FPS Details: Avg=%.1f | Min=%.1f | Max=%.1f%n",
                    metrics.getAverageFps(), metrics.getMinFps(), metrics.getMaxFps());
        }

        System.out.printf("   💾 Memory: %d MB (%.1f%% of target)%n",
                metrics.getMemoryUsageMB(),
                (metrics.getMemoryUsageMB() * 100.0) / PerformanceConfig.TARGET_MEMORY_MB);

        System.out.printf("   🚀 Startup: %.2f giây%n", metrics.getStartupTime() / 1000.0);
        System.out.printf("   ⚡ Response: %d ms%n", metrics.getResponseTime());
    }

    private void printSummaryAndRecommendations(List<PerformanceEvaluator.PerformanceResult> results) {
        long passedTests = results.stream()
                .mapToLong(r -> r.isPassed() ? 1 : 0)
                .sum();

        System.out.printf("\n🏁 TỔNG KẾT: %d/%d chỉ số đạt yêu cầu (%.1f%%)%n",
                passedTests, results.size(), (passedTests * 100.0) / results.size());

        if (passedTests == results.size()) {
            System.out.println("🎉 XUẤT SẮC! Tất cả các chỉ số hiệu năng đều đạt mục tiêu!");
            System.out.println("   Game sẵn sàng để release với hiệu năng tối ưu.");
        } else {
            System.out.println("⚠️  CẦN CẢI THIỆN: Một số chỉ số chưa đạt mục tiêu.");
            System.out.println("\n💡 KHUYẾN NGHỊ:");

            for (PerformanceEvaluator.PerformanceResult result : results) {
                if (!result.isPassed()) {
                    System.out.println("   • " + getRecommendation(result.getMetric()));
                }
            }
        }
    }

    private String getPerformanceLevelEmoji(PerformanceEvaluator.PerformanceLevel level) {
        switch (level) {
            case EXCELLENT: return "🏆";
            case RECOMMENDED: return "✅";
            case MINIMUM: return "⚠️";
            case BELOW_MINIMUM: return "❌";
            default: return "❓";
        }
    }

    private String formatValue(String metric, long value) {
        switch (metric) {
            case "Thời gian khởi động":
            case "Thời gian tải":
                return String.format("%.2f s", value / 1000.0);
            case "Thời gian phản hồi":
                return value + " ms";
            case "FPS trung bình":
                return String.format("%.1f FPS", (float) value);
            case "Sử dụng bộ nhớ":
                return value + " MB";
            default:
                return String.valueOf(value);
        }
    }

    private String getRecommendation(String metric) {
        switch (metric) {
            case "Thời gian khởi động":
                return "Tối ưu khởi tạo assets, sử dụng lazy loading";
            case "FPS trung bình":
                return "Tối ưu render loop, giảm draw calls, optimize textures";
            case "Sử dụng bộ nhớ":
                return "Giải phóng assets không dùng, optimize texture compression";
            case "Thời gian tải":
                return "Implement asset streaming, optimize file sizes";
            case "Thời gian phản hồi":
                return "Tối ưu input handling, giảm logic phức tạp trong main thread";
            default:
                return "Cần phân tích và tối ưu thêm";
        }
    }
}