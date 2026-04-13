package com.chiaseyeuthuong.excel;

import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExcelErrorReportStorageService {

    private static final String REPORT_NOT_FOUND_MESSAGE = "Không tìm thấy file báo cáo lỗi import hoặc file đã hết hạn";
    private static final long REPORT_TTL_SECONDS = 60L * 60L;

    private final Map<String, StoredExcelReport> storage = new ConcurrentHashMap<>();

    public String store(byte[] content, String filename, String contentType) {
        cleanupExpiredReports();

        String token = UUID.randomUUID().toString();
        storage.put(token, new StoredExcelReport(content, filename, contentType, Instant.now().plusSeconds(REPORT_TTL_SECONDS)));
        return token;
    }

    public StoredExcelReport get(String token) {
        cleanupExpiredReports();

        StoredExcelReport report = storage.get(token);
        if (report == null || report.isExpired()) {
            storage.remove(token);
            throw new ResourceNotFoundException(REPORT_NOT_FOUND_MESSAGE);
        }
        return report;
    }

    private void cleanupExpiredReports() {
        Instant now = Instant.now();
        storage.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record StoredExcelReport(byte[] content, String filename, String contentType, Instant expiresAt) {
        public boolean isExpired() {
            return expiresAt.isBefore(Instant.now());
        }
    }
}
