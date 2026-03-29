package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.dto.response.ExcelImportResult;
import com.chiaseyeuthuong.service.AdminExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/excel")
public class ApiAdminExcelController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AdminExcelService adminExcelService;

    @GetMapping("/events/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ResponseEntity<byte[]> exportEvents(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) EEventStatus status,
                                               @RequestParam(required = false) String sortBy,
                                               @RequestParam(required = false) String sortDir,
                                               @RequestParam(required = false) String... categoryIds) {
        byte[] data = adminExcelService.exportEvents(search, status, sortBy, sortDir, categoryIds);
        return buildExcelResponse(data, "su-kien");
    }

    @PostMapping("/events/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse importEvents(@RequestParam("file") MultipartFile file) {
        ExcelImportResult result = adminExcelService.importEvents(file);
        return ApiResponse.builder()
                .status(200)
                .message(result.getMessage())
                .data(result)
                .build();
    }

    @GetMapping("/activities/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ResponseEntity<byte[]> exportActivities(@RequestParam(required = false) String search,
                                                   @RequestParam(required = false) EActivityStatus status) {
        byte[] data = adminExcelService.exportActivities(search, status);
        return buildExcelResponse(data, "hoat-dong");
    }

    @PostMapping("/activities/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse importActivities(@RequestParam("file") MultipartFile file) {
        ExcelImportResult result = adminExcelService.importActivities(file);
        return ApiResponse.builder()
                .status(200)
                .message(result.getMessage())
                .data(result)
                .build();
    }

    @GetMapping("/donors/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ResponseEntity<byte[]> exportDonors(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) EDonorType type,
                                               @RequestParam(required = false) String sortBy,
                                               @RequestParam(required = false) String sortDir) {
        byte[] data = adminExcelService.exportDonors(search, type, sortBy, sortDir);
        return buildExcelResponse(data, "nha-hao-tam");
    }

    @PostMapping("/donors/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse importDonors(@RequestParam("file") MultipartFile file) {
        ExcelImportResult result = adminExcelService.importDonors(file);
        return ApiResponse.builder()
                .status(200)
                .message(result.getMessage())
                .data(result)
                .build();
    }

    @GetMapping("/donations/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ResponseEntity<byte[]> exportDonations(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) EDonationStatus status,
                                                  @RequestParam(required = false) EDonationTarget target,
                                                  @RequestParam(required = false) EDonationType type,
                                                  @RequestParam(required = false) EPaymentMethod paymentMethod,
                                                  @RequestParam(required = false) BigDecimal minAmount,
                                                  @RequestParam(required = false) BigDecimal maxAmount) {
        byte[] data = adminExcelService.exportDonations(search, status, target, type, paymentMethod, minAmount, maxAmount);
        return buildExcelResponse(data, "quyen-gop");
    }

    @PostMapping("/donations/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse importDonations(@RequestParam("file") MultipartFile file, Principal principal) {
        ExcelImportResult result = adminExcelService.importDonations(file, principal.getName());
        return ApiResponse.builder()
                .status(200)
                .message(result.getMessage())
                .data(result)
                .build();
    }

    @GetMapping("/transactions/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ResponseEntity<byte[]> exportTransactions(@RequestParam(required = false) String search,
                                                     @RequestParam(required = false) EPaymentMethod method) {
        byte[] data = adminExcelService.exportTransactions(search, method);
        return buildExcelResponse(data, "giao-dich");
    }

    @PostMapping("/transactions/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse importTransactions(@RequestParam("file") MultipartFile file) {
        ExcelImportResult result = adminExcelService.importTransactions(file);
        return ApiResponse.builder()
                .status(200)
                .message(result.getMessage())
                .data(result)
                .build();
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String moduleName) {
        String filename = moduleName + "-" + LocalDateTime.now().format(FILE_NAME_FORMATTER) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(EXCEL_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(data);
    }
}
