package com.bkav.webchat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

    @Service
    public class PartitionCreationService {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        /**
         * tự động chạy vào 01:00 sáng ngày 28 hàng tháng.
         * chọn ngày 28 là để đảm bảo phân vùng của tháng sau
         * luôn được tạo trước khi tháng mới bắt đầu.
         */
        @Scheduled(cron = "0 0 1 28 * ?")
        public void createNextMonthMessagePartition() {
            try {

                // Ví dụ: Hôm nay là 28/11/2025 -> nextMonth là 01/12/2025
                LocalDate nextMonth = LocalDate.now().plusMonths(1);

                // Lấy ngày đầu tiên của tháng tiếp theo (mốc BẮT ĐẦU)
                LocalDate partitionStartDate = nextMonth.withDayOfMonth(1);

                //  Lấy ngày đầu tiên của tháng SAU ĐÓ
                LocalDate partitionEndDate = partitionStartDate.plusMonths(1);

                // tạo bảng
                String partitionTableName = String.format(
                        "Message_Partition_y%dm%02d",
                        partitionStartDate.getYear(),
                        partitionStartDate.getMonthValue()
                );

                // Định dạng ngày tháng thành chuỗi SQL
                DateTimeFormatter sqlDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
                String startDateString = partitionStartDate.format(sqlDateFormatter);
                String endDateString = partitionEndDate.format(sqlDateFormatter);

                String sql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s " +
                                "PARTITION OF Message " +
                                "FOR VALUES FROM ('%s') TO ('%s');",
                        partitionTableName,
                        startDateString,
                        endDateString
                );

                System.out.println("[Scheduler] Đang thực thi: " + sql);
                jdbcTemplate.execute(sql);
                System.out.println("[Scheduler] Đã tạo thành công phân vùng: " + partitionTableName);

            } catch (Exception e) {
                System.err.println("[Scheduler] Lỗi khi tạo phân vùng: " + e.getMessage());
            }
        }
    }

