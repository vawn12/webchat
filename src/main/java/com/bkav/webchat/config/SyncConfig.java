//package com.bkav.webchat.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.concurrent.Executor;
//import java.util.concurrent.ThreadPoolExecutor;
//
//@Configuration
//@EnableAsync // Kích hoạt khả năng chạy bất đồng bộ trong Spring
//public class SyncConfig {
//
//    @Bean(name = "pushNotificationTaskExecutor")
//    public Executor pushNotificationTaskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//
//        // 1. Số lượng thread luôn luôn duy trì
//        executor.setCorePoolSize(10);
//
//        // 2. Số lượng thread tối đa có thể mở rộng khi hàng đợi đầy
//        executor.setMaxPoolSize(50);
//
//        // 3. Số lượng tác vụ chờ trong hàng đợi trước khi tạo thêm thread mới
//        executor.setQueueCapacity(500);
//
//        // 4. Tiền tố tên thread để dễ dàng theo dõi trong log
//        executor.setThreadNamePrefix("PushNoti-");
//
//        // 5. Chiến lược xử lý khi mọi thread đều bận và hàng đợi đã đầy
//        // CallerRunsPolicy: Buộc luồng chính (Kafka Consumer) phải tự xử lý
//        // Điều này giúp tạo "áp lực ngược" (Backpressure) để hệ thống không bị quá tải
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//
//        executor.initialize();
//        return executor;
//    }
//}
