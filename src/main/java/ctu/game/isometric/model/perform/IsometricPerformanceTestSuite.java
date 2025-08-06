package ctu.game.isometric.model.perform;

import com.badlogic.gdx.Gdx;
import ctu.game.isometric.IsometricGame;

public class IsometricPerformanceTestSuite {
    private final PerformanceMonitor monitor;
    private final IsometricGame game;

    public IsometricPerformanceTestSuite(IsometricGame game) {
        this.monitor = PerformanceMonitor.getInstance();
        this.game = game;
    }

    public PerformanceMetrics runFullPerformanceTest() {
        System.out.println("🚀 Bắt đầu kiểm thử hiệu năng toàn diện...");

        PerformanceMetrics metrics = new PerformanceMetrics();

        try {
            // Test 1: Thời gian khởi động
            metrics.setStartupTime(testStartupTime());

            // Test 2: Hiệu năng FPS
            testFPSPerformance(metrics);

            // Test 3: Sử dụng bộ nhớ
            metrics.setMemoryUsageMB(testMemoryUsage());

            // Test 4: Thời gian tải screens
            metrics.setLoadingTime(testScreenLoadingTime());

            // Test 5: Thời gian phản hồi
            metrics.setResponseTime(testInputResponseTime());

        } catch (Exception e) {
            System.err.println("❌ Lỗi trong quá trình test: " + e.getMessage());
            e.printStackTrace();
        }

        return metrics;
    }

    private long testStartupTime() {
        System.out.println("📊 Test thời gian khởi động...");

        // Thời gian từ khi game được tạo đến khi sẵn sàng
        long startTime = System.currentTimeMillis();

        // Đợi game khởi tạo xong
        while (game.getAssetManager() == null || !game.getAssetManager().isFinished()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Timeout sau 15 giây
            if (System.currentTimeMillis() - startTime > 15000) {
                break;
            }
        }

        long startupTime = System.currentTimeMillis() - startTime;
        System.out.println("✅ Thời gian khởi động: " + startupTime + "ms");
        return startupTime;
    }

    private void testFPSPerformance(PerformanceMetrics metrics) {
        System.out.println("📊 Test hiệu năng FPS trong " +
            (PerformanceConfig.FPS_TEST_DURATION_MS / 1000) + " giây...");

        monitor.startMonitoring();

        long endTime = System.currentTimeMillis() + PerformanceConfig.FPS_TEST_DURATION_MS;
        int frameCount = 0;

        while (System.currentTimeMillis() < endTime) {
            monitor.recordFPSFromGdx();
            frameCount++;

            try {
                Thread.sleep(16); // ~60 FPS target
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        metrics.setAverageFps(monitor.getAverageFPS());
        metrics.setMinFps(monitor.getMinFPS());
        metrics.setMaxFps(monitor.getMaxFPS());

        System.out.printf("✅ FPS - Trung bình: %.1f, Min: %.1f, Max: %.1f (Samples: %d)%n",
            metrics.getAverageFps(), metrics.getMinFps(),
            metrics.getMaxFps(), monitor.getFPSSampleCount());

        monitor.stopMonitoring();
    }

    private long testMemoryUsage() {
        System.out.println("📊 Test sử dụng bộ nhớ...");

        // Đợi một chút để game ổn định
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Chuyển qua các screens khác nhau để load assets
        simulateScreenTransitions();

        long memoryUsage = monitor.getMemoryUsageMB();
        System.out.println("✅ Sử dụng bộ nhớ: " + memoryUsage + " MB");

        return memoryUsage;
    }

    private long testScreenLoadingTime() {
        System.out.println("📊 Test thời gian tải screens...");

        long totalLoadingTime = 0;
        String[] screens = {"SPLASH", "GAME", "DARK_DUNGEON", "CREDITS"};

        for (String screenName : screens) {
            long startTime = System.currentTimeMillis();

            // Chuyển screen
            if (Gdx.app != null) {
                Gdx.app.postRunnable(() -> game.changeScreen(screenName));

                // Đợi screen load xong
                try {
                    Thread.sleep(500); // Đợi transition
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            long loadTime = System.currentTimeMillis() - startTime;
            totalLoadingTime += loadTime;

            System.out.println("  📄 " + screenName + ": " + loadTime + "ms");
        }

        System.out.println("✅ Tổng thời gian tải: " + totalLoadingTime + "ms");
        return totalLoadingTime;
    }

    private long testInputResponseTime() {
        System.out.println("📊 Test thời gian phản hồi input...");

        monitor.startMonitoring();

        // Simulate input events
        for (int i = 0; i < PerformanceConfig.RESPONSE_TIME_SAMPLES; i++) {
            long startTime = System.nanoTime();

            // Simulate input processing
            if (Gdx.app != null) {
                Gdx.app.postRunnable(() -> {
                    // Simulate input handling
                    try {
                        Thread.sleep(1); // Simulate processing
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            long responseTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            monitor.recordResponseTime(responseTime);

            try {
                Thread.sleep(10); // Delay between tests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long avgResponseTime = monitor.getAverageResponseTime();
        System.out.println("✅ Thời gian phản hồi trung bình: " + avgResponseTime + "ms");

        monitor.stopMonitoring();
        return avgResponseTime;
    }

    private void simulateScreenTransitions() {
        String[] screens = {"SPLASH", "DARK_DUNGEON", "CREDITS"};

        for (String screenName : screens) {
            if (Gdx.app != null) {
                Gdx.app.postRunnable(() -> game.changeScreen(screenName));

                try {
                    Thread.sleep(300); // Đợi transition
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}