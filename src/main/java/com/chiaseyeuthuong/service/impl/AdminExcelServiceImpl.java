package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.ExcelImportErrorDetail;
import com.chiaseyeuthuong.dto.response.*;
import com.chiaseyeuthuong.excel.*;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.*;
import com.chiaseyeuthuong.repository.*;
import com.chiaseyeuthuong.service.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-EXCEL-SERVICE")
public class AdminExcelServiceImpl implements AdminExcelService {

    private static final int MAX_EXPORT_SIZE = 100_000;
    private static final int MAX_ERROR_LINES_IN_MESSAGE = 10;
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EVENT_DATA_SHEET_NAME = "SuKien";
    private static final String ACTIVITY_DATA_SHEET_NAME = "HoatDong";
    private static final String DONOR_DATA_SHEET_NAME = "NhaHaoTam";
    private static final String DONATION_DATA_SHEET_NAME = "QuyenGop";
    private static final String TRANSACTION_DATA_SHEET_NAME = "LichSuGiaoDich";
    private static final String GUIDE_SHEET_NAME = "HuongDanNhapLieu";
    private static final String REFERENCE_SHEET_NAME = "DanhMucThamChieu";
    private static final String DONOR_GUIDE_SHEET_NAME = "HuongDanNhapLieu";
    private static final String DONOR_REFERENCE_SHEET_NAME = "DanhMucThamChieu";
    private static final String SYSTEM_RECORD_ID_FIELD = "systemRecordId";
    private static final String SYSTEM_RECORD_ID_HEADER = "__SYSTEM_RECORD_ID";
    private static final String SYSTEM_RECORD_ID_RULE = "Cột kỹ thuật ẩn, không đổi tên, không xóa, không chỉnh sửa. Có giá trị nghĩa là cập nhật bản ghi cũ; để trống khi thêm dòng mới nghĩa là tạo bản ghi mới.";
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d.M.yyyy")
    );
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy H:m"),
            DateTimeFormatter.ofPattern("d/M/yyyy H:m:s"),
            DateTimeFormatter.ofPattern("d-M-yyyy H:m"),
            DateTimeFormatter.ofPattern("d-M-yyyy H:m:s"),
            DateTimeFormatter.ofPattern("d.M.yyyy H:m"),
            DateTimeFormatter.ofPattern("d.M.yyyy H:m:s"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:m"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:m:s"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.forLanguageTag("vi-VN"));

    private final EventService eventService;
    private final ActivityService activityService;
    private final DonorService donorService;
    private final DonationService donationService;
    private final TransactionService transactionService;
    private final ExcelImportExecutionService excelImportExecutionService;

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;

    private final Validator validator;
    private final ExcelErrorReportStorageService excelErrorReportStorageService;
    private final PlatformTransactionManager platformTransactionManager;
    private final ExcelWorkbookSupport excelWorkbookSupport = new ExcelWorkbookSupport();

    @Override
    public byte[] exportEvents(String search, EEventStatus status, String sortBy, String sortDir, String... categoryIds) {
        List<EventResponse> events = eventService
                .getAllEvents(1, MAX_EXPORT_SIZE, sortBy, sortDir, search, status, false, categoryIds)
                .getData();
        return buildEventWorkbook(events);
    }

    @Override
    public byte[] downloadEventsTemplate() {
        return buildEventWorkbook(List.of());
    }

    @Override
    public ExcelImportResult importEvents(MultipartFile file) {
        Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(category -> normalizeHeader(category.getName()), category -> category, (left, right) -> left));
        List<ExcelTemplateColumn> columns = buildEventColumns();

        return excelImportExecutionService.execute(new ExcelImportRequest(
                file,
                "sự kiện",
                EVENT_DATA_SHEET_NAME,
                context -> validateTemplateHeaders(context.headers(), columns),
                (context, rowIndex, displayRowNumber) -> processEventImportRow(
                        context.rowAt(rowIndex),
                        context.headers(),
                        categoriesByName,
                        displayRowNumber
                ),
                ExcelImportMode.CONTINUE_ON_ERROR,
                "ket-qua-import-loi-su-kien.xlsx"
        ));
    }

    @Override
    public byte[] exportActivities(String search, EActivityStatus status, String sortBy, String sortDir) {
        List<ActivityResponse> activities = activityService
                .getAllActivities(1, MAX_EXPORT_SIZE, sortBy, sortDir, search, status, false)
                .getData();
        return buildActivityWorkbook(activities);
    }

    @Override
    public byte[] downloadActivitiesTemplate() {
        return buildActivityWorkbook(List.of());
    }

    @Override
    public ExcelImportResult importActivities(MultipartFile file) {
        Map<String, Event> eventsByName = eventRepository.findAll().stream()
                .collect(Collectors.toMap(event -> normalizeHeader(event.getName()), event -> event, (left, right) -> left));
        List<ExcelTemplateColumn> columns = buildActivityColumns();

        return excelImportExecutionService.execute(new ExcelImportRequest(
                file,
                "hoạt động",
                ACTIVITY_DATA_SHEET_NAME,
                context -> validateTemplateHeaders(context.headers(), columns),
                (context, rowIndex, displayRowNumber) -> processActivityImportRow(
                        context.rowAt(rowIndex),
                        context.headers(),
                        eventsByName,
                        displayRowNumber
                ),
                ExcelImportMode.CONTINUE_ON_ERROR,
                "ket-qua-import-loi-hoat-dong.xlsx"
        ));
    }

    @Override
    public byte[] exportDonors(String search, EDonorType type, String sortBy, String sortDir) {
        List<DonorResponse> donors = donorService
                .getAllDonor(1, MAX_EXPORT_SIZE, search, type, sortBy, sortDir)
                .getData();

        return buildDonorWorkbook(donors, false);
    }

    @Override
    public byte[] downloadDonorsTemplate() {
        return buildDonorWorkbook(List.of(), true);
    }

    @Override
    public ExcelImportResult importDonors(MultipartFile file) {
        return excelImportExecutionService.execute(new ExcelImportRequest(
                file,
                "nhà hảo tâm",
                DONOR_DATA_SHEET_NAME,
                context -> validateDonorTemplateHeaders(context.headers()),
                (context, rowIndex, displayRowNumber) -> processDonorImportRow(
                        context.rowAt(rowIndex),
                        context.headers(),
                        displayRowNumber
                ),
                ExcelImportMode.CONTINUE_ON_ERROR,
                "ket-qua-import-loi-nha-hao-tam.xlsx"
        ));
    }

    @Override
    public byte[] exportDonations(String search, EDonationStatus status, EDonationTarget target, EDonationType type,
                                  EPaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount,
                                  String sortBy, String sortDir) {
        List<DonationResponse> donations = donationService
                .getAllDonations(search, status, target, type, paymentMethod, minAmount, maxAmount,
                        sortBy, sortDir, 1, MAX_EXPORT_SIZE)
                .getData();

        Map<Long, Donation> donationById = donationRepository.findAllById(
                donations.stream().map(DonationResponse::getId).filter(Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(Donation::getId, donation -> donation));
        return buildDonationWorkbook(donations, donationById);
    }

    @Override
    public byte[] downloadDonationsTemplate() {
        return buildDonationWorkbook(List.of(), Map.of());
    }

    @Override
    public ExcelImportResult importDonations(MultipartFile file, String username) {
        Map<String, Event> eventsByName = eventRepository.findAll().stream()
                .collect(Collectors.toMap(event -> normalizeHeader(event.getName()), event -> event, (left, right) -> left));
        Map<String, Activity> activitiesByName = activityRepository.findAll().stream()
                .collect(Collectors.toMap(activity -> normalizeHeader(activity.getName()), activity -> activity, (left, right) -> left));
        List<ExcelTemplateColumn> columns = buildDonationColumns();

        return excelImportExecutionService.execute(new ExcelImportRequest(
                file,
                "quyên góp",
                DONATION_DATA_SHEET_NAME,
                context -> validateTemplateHeaders(context.headers(), columns),
                (context, rowIndex, displayRowNumber) -> processDonationImportRow(
                        context.rowAt(rowIndex),
                        context.headers(),
                        eventsByName,
                        activitiesByName,
                        username,
                        displayRowNumber
                ),
                ExcelImportMode.CONTINUE_ON_ERROR,
                "ket-qua-import-loi-quyen-gop.xlsx"
        ));
    }

    @Override
    public byte[] exportTransactions(String search, EPaymentMethod method, String sortBy, String sortDir) {
        List<TransactionResponse> transactions = transactionService
                .getTransactions(1, MAX_EXPORT_SIZE, search, method, sortBy, sortDir)
                .getData();
        return buildTransactionWorkbook(transactions);
    }

    @Override
    public byte[] downloadTransactionsTemplate() {
        return buildTransactionWorkbook(List.of());
    }

    @Override
    public ExcelImportResult importTransactions(MultipartFile file) {
        List<ExcelTemplateColumn> columns = buildTransactionColumns();

        return excelImportExecutionService.execute(new ExcelImportRequest(
                file,
                "giao dịch",
                TRANSACTION_DATA_SHEET_NAME,
                context -> validateTemplateHeaders(context.headers(), columns),
                (context, rowIndex, displayRowNumber) -> processTransactionImportRow(
                        context.rowAt(rowIndex),
                        context.headers(),
                        displayRowNumber
                ),
                ExcelImportMode.CONTINUE_ON_ERROR,
                "ket-qua-import-loi-giao-dich.xlsx"
        ));
    }

    private byte[] buildEventWorkbook(List<EventResponse> events) {
        List<ExcelTemplateColumn> columns = buildEventColumns();
        List<ExcelReferenceItem> references = new ArrayList<>();
        categoryRepository.findAll().forEach(category -> references.add(new ExcelReferenceItem(
                "Danh mục sự kiện",
                category.getId() + " - " + category.getName(),
                "Nhập ID danh mục ở cột Danh mục ID"
        )));
        eventStatusValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Trạng thái sự kiện",
                value,
                "Giá trị hợp lệ cho cột Trạng thái"
        )));

        return buildStyledWorkbook(
                EVENT_DATA_SHEET_NAME,
                columns,
                references,
                events,
                this::writeEventDataRow,
                "Không thể xuất file Excel sự kiện"
        );
    }

    private byte[] buildActivityWorkbook(List<ActivityResponse> activities) {
        List<ExcelTemplateColumn> columns = buildActivityColumns();
        List<ExcelReferenceItem> references = new ArrayList<>();
        eventRepository.findAll().forEach(event -> references.add(new ExcelReferenceItem(
                "Sự kiện",
                event.getId() + " - " + event.getName(),
                "Nhập ID sự kiện ở cột Sự kiện ID"
        )));
        activityStatusValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Trạng thái hoạt động",
                value,
                "Giá trị hợp lệ cho cột Trạng thái"
        )));

        return buildStyledWorkbook(
                ACTIVITY_DATA_SHEET_NAME,
                columns,
                references,
                activities,
                this::writeActivityDataRow,
                "Không thể xuất file Excel hoạt động"
        );
    }

    private byte[] buildDonationWorkbook(List<DonationResponse> donations, Map<Long, Donation> donationById) {
        List<ExcelTemplateColumn> columns = buildDonationColumns();
        List<ExcelReferenceItem> references = new ArrayList<>();
        donorRepository.findAll().forEach(donor -> references.add(new ExcelReferenceItem(
                "Nhà hảo tâm",
                donor.getId() + " - " + defaultIfBlank(donor.getFullName(), donor.getPhone()),
                "Nhập ID nhà hảo tâm ở cột Nhà hảo tâm ID"
        )));
        eventRepository.findAll().forEach(event -> references.add(new ExcelReferenceItem(
                "Sự kiện",
                event.getId() + " - " + event.getName(),
                "Dùng khi Mục tiêu là Sự kiện"
        )));
        activityRepository.findAll().forEach(activity -> references.add(new ExcelReferenceItem(
                "Hoạt động",
                activity.getId() + " - " + activity.getName(),
                "Dùng khi Mục tiêu là Hoạt động"
        )));
        paymentMethodValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Phương thức thanh toán",
                value,
                "Giá trị hợp lệ cho cột Phương thức"
        )));
        donationStatusValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Trạng thái quyên góp",
                value,
                "Giá trị hợp lệ cho cột Trạng thái"
        )));
        donationTargetValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Mục tiêu quyên góp",
                value,
                "Giá trị hợp lệ cho cột Mục tiêu"
        )));
        yesNoValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Cần biên lai",
                value,
                "Giá trị hợp lệ cho cột Cần biên lai"
        )));

        return buildStyledWorkbook(
                DONATION_DATA_SHEET_NAME,
                columns,
                references,
                donations,
                (row, donation, styles, rowIndex) -> writeDonationDataRow(row, donation, donationById.get(donation.getId()), styles, rowIndex),
                "Không thể xuất file Excel quyên góp"
        );
    }

    private byte[] buildTransactionWorkbook(List<TransactionResponse> transactions) {
        List<ExcelTemplateColumn> columns = buildTransactionColumns();
        List<ExcelReferenceItem> references = new ArrayList<>();
        paymentMethodValues().forEach(value -> references.add(new ExcelReferenceItem(
                "Phương thức thanh toán",
                value,
                "Giá trị hợp lệ cho cột Phương thức"
        )));
        donationRepository.findAll().forEach(donation -> references.add(new ExcelReferenceItem(
                "Đơn quyên góp",
                donation.getId() + " - " + donation.getMemoCode(),
                "Nhập ID đơn quyên góp ở cột Đơn quyên góp ID"
        )));

        return buildStyledWorkbook(
                TRANSACTION_DATA_SHEET_NAME,
                columns,
                references,
                transactions,
                this::writeTransactionDataRow,
                "Không thể xuất file Excel giao dịch"
        );
    }

    private <T> byte[] buildStyledWorkbook(String dataSheetName,
                                           List<ExcelTemplateColumn> columns,
                                           List<ExcelReferenceItem> referenceItems,
                                           List<T> rows,
                                           WorkbookRowWriter<T> rowWriter,
                                           String errorMessage) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ExcelWorkbookSupport.WorkbookStyles styles = excelWorkbookSupport.createStyles(workbook);
            Sheet dataSheet = workbook.createSheet(dataSheetName);
            excelWorkbookSupport.writeHeaderRow(dataSheet, columns, styles);

            int rowIndex = 1;
            for (T rowItem : rows) {
                Row row = dataSheet.createRow(rowIndex);
                rowWriter.write(row, rowItem, styles, rowIndex);
                rowIndex++;
            }

            applyDropdowns(dataSheet, columns);

            Sheet guideSheet = workbook.createSheet(GUIDE_SHEET_NAME);
            excelWorkbookSupport.writeGuideSheet(guideSheet, columns, styles);

            Sheet referenceSheet = workbook.createSheet(REFERENCE_SHEET_NAME);
            List<ExcelReferenceItem> safeReferences = referenceItems == null || referenceItems.isEmpty()
                    ? List.of(new ExcelReferenceItem("Ghi chú", "Không có danh mục tham chiếu cố định", ""))
                    : referenceItems;
            excelWorkbookSupport.writeReferenceSheet(referenceSheet, safeReferences, styles);

            excelWorkbookSupport.autosizeSheet(dataSheet, columns.size());
            excelWorkbookSupport.autosizeSheet(guideSheet, 8);
            excelWorkbookSupport.autosizeSheet(referenceSheet, 3);

            return excelWorkbookSupport.toByteArray(workbook);
        } catch (IOException exception) {
            throw new InvalidDataException(errorMessage);
        }
    }

    private void applyDropdowns(Sheet sheet, List<ExcelTemplateColumn> columns) {
        for (int i = 0; i < columns.size(); i++) {
            List<String> values = columns.get(i).dropdownValues();
            if (values != null && !values.isEmpty()) {
                excelWorkbookSupport.applyDropdownValidation(sheet, i, values, 1, 1000);
            }
        }
    }

    private ExcelTemplateColumn buildSystemRecordIdColumn(String entityLabel) {
        return new ExcelTemplateColumn(
                SYSTEM_RECORD_ID_FIELD,
                SYSTEM_RECORD_ID_HEADER,
                false,
                true,
                "Số nguyên",
                "123",
                SYSTEM_RECORD_ID_RULE + " Dùng để nhận diện " + entityLabel + " khi import lại file export/template.",
                List.of(),
                true
        );
    }

    private Long readSystemRecordId(Row row, Map<String, Integer> headers, int rowNumber) {
        return readLongByHeader(row, headers, SYSTEM_RECORD_ID_HEADER, SYSTEM_RECORD_ID_FIELD, rowNumber, false);
    }

    private List<ExcelTemplateColumn> buildEventColumns() {
        return List.of(
                buildSystemRecordIdColumn("sự kiện"),
                new ExcelTemplateColumn("name", "Tên sự kiện", true, true, "Văn bản", "Chung tay vì em", "Không được để trống", List.of()),
                new ExcelTemplateColumn("categoryId", "Danh mục ID", true, true, "Số nguyên", "1", "Phải tồn tại trong hệ thống", List.of()),
                new ExcelTemplateColumn("categoryName", "Danh mục", false, false, "Văn bản", "Y tế", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("status", "Trạng thái", true, true, "Danh mục", "Sắp diễn ra", "Chỉ nhận giá trị hợp lệ từ danh sách", eventStatusValues()),
                new ExcelTemplateColumn("startDate", "Ngày bắt đầu", true, true, "Ngày", "20/04/2026", "Định dạng dd/MM/yyyy", List.of()),
                new ExcelTemplateColumn("endDate", "Ngày kết thúc", true, true, "Ngày", "30/04/2026", "Định dạng dd/MM/yyyy; không được trước ngày bắt đầu", List.of()),
                new ExcelTemplateColumn("currentAmount", "Số tiền hiện tại", false, true, "Số tiền", "15000000", "Số nguyên không âm", List.of()),
                new ExcelTemplateColumn("targetAmount", "Mục tiêu", false, true, "Số tiền", "50000000", "Số nguyên không âm", List.of()),
                new ExcelTemplateColumn("location", "Địa điểm", false, true, "Văn bản", "TP.HCM", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("shortDescription", "Mô tả ngắn", false, true, "Văn bản", "Gây quỹ hỗ trợ y tế", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("content", "Nội dung", false, true, "Văn bản", "Nội dung chi tiết", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("thumbnailUrl", "Ảnh đại diện", false, true, "URL", "https://example.com/cover.jpg", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("numberOfDonors", "Số nhà hảo tâm", false, false, "Số nguyên", "20", "Cột hệ thống tự sinh", List.of()),
                new ExcelTemplateColumn("createdAt", "Tạo lúc", false, false, "Ngày giờ", "13/04/2026 08:30", "Cột hệ thống tự sinh", List.of()),
                new ExcelTemplateColumn("updatedAt", "Cập nhật lúc", false, false, "Ngày giờ", "13/04/2026 09:30", "Cột hệ thống tự sinh", List.of())
        );
    }

    private List<ExcelTemplateColumn> buildActivityColumns() {
        return List.of(
                buildSystemRecordIdColumn("hoạt động"),
                new ExcelTemplateColumn("name", "Tên hoạt động", true, true, "Văn bản", "Khám sàng lọc", "Không được để trống", List.of()),
                new ExcelTemplateColumn("eventId", "Sự kiện ID", true, true, "Số nguyên", "10", "Phải tồn tại trong hệ thống", List.of()),
                new ExcelTemplateColumn("eventName", "Sự kiện", false, false, "Văn bản", "Gây quỹ mổ tim", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("status", "Trạng thái", false, true, "Danh mục", "Đang diễn ra", "Chỉ nhận giá trị hợp lệ từ danh sách", activityStatusValues()),
                new ExcelTemplateColumn("startDate", "Ngày bắt đầu", false, true, "Ngày", "20/04/2026", "Định dạng dd/MM/yyyy", List.of()),
                new ExcelTemplateColumn("endDate", "Ngày kết thúc", false, true, "Ngày", "30/04/2026", "Định dạng dd/MM/yyyy", List.of()),
                new ExcelTemplateColumn("currentAmount", "Số tiền hiện tại", false, true, "Số tiền", "5000000", "Số nguyên không âm", List.of()),
                new ExcelTemplateColumn("targetAmount", "Mục tiêu", false, true, "Số tiền", "10000000", "Số nguyên không âm", List.of()),
                new ExcelTemplateColumn("location", "Địa điểm", false, true, "Văn bản", "Đà Nẵng", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("shortDescription", "Mô tả ngắn", false, true, "Văn bản", "Khám bệnh miễn phí", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("content", "Nội dung", false, true, "Văn bản", "Nội dung chi tiết", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("thumbnailUrl", "Ảnh đại diện", false, true, "URL", "https://example.com/activity.jpg", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("numberOfDonors", "Số nhà hảo tâm", false, false, "Số nguyên", "20", "Cột hệ thống tự sinh", List.of()),
                new ExcelTemplateColumn("createdAt", "Tạo lúc", false, false, "Ngày giờ", "13/04/2026 08:30", "Cột hệ thống tự sinh", List.of()),
                new ExcelTemplateColumn("updatedAt", "Cập nhật lúc", false, false, "Ngày giờ", "13/04/2026 09:30", "Cột hệ thống tự sinh", List.of())
        );
    }

    private List<ExcelTemplateColumn> buildDonationColumns() {
        return List.of(
                buildSystemRecordIdColumn("khoản quyên góp"),
                new ExcelTemplateColumn("memoCode", "Mã đơn", false, false, "Văn bản", "MEMO-0001", "Cột hệ thống hoặc tham chiếu, không dùng để import", List.of()),
                new ExcelTemplateColumn("donorId", "Nhà hảo tâm ID", true, true, "Số nguyên", "100", "Phải tồn tại trong hệ thống", List.of()),
                new ExcelTemplateColumn("donorName", "Nhà hảo tâm", false, false, "Văn bản", "Nguyễn Văn A", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("donorPhone", "Số điện thoại", false, false, "Văn bản", "0909123456", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("donorEmail", "Email", false, false, "Email", "a@example.com", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("amount", "Số tiền", true, true, "Số tiền", "200000", "Số nguyên từ 1.000 đến 100.000.000", List.of()),
                new ExcelTemplateColumn("paymentMethod", "Phương thức", true, true, "Danh mục", "Chuyển khoản online", "Chỉ nhận giá trị hợp lệ từ danh sách", paymentMethodValues()),
                new ExcelTemplateColumn("status", "Trạng thái", false, true, "Danh mục", "Chờ duyệt", "Không bắt buộc khi tạo mới", donationStatusValues()),
                new ExcelTemplateColumn("target", "Mục tiêu", false, true, "Danh mục", "Sự kiện", "Chọn Sự kiện, Hoạt động hoặc Không gắn mục tiêu", donationTargetValues()),
                new ExcelTemplateColumn("eventId", "Sự kiện ID", false, true, "Số nguyên", "10", "Bắt buộc khi mục tiêu là Sự kiện", List.of()),
                new ExcelTemplateColumn("eventName", "Sự kiện", false, false, "Văn bản", "Gây quỹ mổ tim", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("activityId", "Hoạt động ID", false, true, "Số nguyên", "20", "Bắt buộc khi mục tiêu là Hoạt động", List.of()),
                new ExcelTemplateColumn("activityName", "Hoạt động", false, false, "Văn bản", "Khám sàng lọc", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("needReceipt", "Cần biên lai", false, true, "Danh mục", "Không", "Chỉ nhận Có hoặc Không", yesNoValues()),
                new ExcelTemplateColumn("receiptName", "Tên biên lai", false, true, "Văn bản", "Nguyễn Văn A", "Bắt buộc khi cần biên lai", List.of()),
                new ExcelTemplateColumn("receiptEmail", "Email biên lai", false, true, "Email", "receipt@example.com", "Bắt buộc khi cần biên lai", List.of()),
                new ExcelTemplateColumn("message", "Lời nhắn", false, true, "Văn bản", "Cầu chúc sức khỏe", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("donationVia", "Kênh tạo", false, false, "Văn bản", "Nội bộ", "Cột hệ thống tự sinh", List.of()),
                new ExcelTemplateColumn("donatedAt", "Ngày quyên góp", false, true, "Ngày giờ", "13/04/2026 08:30", "Định dạng dd/MM/yyyy HH:mm", List.of()),
                new ExcelTemplateColumn("createdAt", "Ngày tạo", false, false, "Ngày giờ", "13/04/2026 08:45", "Cột hệ thống tự sinh", List.of())
        );
    }

    private List<ExcelTemplateColumn> buildTransactionColumns() {
        return List.of(
                buildSystemRecordIdColumn("giao dịch"),
                new ExcelTemplateColumn("transactionCode", "Mã giao dịch", false, true, "Văn bản", "TXN-20260413-001", "Không được trùng nếu có nhập", List.of()),
                new ExcelTemplateColumn("amount", "Số tiền", true, true, "Số tiền", "250000", "Số nguyên không âm", List.of()),
                new ExcelTemplateColumn("paymentMethod", "Phương thức", true, true, "Danh mục", "Chuyển khoản online", "Chỉ nhận giá trị hợp lệ từ danh sách", paymentMethodValues()),
                new ExcelTemplateColumn("accountBankId", "Ngân hàng", false, true, "Văn bản", "VCB", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("counterAccountNumber", "Số tài khoản gửi", false, true, "Văn bản", "123456789", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("counterAccountName", "Tên tài khoản gửi", false, true, "Văn bản", "Nguyen Van A", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("description", "Mô tả", false, true, "Văn bản", "Ủng hộ chương trình", "Không bắt buộc", List.of()),
                new ExcelTemplateColumn("transactionDateTime", "Thời gian giao dịch", false, true, "Ngày giờ", "13/04/2026 09:10", "Cho phép nhập ngày giờ hoặc văn bản mô tả thời điểm", List.of()),
                new ExcelTemplateColumn("donationId", "Đơn quyên góp ID", false, true, "Số nguyên", "500", "Không bắt buộc; nếu nhập phải tồn tại", List.of()),
                new ExcelTemplateColumn("donationCode", "Mã đơn", false, false, "Văn bản", "MEMO-0001", "Cột tham chiếu từ hệ thống, không dùng để import", List.of()),
                new ExcelTemplateColumn("createdAt", "Tạo lúc", false, false, "Ngày giờ", "13/04/2026 09:15", "Cột hệ thống tự sinh", List.of())
        );
    }

    private void writeEventDataRow(Row row, EventResponse event, ExcelWorkbookSupport.WorkbookStyles styles, int rowIndex) {
        excelWorkbookSupport.writeLongCell(row, 0, event.getId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 1, event.getName(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 2, event.getCategoryId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 3, event.getCategory() != null ? event.getCategory().getName() : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 4, event.getStatus() != null ? eventStatusToLabel(event.getStatus()) : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDateCell(row, 5, event.getStartDate(), styles.dateRowStyle(rowIndex));
        excelWorkbookSupport.writeDateCell(row, 6, event.getEndDate(), styles.dateRowStyle(rowIndex));
        excelWorkbookSupport.writeDecimalCell(row, 7, event.getCurrentAmount(), styles.amountRowStyle(rowIndex));
        excelWorkbookSupport.writeDecimalCell(row, 8, event.getTargetAmount(), styles.amountRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 9, event.getLocation(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 10, event.getShortDescription(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 11, event.getContent(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 12, event.getThumbnailUrl(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 13, event.getNumberOfDonors(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 14, event.getCreatedAt(), styles.dateTimeRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 15, event.getUpdatedAt(), styles.dateTimeRowStyle(rowIndex));
    }

    private void writeActivityDataRow(Row row, ActivityResponse activity, ExcelWorkbookSupport.WorkbookStyles styles, int rowIndex) {
        excelWorkbookSupport.writeLongCell(row, 0, activity.getId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 1, activity.getName(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 2, activity.getEventId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 3, activity.getEvent() != null ? activity.getEvent().getName() : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 4, activity.getStatus() != null ? activityStatusToLabel(activity.getStatus()) : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDateCell(row, 5, activity.getStartDate(), styles.dateRowStyle(rowIndex));
        excelWorkbookSupport.writeDateCell(row, 6, activity.getEndDate(), styles.dateRowStyle(rowIndex));
        excelWorkbookSupport.writeDecimalCell(row, 7, activity.getCurrentAmount(), styles.amountRowStyle(rowIndex));
        excelWorkbookSupport.writeDecimalCell(row, 8, activity.getTargetAmount(), styles.amountRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 9, activity.getLocation(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 10, activity.getShortDescription(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 11, activity.getContent(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 12, activity.getThumbnailUrl(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 13, activity.getNumberOfDonors(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 14, activity.getCreatedAt(), styles.dateTimeRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 15, activity.getUpdatedAt(), styles.dateTimeRowStyle(rowIndex));
    }

    private void writeDonationDataRow(Row row,
                                      DonationResponse donation,
                                      Donation donationEntity,
                                      ExcelWorkbookSupport.WorkbookStyles styles,
                                      int rowIndex) {
        excelWorkbookSupport.writeLongCell(row, 0, donation.getId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 1, donation.getMemoCode(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 2, donation.getDonorId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 3, donation.getDonorName(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 4, donation.getDonorPhone(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 5, donationEntity != null && donationEntity.getDonor() != null ? donationEntity.getDonor().getEmail() : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDecimalCell(row, 6, donation.getAmount(), styles.amountRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 7, donation.getPaymentMethod() != null ? paymentMethodToLabel(donation.getPaymentMethod()) : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 8, donation.getStatus() != null ? donationStatusToLabel(donation.getStatus()) : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 9, donation.getTarget() != null ? donationTargetToLabel(donation.getTarget()) : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 10, donation.getEventId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 11, donation.getTarget() == EDonationTarget.EVENT ? donation.getObjectName() : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 12, donation.getActivityId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 13, donation.getTarget() == EDonationTarget.ACTIVITY ? donation.getObjectName() : null, styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 14, yesNoLabel(donation.getNeedReceipt()), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 15, donation.getReceiptName(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 16, donation.getReceiptEmail(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 17, donation.getMessage(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 18, donationViaToLabel(donation.getDonationVia()), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 19, donation.getDonatedAt(), styles.dateTimeRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 20, donation.getCreatedAt(), styles.dateTimeRowStyle(rowIndex));
    }

    private void writeTransactionDataRow(Row row, TransactionResponse transaction, ExcelWorkbookSupport.WorkbookStyles styles, int rowIndex) {
        excelWorkbookSupport.writeLongCell(row, 0, transaction.getId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 1, transaction.getTransactionCode(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDecimalCell(row, 2, transaction.getAmount(), styles.amountRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 3, defaultIfBlank(transaction.getPaymentMethodValue(), paymentMethodToLabel(transaction.getPaymentMethod())), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 4, transaction.getAccountBankId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 5, transaction.getCounterAccountNumber(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 6, transaction.getCounterAccountName(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 7, transaction.getDescription(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 8, transaction.getTransactionDateTime(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeLongCell(row, 9, transaction.getDonationId(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeTextCell(row, 10, transaction.getDonationCode(), styles.textRowStyle(rowIndex));
        excelWorkbookSupport.writeDateTimeCell(row, 11, transaction.getCreatedAt(), styles.dateTimeRowStyle(rowIndex));
    }

    private void validateTemplateHeaders(Map<String, Integer> headers, List<ExcelTemplateColumn> columns) {
        List<String> missingHeaders = columns.stream()
                .filter(ExcelTemplateColumn::importable)
                .map(ExcelTemplateColumn::headerLabel)
                .filter(header -> !headers.containsKey(normalizeHeader(header)))
                .toList();

        if (!missingHeaders.isEmpty()) {
            throw new InvalidDataException("Sai mẫu file import: thiếu các cột " + String.join(", ", missingHeaders));
        }
    }

    private void processEventImportRow(Row row,
                                       Map<String, Integer> headers,
                                       Map<String, Category> categoriesByName,
                                       int rowNumber) {
        Long eventId = readSystemRecordId(row, headers, rowNumber);

        EventRequest request = new EventRequest();
        request.setName(requireStringByHeader(row, headers, "Tên sự kiện", "name", rowNumber));
        request.setCategoryId(resolveCategoryIdForImport(row, headers, categoriesByName, rowNumber));
        request.setStatus(parseEventStatusForImport(requireStringByHeader(row, headers, "Trạng thái", "status", rowNumber), rowNumber));
        request.setStartDate(readDateByHeader(row, headers, "Ngày bắt đầu", "startDate", rowNumber, true));
        request.setEndDate(readDateByHeader(row, headers, "Ngày kết thúc", "endDate", rowNumber, true));
        request.setCurrentAmount(requireNonNegative(
                readDecimalByHeader(row, headers, "Số tiền hiện tại", "currentAmount", rowNumber, false, BigDecimal.ZERO),
                "Số tiền hiện tại",
                "currentAmount",
                rowNumber
        ));
        request.setTargetAmount(requireNonNegative(
                readDecimalByHeader(row, headers, "Mục tiêu", "targetAmount", rowNumber, false, BigDecimal.ZERO),
                "Mục tiêu",
                "targetAmount",
                rowNumber
        ));
        request.setLocation(readStringByHeader(row, headers, "Địa điểm", rowNumber));
        request.setShortDescription(readStringByHeader(row, headers, "Mô tả ngắn", rowNumber));
        request.setContent(readStringByHeader(row, headers, "Nội dung", rowNumber));
        request.setThumbnailUrl(readStringByHeader(row, headers, "Ảnh đại diện", rowNumber));

        validateBeanForExcel(request, Map.of(
                "name", "Tên sự kiện",
                "categoryId", "Danh mục ID",
                "status", "Trạng thái",
                "startDate", "Ngày bắt đầu",
                "endDate", "Ngày kết thúc"
        ), rowNumber);

        try {
            if (eventId == null) {
                eventService.createEvent(request);
            } else {
                eventService.updateEvent(eventId, request);
            }
        } catch (Exception exception) {
            throw mapEventImportServiceException(exception, rowNumber);
        }
    }

    private void processActivityImportRow(Row row,
                                          Map<String, Integer> headers,
                                          Map<String, Event> eventsByName,
                                          int rowNumber) {
        ActivityRequest request = new ActivityRequest();
        request.setId(readSystemRecordId(row, headers, rowNumber));
        request.setName(requireStringByHeader(row, headers, "Tên hoạt động", "name", rowNumber));
        request.setEventId(resolveEventIdForImport(row, headers, eventsByName, rowNumber));
        request.setStatus(parseActivityStatusForImport(readStringByHeader(row, headers, "Trạng thái", rowNumber), rowNumber));
        request.setStartDate(readDateByHeader(row, headers, "Ngày bắt đầu", "startDate", rowNumber, false));
        request.setEndDate(readDateByHeader(row, headers, "Ngày kết thúc", "endDate", rowNumber, false));
        request.setCurrentAmount(requireNonNegative(
                readDecimalByHeader(row, headers, "Số tiền hiện tại", "currentAmount", rowNumber, false, BigDecimal.ZERO),
                "Số tiền hiện tại",
                "currentAmount",
                rowNumber
        ));
        request.setTargetAmount(requireNonNegative(
                readDecimalByHeader(row, headers, "Mục tiêu", "targetAmount", rowNumber, false, BigDecimal.ZERO),
                "Mục tiêu",
                "targetAmount",
                rowNumber
        ));
        request.setLocation(readStringByHeader(row, headers, "Địa điểm", rowNumber));
        request.setShortDescription(readStringByHeader(row, headers, "Mô tả ngắn", rowNumber));
        request.setContent(readStringByHeader(row, headers, "Nội dung", rowNumber));
        request.setThumbnailUrl(readStringByHeader(row, headers, "Ảnh đại diện", rowNumber));

        validateBeanForExcel(request, Map.of(
                "id", SYSTEM_RECORD_ID_HEADER,
                "name", "Tên hoạt động",
                "eventId", "Sự kiện ID",
                "status", "Trạng thái"
        ), rowNumber);

        try {
            activityService.saveActivity(request);
        } catch (Exception exception) {
            throw mapActivityImportServiceException(exception, rowNumber);
        }
    }

    private void processDonationImportRow(Row row,
                                          Map<String, Integer> headers,
                                          Map<String, Event> eventsByName,
                                          Map<String, Activity> activitiesByName,
                                          String username,
                                          int rowNumber) {
        Long donationId = readSystemRecordId(row, headers, rowNumber);
        Long donorId = requireDonorIdForImport(row, headers, rowNumber);
        Long eventId = readExistingEventIdByHeader(row, headers, eventsByName, rowNumber);
        Long activityId = readExistingActivityIdByHeader(row, headers, activitiesByName, rowNumber);
        EDonationTarget target = parseDonationTargetForImport(readStringByHeader(row, headers, "Mục tiêu", rowNumber), eventId, activityId, rowNumber);

        if (target == EDonationTarget.EVENT && eventId == null) {
            throw new ExcelImportValidationException(rowNumber, "Sự kiện ID", "eventId", null,
                    "Phải nhập Sự kiện ID khi mục tiêu là Sự kiện",
                    "Chọn một sự kiện hợp lệ trong sheet DanhMucThamChieu");
        }
        if (target == EDonationTarget.ACTIVITY && activityId == null) {
            throw new ExcelImportValidationException(rowNumber, "Hoạt động ID", "activityId", null,
                    "Phải nhập Hoạt động ID khi mục tiêu là Hoạt động",
                    "Chọn một hoạt động hợp lệ trong sheet DanhMucThamChieu");
        }
        if (target == EDonationTarget.NONE) {
            eventId = null;
            activityId = null;
        } else if (target == EDonationTarget.EVENT) {
            activityId = null;
        } else if (target == EDonationTarget.ACTIVITY) {
            eventId = null;
        }

        DonationRequest request = new DonationRequest();
        request.setDonorId(donorId);
        request.setAmount(readDecimalByHeader(row, headers, "Số tiền", "amount", rowNumber, true, null));
        request.setPaymentMethod(parsePaymentMethodForImport(requireStringByHeader(row, headers, "Phương thức", "paymentMethod", rowNumber), rowNumber));
        request.setMessage(readStringByHeader(row, headers, "Lời nhắn", rowNumber));
        request.setNeedReceipt(readBooleanByHeader(row, headers, "Cần biên lai", "needReceipt", rowNumber, false, false));
        request.setReceiptName(readStringByHeader(row, headers, "Tên biên lai", rowNumber));
        request.setReceiptEmail(readStringByHeader(row, headers, "Email biên lai", rowNumber));
        request.setEventId(eventId);
        request.setActivityId(activityId);
        request.setDonatedAt(readDateTimeByHeader(row, headers, "Ngày quyên góp", "donatedAt", rowNumber, false));

        if (Boolean.TRUE.equals(request.getNeedReceipt())) {
            if (!StringUtils.hasText(request.getReceiptName())) {
                throw new ExcelImportValidationException(rowNumber, "Tên biên lai", "receiptName", null,
                        "Trường này không được để trống khi cần biên lai",
                        "Nhập tên người hoặc tổ chức xuất hiện trên biên lai");
            }
            if (!StringUtils.hasText(request.getReceiptEmail())) {
                throw new ExcelImportValidationException(rowNumber, "Email biên lai", "receiptEmail", null,
                        "Trường này không được để trống khi cần biên lai",
                        "Nhập email nhận biên lai hợp lệ");
            }
        }

        validateBeanForExcel(request, Map.of(
                "donorId", "Nhà hảo tâm ID",
                "amount", "Số tiền",
                "paymentMethod", "Phương thức",
                "eventId", "Sự kiện ID",
                "activityId", "Hoạt động ID",
                "receiptEmail", "Email biên lai"
        ), rowNumber);

        EDonationStatus importedStatus = parseDonationStatusForImport(readStringByHeader(row, headers, "Trạng thái", rowNumber), rowNumber);

        try {
            if (donationId != null) {
                Donation existingDonation = donationService.getDonation(donationId);
                donationService.updateStaffDonation(donationId, request);
                if (importedStatus != null && importedStatus != existingDonation.getStatus()) {
                    donationService.changeStatusDonation(importedStatus, donationId);
                }
            } else {
                long createdDonationId = donationService.createStaffDonation(request, username);
                if (importedStatus != null && importedStatus != EDonationStatus.PENDING_APPROVED) {
                    donationService.changeStatusDonation(importedStatus, createdDonationId);
                }
            }
        } catch (Exception exception) {
            throw mapDonationImportServiceException(exception, rowNumber);
        }
    }

    private void processTransactionImportRow(Row row,
                                             Map<String, Integer> headers,
                                             int rowNumber) {
        Long transactionId = readSystemRecordId(row, headers, rowNumber);
        Transaction transaction = transactionId != null
                ? transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ExcelImportValidationException(
                        rowNumber,
                        SYSTEM_RECORD_ID_HEADER,
                        SYSTEM_RECORD_ID_FIELD,
                        String.valueOf(transactionId),
                        "Không tìm thấy giao dịch với ID " + transactionId,
                        "Để trống cột kỹ thuật " + SYSTEM_RECORD_ID_HEADER + " nếu muốn tạo giao dịch mới"
                ))
                : new Transaction();

        transaction.setAmount(requireNonNegative(
                readDecimalByHeader(row, headers, "Số tiền", "amount", rowNumber, true, null),
                "Số tiền",
                "amount",
                rowNumber
        ));
        transaction.setPaymentMethod(parsePaymentMethodForImport(requireStringByHeader(row, headers, "Phương thức", "paymentMethod", rowNumber), rowNumber));
        transaction.setAccountBankId(readStringByHeader(row, headers, "Ngân hàng", rowNumber));
        transaction.setCounterAccountNumber(readStringByHeader(row, headers, "Số tài khoản gửi", rowNumber));
        transaction.setCounterAccountName(readStringByHeader(row, headers, "Tên tài khoản gửi", rowNumber));
        transaction.setDescription(readStringByHeader(row, headers, "Mô tả", rowNumber));
        transaction.setTransactionDateTime(readDateTimeTextByHeader(row, headers, "Thời gian giao dịch", "transactionDateTime", rowNumber));

        String transactionCode = readStringByHeader(row, headers, "Mã giao dịch", rowNumber);
        if (StringUtils.hasText(transactionCode)) {
            Transaction existingByCode = transactionRepository.findByTransactionCode(transactionCode).orElse(null);
            if (existingByCode != null && !Objects.equals(existingByCode.getId(), transaction.getId())) {
                throw new ExcelImportValidationException(rowNumber, "Mã giao dịch", "transactionCode", transactionCode,
                        "Mã giao dịch đã tồn tại",
                        "Nhập mã giao dịch khác hoặc để trống");
            }
            transaction.setTransactionCode(transactionCode);
        } else {
            transaction.setTransactionCode(null);
        }

        Donation donation = readDonationByHeader(row, headers, rowNumber);
        if (donation != null) {
            Transaction existingByDonation = transactionRepository.findByDonationId(donation.getId()).orElse(null);
            if (existingByDonation != null && !Objects.equals(existingByDonation.getId(), transaction.getId())) {
                throw new ExcelImportValidationException(rowNumber, "Đơn quyên góp ID", "donationId", String.valueOf(donation.getId()),
                        "Đơn quyên góp #" + donation.getMemoCode() + " đã có giao dịch liên kết",
                        "Chỉ liên kết mỗi đơn quyên góp với một giao dịch");
            }
            transaction.setDonation(donation);
        } else {
            transaction.setDonation(null);
        }

        try {
            transactionRepository.save(transaction);
        } catch (Exception exception) {
            throw mapTransactionImportServiceException(exception, rowNumber);
        }
    }

    private Integer resolveCategoryIdForImport(Row row,
                                               Map<String, Integer> headers,
                                               Map<String, Category> categoriesByName,
                                               int rowNumber) {
        Long categoryId = readLongByHeader(row, headers, "Danh mục ID", "categoryId", rowNumber, true);
        if (categoryId != null) {
            return categoryRepository.findById(Math.toIntExact(categoryId))
                    .orElseThrow(() -> new ExcelImportValidationException(
                            rowNumber,
                            "Danh mục ID",
                            "categoryId",
                            String.valueOf(categoryId),
                            "Không tìm thấy danh mục với ID " + categoryId,
                            "Chọn một ID hợp lệ trong sheet DanhMucThamChieu"
                    ))
                    .getId();
        }

        String categoryName = readStringByHeader(row, headers, "Danh mục", rowNumber);
        if (!StringUtils.hasText(categoryName)) {
            throw new ExcelImportValidationException(rowNumber, "Danh mục ID", "categoryId", null,
                    "Trường này không được để trống",
                    "Nhập Danh mục ID hợp lệ");
        }

        Category category = categoriesByName.get(normalizeHeader(categoryName));
        if (category == null) {
            throw new ExcelImportValidationException(rowNumber, "Danh mục", "categoryName", categoryName,
                    "Không tìm thấy danh mục " + categoryName,
                    "Kiểm tra lại danh mục trong sheet DanhMucThamChieu");
        }
        return category.getId();
    }

    private Long resolveEventIdForImport(Row row,
                                         Map<String, Integer> headers,
                                         Map<String, Event> eventsByName,
                                         int rowNumber) {
        Long eventId = readLongByHeader(row, headers, "Sự kiện ID", "eventId", rowNumber, true);
        if (eventId != null) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new ExcelImportValidationException(
                            rowNumber,
                            "Sự kiện ID",
                            "eventId",
                            String.valueOf(eventId),
                            "Không tìm thấy sự kiện với ID " + eventId,
                            "Chọn một ID hợp lệ trong sheet DanhMucThamChieu"
                    ))
                    .getId();
        }

        String eventName = readStringByHeader(row, headers, "Sự kiện", rowNumber);
        if (!StringUtils.hasText(eventName)) {
            throw new ExcelImportValidationException(rowNumber, "Sự kiện ID", "eventId", null,
                    "Trường này không được để trống",
                    "Nhập Sự kiện ID hợp lệ");
        }

        Event event = eventsByName.get(normalizeHeader(eventName));
        if (event == null) {
            throw new ExcelImportValidationException(rowNumber, "Sự kiện", "eventName", eventName,
                    "Không tìm thấy sự kiện " + eventName,
                    "Kiểm tra lại sự kiện trong sheet DanhMucThamChieu");
        }
        return event.getId();
    }

    private Long readExistingEventIdByHeader(Row row,
                                             Map<String, Integer> headers,
                                             Map<String, Event> eventsByName,
                                             int rowNumber) {
        Long eventId = readLongByHeader(row, headers, "Sự kiện ID", "eventId", rowNumber, false);
        if (eventId != null) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new ExcelImportValidationException(
                            rowNumber,
                            "Sự kiện ID",
                            "eventId",
                            String.valueOf(eventId),
                            "Không tìm thấy sự kiện với ID " + eventId,
                            "Chọn một ID hợp lệ trong sheet DanhMucThamChieu"
                    ))
                    .getId();
        }

        String eventName = readStringByHeader(row, headers, "Sự kiện", rowNumber);
        if (!StringUtils.hasText(eventName)) {
            return null;
        }

        Event event = eventsByName.get(normalizeHeader(eventName));
        if (event == null) {
            throw new ExcelImportValidationException(rowNumber, "Sự kiện", "eventName", eventName,
                    "Không tìm thấy sự kiện " + eventName,
                    "Kiểm tra lại sự kiện trong sheet DanhMucThamChieu");
        }
        return event.getId();
    }

    private Long readExistingActivityIdByHeader(Row row,
                                                Map<String, Integer> headers,
                                                Map<String, Activity> activitiesByName,
                                                int rowNumber) {
        Long activityId = readLongByHeader(row, headers, "Hoạt động ID", "activityId", rowNumber, false);
        if (activityId != null) {
            return activityRepository.findById(activityId)
                    .orElseThrow(() -> new ExcelImportValidationException(
                            rowNumber,
                            "Hoạt động ID",
                            "activityId",
                            String.valueOf(activityId),
                            "Không tìm thấy hoạt động với ID " + activityId,
                            "Chọn một ID hợp lệ trong sheet DanhMucThamChieu"
                    ))
                    .getId();
        }

        String activityName = readStringByHeader(row, headers, "Hoạt động", rowNumber);
        if (!StringUtils.hasText(activityName)) {
            return null;
        }

        Activity activity = activitiesByName.get(normalizeHeader(activityName));
        if (activity == null) {
            throw new ExcelImportValidationException(rowNumber, "Hoạt động", "activityName", activityName,
                    "Không tìm thấy hoạt động " + activityName,
                    "Kiểm tra lại hoạt động trong sheet DanhMucThamChieu");
        }
        return activity.getId();
    }

    private Long requireDonorIdForImport(Row row, Map<String, Integer> headers, int rowNumber) {
        Long donorId = readLongByHeader(row, headers, "Nhà hảo tâm ID", "donorId", rowNumber, true);
        if (donorId == null) {
            throw new ExcelImportValidationException(rowNumber, "Nhà hảo tâm ID", "donorId", null,
                    "Trường này không được để trống",
                    "Nhập ID nhà hảo tâm hợp lệ");
        }
        return donorRepository.findById(donorId)
                .orElseThrow(() -> new ExcelImportValidationException(
                        rowNumber,
                        "Nhà hảo tâm ID",
                        "donorId",
                        String.valueOf(donorId),
                        "Không tìm thấy nhà hảo tâm với ID " + donorId,
                        "Tải danh sách nhà hảo tâm mới nhất để tra đúng ID"
                ))
                .getId();
    }

    private Donation readDonationByHeader(Row row, Map<String, Integer> headers, int rowNumber) {
        Long donationId = readLongByHeader(row, headers, "Đơn quyên góp ID", "donationId", rowNumber, false);
        if (donationId == null) {
            return null;
        }
        return donationRepository.findById(donationId)
                .orElseThrow(() -> new ExcelImportValidationException(
                        rowNumber,
                        "Đơn quyên góp ID",
                        "donationId",
                        String.valueOf(donationId),
                        "Không tìm thấy đơn quyên góp với ID " + donationId,
                        "Kiểm tra lại ID đơn quyên góp trong sheet DanhMucThamChieu"
                ));
    }

    private BigDecimal readDecimalByHeader(Row row,
                                           Map<String, Integer> headers,
                                           String headerLabel,
                                           String fieldKey,
                                           int rowNumber,
                                           boolean required,
                                           BigDecimal defaultValue) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Sai mẫu file import: thiếu cột " + headerLabel,
                        "Tải lại file mẫu import mới nhất từ hệ thống");
            }
            return defaultValue;
        }

        String rawValue = getCellString(row.getCell(columnIndex));
        if (!StringUtils.hasText(rawValue)) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Trường này không được để trống",
                        "Nhập số hợp lệ cho cột " + headerLabel);
            }
            return defaultValue;
        }

        try {
            return readDecimal(row, headers, headerLabel);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, rawValue,
                    "Giá trị số không hợp lệ",
                    "Chỉ nhập số nguyên không âm, không thêm ký tự lạ");
        }
    }

    private BigDecimal requireNonNegative(BigDecimal value,
                                          String headerLabel,
                                          String fieldKey,
                                          int rowNumber) {
        if (value != null && value.signum() < 0) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, value.toPlainString(),
                    "Giá trị không được âm",
                    "Chỉ nhập số tiền lớn hơn hoặc bằng 0");
        }
        return value;
    }

    private LocalDate readDateByHeader(Row row,
                                       Map<String, Integer> headers,
                                       String headerLabel,
                                       String fieldKey,
                                       int rowNumber,
                                       boolean required) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Sai mẫu file import: thiếu cột " + headerLabel,
                        "Tải lại file mẫu import mới nhất từ hệ thống");
            }
            return null;
        }

        String rawValue = getCellString(row.getCell(columnIndex));
        if (!StringUtils.hasText(rawValue)) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Trường này không được để trống",
                        "Nhập ngày theo định dạng dd/MM/yyyy");
            }
            return null;
        }

        try {
            return readDate(row, headers, headerLabel);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, rawValue,
                    "Ngày không hợp lệ",
                    "Nhập ngày theo định dạng dd/MM/yyyy");
        }
    }

    private LocalDateTime readDateTimeByHeader(Row row,
                                               Map<String, Integer> headers,
                                               String headerLabel,
                                               String fieldKey,
                                               int rowNumber,
                                               boolean required) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Sai mẫu file import: thiếu cột " + headerLabel,
                        "Tải lại file mẫu import mới nhất từ hệ thống");
            }
            return null;
        }

        Cell cell = row.getCell(columnIndex);
        String rawValue = getCellString(cell);
        if (!StringUtils.hasText(rawValue)) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Trường này không được để trống",
                        "Nhập ngày giờ theo định dạng dd/MM/yyyy HH:mm");
            }
            return null;
        }

        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(rawValue.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, rawValue,
                "Ngày giờ không hợp lệ",
                "Nhập ngày giờ theo định dạng dd/MM/yyyy HH:mm");
    }

    private String readDateTimeTextByHeader(Row row,
                                            Map<String, Integer> headers,
                                            String headerLabel,
                                            String fieldKey,
                                            int rowNumber) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        String rawValue = getCellString(cell);
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(rawValue.trim(), formatter).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (DateTimeParseException ignored) {
            }
        }

        return rawValue.trim();
    }

    private Boolean readBooleanByHeader(Row row,
                                        Map<String, Integer> headers,
                                        String headerLabel,
                                        String fieldKey,
                                        int rowNumber,
                                        boolean required,
                                        Boolean defaultValue) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Sai mẫu file import: thiếu cột " + headerLabel,
                        "Tải lại file mẫu import mới nhất từ hệ thống");
            }
            return defaultValue;
        }

        String rawValue = getCellString(row.getCell(columnIndex));
        if (!StringUtils.hasText(rawValue)) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Trường này không được để trống",
                        "Chỉ nhập Có hoặc Không");
            }
            return defaultValue;
        }

        try {
            return readBoolean(row, headers, headerLabel);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, rawValue,
                    "Giá trị không hợp lệ",
                    "Chỉ nhập Có hoặc Không");
        }
    }

    private EEventStatus parseEventStatusForImport(String rawValue, int rowNumber) {
        try {
            return parseEventStatus(rawValue);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, "Trạng thái", "status", rawValue,
                    "Trạng thái sự kiện không hợp lệ",
                    "Chỉ nhập một trong các giá trị ở sheet DanhMucThamChieu");
        }
    }

    private EActivityStatus parseActivityStatusForImport(String rawValue, int rowNumber) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return parseActivityStatus(rawValue);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, "Trạng thái", "status", rawValue,
                    "Trạng thái hoạt động không hợp lệ",
                    "Chỉ nhập một trong các giá trị ở sheet DanhMucThamChieu");
        }
    }

    private EPaymentMethod parsePaymentMethodForImport(String rawValue, int rowNumber) {
        try {
            return parsePaymentMethod(rawValue);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, "Phương thức", "paymentMethod", rawValue,
                    "Phương thức thanh toán không hợp lệ",
                    "Chỉ nhập một trong các giá trị ở sheet DanhMucThamChieu");
        }
    }

    private EDonationStatus parseDonationStatusForImport(String rawValue, int rowNumber) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return parseDonationStatus(rawValue);
        } catch (Exception exception) {
            throw new ExcelImportValidationException(rowNumber, "Trạng thái", "status", rawValue,
                    "Trạng thái quyên góp không hợp lệ",
                    "Chỉ nhập một trong các giá trị ở sheet DanhMucThamChieu");
        }
    }

    private EDonationTarget parseDonationTargetForImport(String rawValue,
                                                         Long eventId,
                                                         Long activityId,
                                                         int rowNumber) {
        if (!StringUtils.hasText(rawValue)) {
            if (activityId != null) {
                return EDonationTarget.ACTIVITY;
            }
            if (eventId != null) {
                return EDonationTarget.EVENT;
            }
            return EDonationTarget.NONE;
        }

        String normalized = normalizeHeader(rawValue);
        return switch (normalized) {
            case "event", "sukien" -> EDonationTarget.EVENT;
            case "activity", "hoatdong" -> EDonationTarget.ACTIVITY;
            case "none", "khongganmuctieu", "khong" -> EDonationTarget.NONE;
            default -> throw new ExcelImportValidationException(rowNumber, "Mục tiêu", "target", rawValue,
                    "Mục tiêu quyên góp không hợp lệ",
                    "Chỉ nhập Sự kiện, Hoạt động hoặc Không gắn mục tiêu");
        };
    }

    private ExcelImportValidationException mapEventImportServiceException(Exception exception, int rowNumber) {
        String message = extractMessage(exception);
        if (message.contains("Danh mục")) {
            return new ExcelImportValidationException(rowNumber, "Danh mục ID", "categoryId", null, message,
                    "Kiểm tra ID danh mục trong sheet DanhMucThamChieu");
        }
        if (message.contains("thời gian bắt đầu") || message.contains("bắt đầu")) {
            return new ExcelImportValidationException(rowNumber, "Ngày bắt đầu", "startDate", null, message,
                    "Kiểm tra lại ngày bắt đầu và trạng thái sự kiện");
        }
        if (message.contains("thời gian kết thúc") || message.contains("kết thúc")) {
            return new ExcelImportValidationException(rowNumber, "Ngày kết thúc", "endDate", null, message,
                    "Kiểm tra lại ngày kết thúc và trạng thái sự kiện");
        }
        if (message.contains("Trạng thái")) {
            return new ExcelImportValidationException(rowNumber, "Trạng thái", "status", null, message,
                    "Kiểm tra trạng thái sự kiện theo đúng danh mục tham chiếu");
        }
        return new ExcelImportValidationException(rowNumber, "Dữ liệu dòng", null, null, message,
                "Kiểm tra lại toàn bộ dữ liệu của dòng này");
    }

    private ExcelImportValidationException mapActivityImportServiceException(Exception exception, int rowNumber) {
        String message = extractMessage(exception);
        if (message.contains("Event not found") || message.contains("sự kiện")) {
            return new ExcelImportValidationException(rowNumber, "Sự kiện ID", "eventId", null, message,
                    "Kiểm tra ID sự kiện trong sheet DanhMucThamChieu");
        }
        if (message.contains("Trạng thái")) {
            return new ExcelImportValidationException(rowNumber, "Trạng thái", "status", null, message,
                    "Kiểm tra lại trạng thái hoạt động");
        }
        return new ExcelImportValidationException(rowNumber, "Dữ liệu dòng", null, null, message,
                "Kiểm tra lại toàn bộ dữ liệu của dòng này");
    }

    private ExcelImportValidationException mapDonationImportServiceException(Exception exception, int rowNumber) {
        String message = extractMessage(exception);
        if (message.contains("nhà hảo tâm")) {
            return new ExcelImportValidationException(rowNumber, "Nhà hảo tâm ID", "donorId", null, message,
                    "Kiểm tra ID nhà hảo tâm trong sheet DanhMucThamChieu");
        }
        if (message.contains("Số tiền") || message.contains("đồng")) {
            return new ExcelImportValidationException(rowNumber, "Số tiền", "amount", null, message,
                    "Nhập số nguyên từ 1.000 đến 100.000.000");
        }
        if (message.contains("Email")) {
            return new ExcelImportValidationException(rowNumber, "Email biên lai", "receiptEmail", null, message,
                    "Kiểm tra email biên lai đúng định dạng");
        }
        if (message.contains("Tên")) {
            return new ExcelImportValidationException(rowNumber, "Tên biên lai", "receiptName", null, message,
                    "Nhập tên trên biên lai khi cần xuất biên lai");
        }
        if (message.contains("hoạt động")) {
            return new ExcelImportValidationException(rowNumber, "Hoạt động ID", "activityId", null, message,
                    "Kiểm tra ID hoạt động trong sheet DanhMucThamChieu");
        }
        if (message.contains("sự kiện")) {
            return new ExcelImportValidationException(rowNumber, "Sự kiện ID", "eventId", null, message,
                    "Kiểm tra ID sự kiện trong sheet DanhMucThamChieu");
        }
        if (message.contains("trạng thái")) {
            return new ExcelImportValidationException(rowNumber, "Trạng thái", "status", null, message,
                    "Kiểm tra trạng thái hiện tại của khoản quyên góp");
        }
        return new ExcelImportValidationException(rowNumber, "Dữ liệu dòng", null, null, message,
                "Kiểm tra lại toàn bộ dữ liệu của dòng này");
    }

    private ExcelImportValidationException mapTransactionImportServiceException(Exception exception, int rowNumber) {
        String message = extractMessage(exception);
        if (message.contains("Mã giao dịch")) {
            return new ExcelImportValidationException(rowNumber, "Mã giao dịch", "transactionCode", null, message,
                    "Nhập mã giao dịch khác hoặc để trống");
        }
        if (message.contains("đơn quyên góp")) {
            return new ExcelImportValidationException(rowNumber, "Đơn quyên góp ID", "donationId", null, message,
                    "Kiểm tra ID đơn quyên góp trong sheet DanhMucThamChieu");
        }
        if (message.contains("Số tiền")) {
            return new ExcelImportValidationException(rowNumber, "Số tiền", "amount", null, message,
                    "Nhập số tiền hợp lệ");
        }
        return new ExcelImportValidationException(rowNumber, "Dữ liệu dòng", null, null, message,
                "Kiểm tra lại toàn bộ dữ liệu của dòng này");
    }

    private List<String> eventStatusValues() {
        return List.of("Bản nháp", "Sắp diễn ra", "Đang diễn ra", "Hoàn thành");
    }

    private List<String> activityStatusValues() {
        return List.of("Bản nháp", "Sắp diễn ra", "Đang diễn ra", "Hoàn thành");
    }

    private List<String> paymentMethodValues() {
        return List.of("Tiền mặt", "Chuyển khoản online", "Chuyển khoản thủ công");
    }

    private List<String> donationStatusValues() {
        return List.of("Chờ duyệt", "Chờ thanh toán", "Đã xác nhận", "Đã từ chối", "Thất bại", "Đã hủy");
    }

    private List<String> donationTargetValues() {
        return List.of("Sự kiện", "Hoạt động", "Không gắn mục tiêu");
    }

    private List<String> yesNoValues() {
        return List.of("Có", "Không");
    }

    private String eventStatusToLabel(EEventStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case UPCOMING -> "Sắp diễn ra";
            case ONGOING -> "Đang diễn ra";
            case COMPLETED -> "Hoàn thành";
        };
    }

    private String activityStatusToLabel(EActivityStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case UPCOMING -> "Sắp diễn ra";
            case ONGOING -> "Đang diễn ra";
            case COMPLETED -> "Hoàn thành";
        };
    }

    private String paymentMethodToLabel(EPaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        return switch (paymentMethod) {
            case CASH -> "Tiền mặt";
            case BANK_TRANSFER_ONLINE -> "Chuyển khoản online";
            case BANK_TRANSFER_OFFLINE -> "Chuyển khoản thủ công";
        };
    }

    private String donationTargetToLabel(EDonationTarget target) {
        if (target == null) {
            return null;
        }
        return switch (target) {
            case EVENT -> "Sự kiện";
            case ACTIVITY -> "Hoạt động";
            case NONE -> "Không gắn mục tiêu";
        };
    }

    private String yesNoLabel(Boolean value) {
        if (value == null) {
            return "";
        }
        return value ? "Có" : "Không";
    }

    private ExcelImportResult importWorkbook(MultipartFile file,
                                             String moduleLabel,
                                             HeaderValidator headerValidator,
                                             RowProcessor rowProcessor) {
        try (Workbook workbook = openWorkbook(file)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new InvalidDataException("File Excel không có sheet dữ liệu");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new InvalidDataException("File Excel không có dòng tiêu đề");
            }

            Map<String, Integer> headers = buildHeaderMap(headerRow);
            if (headers.isEmpty()) {
                throw new InvalidDataException("Không đọc được cột tiêu đề từ file Excel");
            }

            headerValidator.validate(headers);

            int totalRows = 0;
            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRowNumber = rowIndex + 1;

                try {
                    rowProcessor.process(row, headers, displayRowNumber);
                    successCount++;
                } catch (Exception ex) {
                    String reason = extractMessage(ex);
                    errors.add("Dòng " + displayRowNumber + ": " + reason);
                    log.warn("Import {} failed at row {}: {}", moduleLabel, displayRowNumber, reason);
                }
            }

            if (totalRows == 0) {
                throw new InvalidDataException("File Excel không có dòng dữ liệu để nhập");
            }

            int failureCount = errors.size();
            return ExcelImportResult.builder()
                    .module(moduleLabel)
                    .totalRows(totalRows)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .success(failureCount == 0)
                    .partialSuccess(successCount > 0 && failureCount > 0)
                    .errors(errors)
                    .errorDetails(List.of())
                    .message(buildImportMessage(moduleLabel, totalRows, successCount, failureCount, errors))
                    .build();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể đọc file Excel. Vui lòng kiểm tra lại định dạng tệp");
        }
    }

    private Workbook openWorkbook(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidDataException("Vui lòng chọn file Excel để nhập");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
                throw new InvalidDataException("Chỉ hỗ trợ file Excel định dạng .xlsx hoặc .xls");
            }
        }

        return WorkbookFactory.create(file.getInputStream());
    }

    private void writeHeaderRow(Sheet sheet, Workbook workbook, List<String> headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(headers.get(columnIndex));
            cell.setCellStyle(headerStyle);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        return headerStyle;
    }

    private void finalizeWorkbook(Sheet sheet, Workbook workbook, ByteArrayOutputStream output, int columnCount) throws IOException {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 256 * 50));
        }
        workbook.write(output);
    }

    private void setTextCell(Row row, int columnIndex, String value) {
        row.createCell(columnIndex, CellType.STRING).setCellValue(defaultString(value));
    }

    private void setNumberCell(Row row, int columnIndex, Number value) {
        if (value == null) {
            setTextCell(row, columnIndex, "");
            return;
        }
        row.createCell(columnIndex, CellType.NUMERIC).setCellValue(value.doubleValue());
    }

    private void setDecimalCell(Row row, int columnIndex, BigDecimal value) {
        if (value == null) {
            setTextCell(row, columnIndex, "");
            return;
        }
        row.createCell(columnIndex, CellType.NUMERIC).setCellValue(value.doubleValue());
    }

    private void setDateCell(Row row, int columnIndex, LocalDate value) {
        setTextCell(row, columnIndex, value != null ? value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
    }

    private void setDateTimeCell(Row row, int columnIndex, LocalDateTime value) {
        setTextCell(row, columnIndex, value != null ? value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        short lastCellNum = headerRow.getLastCellNum();
        for (int columnIndex = 0; columnIndex < lastCellNum; columnIndex++) {
            Cell cell = headerRow.getCell(columnIndex);
            String rawHeader = getCellString(cell);
            if (!StringUtils.hasText(rawHeader)) {
                continue;
            }
            headers.put(normalizeHeader(rawHeader), columnIndex);
        }
        return headers;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        short firstCellNum = row.getFirstCellNum();
        short lastCellNum = row.getLastCellNum();

        if (firstCellNum < 0 || lastCellNum < 0) {
            return true;
        }

        for (int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue;
            }
            if (StringUtils.hasText(getCellString(cell))) {
                return false;
            }
        }

        return true;
    }

    private void ensureAnyHeaderPresent(Map<String, Integer> headers, String label, String... aliases) {
        for (String alias : aliases) {
            if (headers.containsKey(normalizeHeader(alias))) {
                return;
            }
        }
        throw new InvalidDataException("File Excel thiếu cột bắt buộc: " + label);
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private String readString(Row row, Map<String, Integer> headers, String... aliases) {
        Integer columnIndex = findColumnIndex(headers, aliases);
        if (columnIndex == null) {
            return null;
        }
        String value = getCellString(row.getCell(columnIndex));
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireString(Row row, Map<String, Integer> headers, String label, String... aliases) {
        String value = readString(row, headers, aliases);
        if (!StringUtils.hasText(value)) {
            throw new InvalidDataException("Thiếu " + label);
        }
        return value;
    }

    private Long readLong(Row row, Map<String, Integer> headers, String... aliases) {
        BigDecimal decimal = readDecimal(row, headers, aliases);
        if (decimal == null) {
            return null;
        }
        try {
            return decimal.longValueExact();
        } catch (ArithmeticException ex) {
            throw new InvalidDataException("Giá trị số nguyên không hợp lệ");
        }
    }

    private Integer readInteger(Row row, Map<String, Integer> headers, String... aliases) {
        Long value = readLong(row, headers, aliases);
        return value != null ? Math.toIntExact(value) : null;
    }

    private BigDecimal readDecimalOrDefault(Row row, Map<String, Integer> headers, BigDecimal defaultValue, String... aliases) {
        BigDecimal value = readDecimal(row, headers, aliases);
        return value != null ? value : defaultValue;
    }

    private BigDecimal requireDecimal(Row row, Map<String, Integer> headers, String label, String... aliases) {
        BigDecimal value = readDecimal(row, headers, aliases);
        if (value == null) {
            throw new InvalidDataException("Thiếu " + label);
        }
        return value;
    }

    private BigDecimal readDecimal(Row row, Map<String, Integer> headers, String... aliases) {
        Integer columnIndex = findColumnIndex(headers, aliases);
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String raw = getCellString(cell);
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        try {
            return normalizeDecimalString(raw);
        } catch (NumberFormatException ex) {
            throw new InvalidDataException("Giá trị số không hợp lệ: " + raw);
        }
    }

    private LocalDate readDate(Row row, Map<String, Integer> headers, String... aliases) {
        Integer columnIndex = findColumnIndex(headers, aliases);
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        String raw = getCellString(cell);
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new InvalidDataException("Ngày tháng không hợp lệ: " + raw);
    }

    private LocalDate requireDate(Row row, Map<String, Integer> headers, String label, String... aliases) {
        LocalDate value = readDate(row, headers, aliases);
        if (value == null) {
            throw new InvalidDataException("Thiếu " + label);
        }
        return value;
    }

    private Boolean readBooleanOrDefault(Row row, Map<String, Integer> headers, boolean defaultValue, String... aliases) {
        Boolean value = readBoolean(row, headers, aliases);
        return value != null ? value : defaultValue;
    }

    private Boolean readBoolean(Row row, Map<String, Integer> headers, String... aliases) {
        Integer columnIndex = findColumnIndex(headers, aliases);
        if (columnIndex == null) {
            return null;
        }

        String raw = getCellString(row.getCell(columnIndex));
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String normalized = normalizeHeader(raw);
        return switch (normalized) {
            case "true", "1", "co", "yes", "y", "x" -> true;
            case "false", "0", "khong", "no", "n" -> false;
            default -> throw new InvalidDataException("Giá trị đúng/sai không hợp lệ: " + raw);
        };
    }

    private Integer findColumnIndex(Map<String, Integer> headers, String... aliases) {
        for (String alias : aliases) {
            Integer index = headers.get(normalizeHeader(alias));
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private Integer resolveCategoryId(Row row, Map<String, Integer> headers, Map<String, Category> categoriesByName) {
        Integer categoryId = readInteger(row, headers, "danhmucid", "categoryid");
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID " + categoryId))
                    .getId();
        }

        String categoryName = readString(row, headers, "danhmuc", "category");
        if (!StringUtils.hasText(categoryName)) {
            throw new InvalidDataException("Thiếu danh mục (ID hoặc tên)");
        }

        Category category = categoriesByName.get(normalizeHeader(categoryName));
        if (category == null) {
            throw new ResourceNotFoundException("Không tìm thấy danh mục: " + categoryName);
        }
        return category.getId();
    }

    private Long resolveEventId(Row row, Map<String, Integer> headers, Map<String, Event> eventsByName) {
        Long eventId = readLong(row, headers, "sukienid", "eventid");
        if (eventId != null) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện với ID " + eventId))
                    .getId();
        }

        String eventName = readString(row, headers, "sukien", "event");
        if (!StringUtils.hasText(eventName)) {
            throw new InvalidDataException("Thiếu sự kiện (ID hoặc tên)");
        }

        Event event = eventsByName.get(normalizeHeader(eventName));
        if (event == null) {
            throw new ResourceNotFoundException("Không tìm thấy sự kiện: " + eventName);
        }
        return event.getId();
    }

    private Long resolveOptionalEventId(Row row, Map<String, Integer> headers, Map<String, Event> eventsByName) {
        Long eventId = readLong(row, headers, "sukienid", "eventid");
        if (eventId != null) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện với ID " + eventId))
                    .getId();
        }

        String eventName = readString(row, headers, "sukien", "event");
        if (!StringUtils.hasText(eventName)) {
            return null;
        }

        Event event = eventsByName.get(normalizeHeader(eventName));
        if (event == null) {
            throw new ResourceNotFoundException("Không tìm thấy sự kiện: " + eventName);
        }
        return event.getId();
    }

    private Long resolveOptionalActivityId(Row row, Map<String, Integer> headers, Map<String, Activity> activitiesByName) {
        Long activityId = readLong(row, headers, "hoatdongid", "activityid");
        if (activityId != null) {
            return activityRepository.findById(activityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hoạt động với ID " + activityId))
                    .getId();
        }

        String activityName = readString(row, headers, "hoatdong", "activity");
        if (!StringUtils.hasText(activityName)) {
            return null;
        }

        Activity activity = activitiesByName.get(normalizeHeader(activityName));
        if (activity == null) {
            throw new ResourceNotFoundException("Không tìm thấy hoạt động: " + activityName);
        }
        return activity.getId();
    }

    private Long resolveDonorId(Row row, Map<String, Integer> headers) {
        Long donorId = readLong(row, headers, "nhahotamid", "donorid");
        if (donorId != null) {
            return donorRepository.findById(donorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà hảo tâm với ID " + donorId))
                    .getId();
        }

        String phone = readString(row, headers, "sodienthoai", "phone");
        if (StringUtils.hasText(phone)) {
            return donorRepository.findByPhone(phone.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà hảo tâm với số điện thoại " + phone))
                    .getId();
        }

        String email = readString(row, headers, "email", "donoremail");
        if (StringUtils.hasText(email)) {
            return donorRepository.findByEmailIgnoreCase(email.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà hảo tâm với email " + email))
                    .getId();
        }

        throw new InvalidDataException("Thiếu nhà hảo tâm (ID, số điện thoại hoặc email)");
    }

    private Donation resolveOptionalDonation(Row row, Map<String, Integer> headers) {
        Long donationId = readLong(row, headers, "donquyengopid", "donationid");
        if (donationId != null) {
            return donationRepository.findById(donationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn quyên góp với ID " + donationId));
        }

        String donationCode = readString(row, headers, "madon", "mado", "memo", "donationcode");
        if (!StringUtils.hasText(donationCode)) {
            return null;
        }

        return donationRepository.findByMemoCode(donationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn quyên góp với mã " + donationCode));
    }

    private EEventStatus parseEventStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = normalizeHeader(raw);
        return switch (normalized) {
            case "draft", "bannhap" -> EEventStatus.DRAFT;
            case "upcoming", "sapdienra" -> EEventStatus.UPCOMING;
            case "ongoing", "dangdienra" -> EEventStatus.ONGOING;
            case "completed", "hoanthanh" -> EEventStatus.COMPLETED;
            default -> throw new InvalidDataException("Trạng thái sự kiện không hợp lệ: " + raw);
        };
    }

    private EActivityStatus parseActivityStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = normalizeHeader(raw);
        return switch (normalized) {
            case "draft", "bannhap" -> EActivityStatus.DRAFT;
            case "upcoming", "sapdienra" -> EActivityStatus.UPCOMING;
            case "ongoing", "dangdienra" -> EActivityStatus.ONGOING;
            case "completed", "hoanthanh" -> EActivityStatus.COMPLETED;
            default -> throw new InvalidDataException("Trạng thái hoạt động không hợp lệ: " + raw);
        };
    }

    private EDonorType parseDonorType(String raw) {
        String normalized = normalizeHeader(raw);
        return switch (normalized) {
            case "individual", "canhan" -> EDonorType.INDIVIDUAL;
            case "organization", "tochuc" -> EDonorType.ORGANIZATION;
            default -> throw new InvalidDataException("Loại nhà hảo tâm không hợp lệ: " + raw);
        };
    }

    private EPaymentMethod parsePaymentMethod(String raw) {
        String normalized = normalizeHeader(raw);
        return switch (normalized) {
            case "cash", "tienmat" -> EPaymentMethod.CASH;
            case "banktransferonline", "chuyenkhoanonline", "ckonline" -> EPaymentMethod.BANK_TRANSFER_ONLINE;
            case "banktransferoffline", "chuyenkhoanoffline", "chuyenkhoanthucong", "chuyenkhoan" -> EPaymentMethod.BANK_TRANSFER_OFFLINE;
            default -> throw new InvalidDataException("Phương thức thanh toán không hợp lệ: " + raw);
        };
    }

    private EDonationStatus parseDonationStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = normalizeHeader(raw);
        return switch (normalized) {
            case "pendingapproved", "choduyet" -> EDonationStatus.PENDING_APPROVED;
            case "pendingpayment", "chothanhtoan" -> EDonationStatus.PENDING_PAYMENT;
            case "confirmed", "daxacnhan" -> EDonationStatus.CONFIRMED;
            case "rejected", "datuchoi" -> EDonationStatus.REJECTED;
            case "failed", "thatbai" -> EDonationStatus.FAILED;
            case "cancelled", "dahuy" -> EDonationStatus.CANCELLED;
            default -> throw new InvalidDataException("Trạng thái quyên góp không hợp lệ: " + raw);
        };
    }

    private String donorTypeToLabel(EDonorType type) {
        if (type == null) return null;
        return type == EDonorType.ORGANIZATION ? "Tổ chức" : "Cá nhân";
    }

    private String donationStatusToLabel(EDonationStatus status) {
        if (status == null) return null;
        return switch (status) {
            case PENDING_APPROVED -> "Chờ duyệt";
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case REJECTED -> "Đã từ chối";
            case FAILED -> "Thất bại";
        };
    }

    private String donationViaToLabel(EDonationVia donationVia) {
        if (donationVia == null) return null;
        return donationVia == EDonationVia.STAFF ? "Nội bộ" : "Website";
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toLowerCase(Locale.ROOT);

        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private BigDecimal normalizeDecimalString(String raw) {
        String sanitized = raw.trim()
                .replace("\u00A0", "")
                .replace("₫", "")
                .replace("đ", "")
                .replace("vnd", "")
                .replace("VND", "")
                .replace(" ", "");

        int lastComma = sanitized.lastIndexOf(',');
        int lastDot = sanitized.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                sanitized = sanitized.replace(".", "");
                sanitized = sanitized.replace(",", ".");
            } else {
                sanitized = sanitized.replace(",", "");
            }
        } else if (lastComma >= 0) {
            if (sanitized.chars().filter(ch -> ch == ',').count() > 1 || hasThreeDigitsAfterSeparator(sanitized, ',')) {
                sanitized = sanitized.replace(",", "");
            } else {
                sanitized = sanitized.replace(",", ".");
            }
        } else if (lastDot >= 0 && (sanitized.chars().filter(ch -> ch == '.').count() > 1 || hasThreeDigitsAfterSeparator(sanitized, '.'))) {
            sanitized = sanitized.replace(".", "");
        }

        return new BigDecimal(sanitized);
    }

    private boolean hasThreeDigitsAfterSeparator(String raw, char separator) {
        int index = raw.lastIndexOf(separator);
        if (index < 0) {
            return false;
        }
        return raw.length() - index - 1 == 3;
    }

    private void validateBean(Object bean) {
        Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.joining("; "));
        throw new InvalidDataException(message);
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        String lastMessage = null;

        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                lastMessage = current.getMessage();
            }
            current = current.getCause();
        }

        return StringUtils.hasText(lastMessage) ? lastMessage : "Lỗi không xác định";
    }

    private String buildImportMessage(String moduleLabel,
                                      int totalRows,
                                      int successCount,
                                      int failureCount,
                                      List<String> errors) {
        String prefix;
        if (failureCount == 0) {
            prefix = String.format("Nhập Excel %s thành công: %d/%d dòng hợp lệ.", moduleLabel, successCount, totalRows);
        } else if (successCount == 0) {
            prefix = String.format("Nhập Excel %s thất bại: 0/%d dòng hợp lệ.", moduleLabel, totalRows);
        } else {
            prefix = String.format("Nhập Excel %s hoàn tất: %d/%d dòng thành công, %d dòng lỗi.", moduleLabel, successCount, totalRows, failureCount);
        }

        if (errors.isEmpty()) {
            return prefix;
        }

        List<String> summarizedErrors = errors.stream()
                .limit(MAX_ERROR_LINES_IN_MESSAGE)
                .toList();

        StringBuilder builder = new StringBuilder(prefix)
                .append("\nLý do lỗi:");
        summarizedErrors.forEach(error -> builder.append("\n- ").append(error));

        if (errors.size() > MAX_ERROR_LINES_IN_MESSAGE) {
            builder.append("\n- ... và ").append(errors.size() - MAX_ERROR_LINES_IN_MESSAGE).append(" lỗi khác");
        }

        return builder.toString();
    }

    private String defaultString(String value) {
        return value != null ? value : "";
    }

    private byte[] buildDonorWorkbook(List<DonorResponse> donors, boolean templateOnly) {
        List<DonorExcelColumn> columns = buildDonorColumns();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DonorWorkbookStyles styles = createDonorWorkbookStyles(workbook);

            Sheet dataSheet = workbook.createSheet(DONOR_DATA_SHEET_NAME);
            writeDonorHeaderRow(dataSheet, columns, styles);

            int rowIndex = 1;
            if (!templateOnly) {
                for (DonorResponse donor : donors) {
                    writeDonorDataRow(dataSheet.createRow(rowIndex), donor, styles, rowIndex);
                    rowIndex++;
                }
            }

            dataSheet.createFreezePane(0, 1);
            dataSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columns.size() - 1));

            Sheet guideSheet = workbook.createSheet(DONOR_GUIDE_SHEET_NAME);
            writeDonorGuideSheet(guideSheet, columns, styles);

            Sheet referenceSheet = workbook.createSheet(DONOR_REFERENCE_SHEET_NAME);
            writeDonorReferenceSheet(referenceSheet, styles);
            applyDonorTypeValidation(dataSheet);

            autosizeSheet(dataSheet, columns.size());
            autosizeSheet(guideSheet, 8);
            autosizeSheet(referenceSheet, 3);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException(templateOnly
                    ? "Không thể tạo file mẫu import nhà hảo tâm"
                    : "Không thể xuất file Excel nhà hảo tâm");
        }
    }

    private void validateDonorTemplateHeaders(Map<String, Integer> headers) {
        List<String> missingHeaders = buildDonorColumns().stream()
                .filter(DonorExcelColumn::importable)
                .map(DonorExcelColumn::headerLabel)
                .filter(header -> !headers.containsKey(normalizeHeader(header)))
                .toList();

        if (!missingHeaders.isEmpty()) {
            throw new InvalidDataException("Sai mẫu file import: thiếu các cột bắt buộc " + String.join(", ", missingHeaders));
        }
    }

    private void processDonorImportRow(Row row, Map<String, Integer> headers, int rowNumber) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.executeWithoutResult(status -> {
            Long donorId = readSystemRecordId(row, headers, rowNumber);
            String rawDonorType = requireStringByHeader(row, headers, "Loại", "type", rowNumber);
            EDonorType donorType = parseDonorTypeForImport(rawDonorType, rowNumber, "Loại", "type");

            if (donorType == EDonorType.INDIVIDUAL) {
                IndividualDonorRequest request = new IndividualDonorRequest();
                String fullName = requireStringByHeader(row, headers, "Họ tên cá nhân", "fullName", rowNumber);
                String displayName = readStringByHeader(row, headers, "Tên hiển thị", rowNumber);
                request.setFullName(fullName);
                request.setDisplayName(defaultIfBlank(displayName, fullName));
                request.setPhone(requireStringByHeader(row, headers, "Số điện thoại", "phone", rowNumber));
                request.setEmail(readStringByHeader(row, headers, "Email", rowNumber));
                request.setReferralSource(readStringByHeader(row, headers, "Nguồn biết đến", rowNumber));
                request.setNote(readStringByHeader(row, headers, "Ghi chú", rowNumber));

                validateBeanForExcel(request, Map.of(
                        "fullName", "Họ tên cá nhân",
                        "displayName", "Tên hiển thị",
                        "phone", "Số điện thoại",
                        "email", "Email"
                ), rowNumber);

                try {
                    if (donorId != null) {
                        donorService.updateIndividualDonor(donorId, request);
                    } else {
                        donorService.saveIndividualDonor(request);
                    }
                } catch (Exception ex) {
                    throw mapDonorImportServiceException(ex, rowNumber);
                }
                return;
            }

            OrganizeDonorRequest request = new OrganizeDonorRequest();
            request.setName(requireStringByHeader(row, headers, "Tên tổ chức", "name", rowNumber));
            request.setTaxCode(requireStringByHeader(row, headers, "Mã số thuế", "taxCode", rowNumber));
            request.setRepresentative(requireStringByHeader(row, headers, "Người đại diện", "representative", rowNumber));
            request.setPhone(requireStringByHeader(row, headers, "Số điện thoại", "phone", rowNumber));
            request.setEmail(requireStringByHeader(row, headers, "Email", "email", rowNumber));
            request.setBillingAddress(readStringByHeader(row, headers, "Địa chỉ xuất hóa đơn", rowNumber));
            request.setReferralSource(readStringByHeader(row, headers, "Nguồn biết đến", rowNumber));
            request.setNote(readStringByHeader(row, headers, "Ghi chú", rowNumber));

            validateBeanForExcel(request, Map.of(
                    "name", "Tên tổ chức",
                    "taxCode", "Mã số thuế",
                    "representative", "Người đại diện",
                    "phone", "Số điện thoại",
                    "email", "Email"
            ), rowNumber);

            try {
                if (donorId != null) {
                    donorService.updateOrganizeDonor(donorId, request);
                } else {
                    donorService.saveOrganizeDonor(request);
                }
            } catch (Exception ex) {
                throw mapDonorImportServiceException(ex, rowNumber);
            }
        });
    }

    private ExcelImportValidationException mapDonorImportServiceException(Exception exception, int rowNumber) {
        String message = extractMessage(exception);
        if (message.contains("Số điện thoại")) {
            return new ExcelImportValidationException(rowNumber, "Số điện thoại", "phone", null, message,
                    "Kiểm tra số điện thoại đúng định dạng và chưa bị trùng");
        }
        if (message.contains("Email")) {
            return new ExcelImportValidationException(rowNumber, "Email", "email", null, message,
                    "Kiểm tra email đúng định dạng và chưa tồn tại trong hệ thống");
        }
        if (message.contains("Mã số thuế")) {
            return new ExcelImportValidationException(rowNumber, "Mã số thuế", "taxCode", null, message,
                    "Kiểm tra mã số thuế theo hồ sơ tổ chức");
        }
        return new ExcelImportValidationException(rowNumber, "Dữ liệu dòng", null, null, message,
                "Kiểm tra lại toàn bộ dữ liệu ở dòng này");
    }

    private void validateBeanForExcel(Object bean, Map<String, String> fieldLabels, int rowNumber) {
        Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        if (violations.isEmpty()) {
            return;
        }

        ConstraintViolation<Object> violation = violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .findFirst()
                .orElseThrow();

        String fieldKey = violation.getPropertyPath().toString();
        String columnName = fieldLabels.getOrDefault(fieldKey, fieldKey);
        throw new ExcelImportValidationException(rowNumber, columnName, fieldKey, null,
                violation.getMessage(), buildExcelSuggestion(fieldKey));
    }

    private String buildExcelSuggestion(String fieldKey) {
        return switch (fieldKey) {
            case "phone" -> "Nhập số điện thoại gồm 10 hoặc 11 chữ số, không chứa ký tự lạ";
            case "email" -> "Nhập email đúng định dạng, ví dụ example@domain.com";
            case "displayName" -> "Nhập tên hiển thị dùng trên website";
            case "name" -> "Nhập đúng tên tổ chức theo hồ sơ pháp lý";
            case "taxCode" -> "Kiểm tra mã số thuế trước khi import";
            default -> "Kiểm tra dữ liệu theo đúng cột và định dạng của file mẫu import";
        };
    }

    private ExcelImportErrorDetail toImportErrorDetail(ExcelImportValidationException exception) {
        return ExcelImportErrorDetail.builder()
                .rowNumber(exception.getRowNumber())
                .columnName(exception.getColumnName())
                .fieldKey(exception.getFieldKey())
                .invalidValue(exception.getInvalidValue())
                .message(exception.getMessage())
                .suggestion(exception.getSuggestion())
                .build();
    }

    private String formatImportErrorDetail(ExcelImportErrorDetail detail) {
        String columnPart = StringUtils.hasText(detail.getColumnName()) ? " - cột " + detail.getColumnName() : "";
        return "Dòng " + detail.getRowNumber() + columnPart + ": " + detail.getMessage();
    }

    private byte[] buildDonorImportErrorReport(List<ExcelImportErrorDetail> errorDetails) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DonorWorkbookStyles styles = createDonorWorkbookStyles(workbook);
            Sheet sheet = workbook.createSheet("BaoCaoLoiImport");

            Row headerRow = sheet.createRow(0);
            List<String> headers = List.of("Dòng", "Cột", "Mã trường", "Giá trị nhập", "Lý do lỗi", "Gợi ý sửa");
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(styles.errorHeaderStyle());
            }

            int rowIndex = 1;
            for (ExcelImportErrorDetail errorDetail : errorDetails) {
                Row row = sheet.createRow(rowIndex++);
                writeStyledCell(row, 0, errorDetail.getRowNumber() != null ? String.valueOf(errorDetail.getRowNumber()) : "", styles.referenceCellStyle());
                writeStyledCell(row, 1, defaultString(errorDetail.getColumnName()), styles.referenceCellStyle());
                writeStyledCell(row, 2, defaultString(errorDetail.getFieldKey()), styles.referenceCellStyle());
                writeStyledCell(row, 3, defaultString(errorDetail.getInvalidValue()), styles.invalidValueCellStyle());
                writeStyledCell(row, 4, defaultString(errorDetail.getMessage()), styles.referenceCellStyle());
                writeStyledCell(row, 5, defaultString(errorDetail.getSuggestion()), styles.referenceCellStyle());
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.size() - 1));
            autosizeSheet(sheet, headers.size());
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể tạo file báo cáo lỗi import nhà hảo tâm");
        }
    }

    private List<DonorExcelColumn> buildDonorColumns() {
        return List.of(
                new DonorExcelColumn(SYSTEM_RECORD_ID_FIELD, SYSTEM_RECORD_ID_HEADER, false, true, "Số nguyên", "123", SYSTEM_RECORD_ID_RULE + " Dùng để nhận diện nhà hảo tâm cũ khi import lại file export/template.", List.of(), true),
                new DonorExcelColumn("type", "Loại", true, true, "Danh mục", "Cá nhân / Tổ chức", "Chỉ nhận giá trị Cá nhân hoặc Tổ chức", List.of("Cá nhân", "Tổ chức")),
                new DonorExcelColumn("fullName", "Họ tên cá nhân", false, true, "Văn bản", "Nguyễn Văn A", "Bắt buộc khi loại là Cá nhân", List.of()),
                new DonorExcelColumn("displayName", "Tên hiển thị", false, true, "Văn bản", "Anh A", "Bắt buộc khi loại là Cá nhân", List.of()),
                new DonorExcelColumn("organizationName", "Tên tổ chức", false, true, "Văn bản", "Công ty TNHH ABC", "Bắt buộc khi loại là Tổ chức", List.of()),
                new DonorExcelColumn("taxCode", "Mã số thuế", false, true, "Văn bản", "0312345678", "Bắt buộc khi loại là Tổ chức", List.of()),
                new DonorExcelColumn("representative", "Người đại diện", false, true, "Văn bản", "Trần Thị B", "Bắt buộc khi loại là Tổ chức", List.of()),
                new DonorExcelColumn("billingAddress", "Địa chỉ xuất hóa đơn", false, true, "Văn bản", "12 Nguyễn Huệ, Quận 1", "Không bắt buộc", List.of()),
                new DonorExcelColumn("phone", "Số điện thoại", true, true, "Văn bản", "0909123456", "10 hoặc 11 chữ số; không chứa ký tự lạ", List.of()),
                new DonorExcelColumn("email", "Email", false, true, "Email", "abc@example.com", "Không bắt buộc với cá nhân; bắt buộc với tổ chức", List.of()),
                new DonorExcelColumn("referralSource", "Nguồn biết đến", false, true, "Văn bản", "Facebook", "Không bắt buộc", List.of()),
                new DonorExcelColumn("note", "Ghi chú", false, true, "Văn bản", "Nhà hảo tâm lâu năm", "Không bắt buộc", List.of()),
                new DonorExcelColumn("numberOfDonations", "Số lần đóng góp", false, false, "Số nguyên", "5", "Cột hệ thống tự sinh, chỉ để tham khảo", List.of()),
                new DonorExcelColumn("totalDonationAmount", "Tổng tiền", false, false, "Tiền tệ", "5.000.000 ₫", "Cột hệ thống tự sinh, chỉ để tham khảo", List.of()),
                new DonorExcelColumn("createdAt", "Ngày tham gia", false, false, "Ngày giờ", "12/04/2026 10:30", "Cột hệ thống tự sinh, chỉ để tham khảo", List.of())
        );
    }

    private void writeDonorHeaderRow(Sheet sheet, List<DonorExcelColumn> columns, DonorWorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(26);
        CreationHelper creationHelper = sheet.getWorkbook().getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            DonorExcelColumn column = columns.get(columnIndex);
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(column.required() ? styles.requiredHeaderStyle() : styles.optionalHeaderStyle());

            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(columnIndex);
            anchor.setCol2(columnIndex + 3);
            anchor.setRow1(0);
            anchor.setRow2(4);
            Comment comment = drawing.createCellComment(anchor);
            String commentText = "Mã trường: " + column.fieldKey()
                    + "\nHiển thị: " + (column.hidden() ? "Ẩn - cột kỹ thuật" : "Hiện")
                    + "\nBắt buộc: " + (column.required() ? "Có" : "Không")
                    + "\nĐược import: " + (column.importable() ? "Có" : "Không")
                    + "\nKiểu dữ liệu: " + column.dataType()
                    + "\nVí dụ: " + column.example()
                    + "\nQuy tắc: " + column.rules();
            comment.setString(creationHelper.createRichTextString(commentText));
            cell.setCellComment(comment);
            sheet.setColumnHidden(columnIndex, column.hidden());
        }
    }

    private void writeDonorDataRow(Row row, DonorResponse donor, DonorWorkbookStyles styles, int rowIndex) {
        boolean isOrg = donor.getType() == EDonorType.ORGANIZATION;
        OrganizationResponse organization = donor.getOrganization();
        CellStyle rowStyle = rowIndex % 2 == 0 ? styles.evenRowStyle() : styles.oddRowStyle();

        writeStyledCell(row, 0, donor.getId() != null ? String.valueOf(donor.getId()) : "", rowStyle);
        writeStyledCell(row, 1, donorTypeToLabel(donor.getType()), rowStyle);
        writeStyledCell(row, 2, isOrg ? "" : donor.getFullName(), rowStyle);
        writeStyledCell(row, 3, isOrg ? "" : donor.getDisplayName(), rowStyle);
        writeStyledCell(row, 4, isOrg && organization != null ? organization.getName() : "", rowStyle);
        writeStyledCell(row, 5, organization != null ? organization.getTaxCode() : "", rowStyle);
        writeStyledCell(row, 6, organization != null ? organization.getRepresentative() : "", rowStyle);
        writeStyledCell(row, 7, organization != null ? organization.getBillingAddress() : "", rowStyle);
        writeStyledCell(row, 8, donor.getPhone(), rowStyle);
        writeStyledCell(row, 9, donor.getEmail(), rowStyle);
        writeStyledCell(row, 10, donor.getReferralSource(), rowStyle);
        writeStyledCell(row, 11, donor.getNote(), rowStyle);
        writeStyledCell(row, 12, donor.getNumberOfDonations() != null ? String.valueOf(donor.getNumberOfDonations()) : "", rowStyle);
        writeStyledCell(row, 13, formatCurrencyForExcel(donor.getTotalDonationAmount()), rowStyle);
        writeStyledCell(row, 14, formatDateTimeForExcel(donor.getCreatedAt()), rowStyle);
    }

    private void writeDonorGuideSheet(Sheet sheet, List<DonorExcelColumn> columns, DonorWorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        List<String> headers = List.of("Mã trường", "Cột Excel", "Hiển thị", "Bắt buộc", "Được import", "Kiểu dữ liệu", "Ví dụ hợp lệ", "Quy tắc nhập liệu");
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(styles.guideHeaderStyle());
        }

        int rowIndex = 1;
        for (DonorExcelColumn column : columns) {
            Row row = sheet.createRow(rowIndex++);
            writeStyledCell(row, 0, column.fieldKey(), styles.referenceCellStyle());
            writeStyledCell(row, 1, column.headerLabel(), styles.referenceCellStyle());
            writeStyledCell(row, 2, column.hidden() ? "Ẩn" : "Hiện", styles.referenceCellStyle());
            writeStyledCell(row, 3, column.required() ? "Có" : "Không", styles.referenceCellStyle());
            writeStyledCell(row, 4, column.importable() ? "Có" : "Không", styles.referenceCellStyle());
            writeStyledCell(row, 5, column.dataType(), styles.referenceCellStyle());
            writeStyledCell(row, 6, column.example(), styles.referenceCellStyle());
            writeStyledCell(row, 7, column.rules(), styles.referenceCellStyle());
        }

        sheet.createFreezePane(0, 1);
    }

    private void writeDonorReferenceSheet(Sheet sheet, DonorWorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        List<String> headers = List.of("Nhóm tham chiếu", "Giá trị hợp lệ", "Ghi chú");
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(styles.guideHeaderStyle());
        }

        Row row1 = sheet.createRow(1);
        writeStyledCell(row1, 0, "Loại nhà hảo tâm", styles.referenceCellStyle());
        writeStyledCell(row1, 1, "Cá nhân", styles.referenceCellStyle());
        writeStyledCell(row1, 2, "Dùng cho nhà hảo tâm cá nhân", styles.referenceCellStyle());

        Row row2 = sheet.createRow(2);
        writeStyledCell(row2, 0, "Loại nhà hảo tâm", styles.referenceCellStyle());
        writeStyledCell(row2, 1, "Tổ chức", styles.referenceCellStyle());
        writeStyledCell(row2, 2, "Dùng cho nhà hảo tâm tổ chức", styles.referenceCellStyle());

        sheet.createFreezePane(0, 1);
    }

    private void applyDonorTypeValidation(Sheet dataSheet) {
        DataValidationHelper helper = dataSheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint("'" + DONOR_REFERENCE_SHEET_NAME + "'!$B$2:$B$3");
        CellRangeAddressList addressList = new CellRangeAddressList(1, 500, 1, 1);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        dataSheet.addValidationData(validation);
    }

    private DonorWorkbookStyles createDonorWorkbookStyles(Workbook workbook) {
        Font whiteBoldFont = workbook.createFont();
        whiteBoldFont.setBold(true);
        whiteBoldFont.setColor(IndexedColors.WHITE.getIndex());

        Font defaultFont = workbook.createFont();
        defaultFont.setFontName("Arial");

        CellStyle requiredHeaderStyle = workbook.createCellStyle();
        requiredHeaderStyle.setFont(whiteBoldFont);
        requiredHeaderStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        requiredHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        requiredHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        requiredHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        requiredHeaderStyle.setWrapText(true);

        CellStyle optionalHeaderStyle = workbook.createCellStyle();
        optionalHeaderStyle.cloneStyleFrom(requiredHeaderStyle);
        optionalHeaderStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());

        CellStyle oddRowStyle = workbook.createCellStyle();
        oddRowStyle.setFont(defaultFont);
        oddRowStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        oddRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        oddRowStyle.setWrapText(true);

        CellStyle evenRowStyle = workbook.createCellStyle();
        evenRowStyle.cloneStyleFrom(oddRowStyle);
        evenRowStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());

        CellStyle guideHeaderStyle = workbook.createCellStyle();
        guideHeaderStyle.cloneStyleFrom(requiredHeaderStyle);
        guideHeaderStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());

        CellStyle referenceCellStyle = workbook.createCellStyle();
        referenceCellStyle.setFont(defaultFont);
        referenceCellStyle.setWrapText(true);
        referenceCellStyle.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle errorHeaderStyle = workbook.createCellStyle();
        errorHeaderStyle.setFont(whiteBoldFont);
        errorHeaderStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        errorHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        errorHeaderStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle invalidValueCellStyle = workbook.createCellStyle();
        invalidValueCellStyle.cloneStyleFrom(referenceCellStyle);
        invalidValueCellStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        invalidValueCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return new DonorWorkbookStyles(requiredHeaderStyle, optionalHeaderStyle, oddRowStyle, evenRowStyle,
                guideHeaderStyle, referenceCellStyle, errorHeaderStyle, invalidValueCellStyle);
    }

    private void writeStyledCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex, CellType.STRING);
        cell.setCellValue(defaultString(value));
        cell.setCellStyle(style);
    }

    private void autosizeSheet(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 256 * 48));
        }
    }

    private String formatCurrencyForExcel(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + " ₫";
    }

    private String formatDateTimeForExcel(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String readStringByHeader(Row row, Map<String, Integer> headers, String headerLabel, int rowNumber) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, normalizeHeader(headerLabel), null,
                    "Sai mẫu file import: thiếu cột " + headerLabel,
                    "Tải lại file mẫu import mới nhất từ hệ thống");
        }

        String value = getCellString(row.getCell(columnIndex));
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireStringByHeader(Row row, Map<String, Integer> headers, String headerLabel, String fieldKey, int rowNumber) {
        String value = readStringByHeader(row, headers, headerLabel, rowNumber);
        if (!StringUtils.hasText(value)) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, value,
                    "Trường này không được để trống",
                    "Nhập giá trị cho cột " + headerLabel + " theo đúng mẫu import");
        }
        return value;
    }

    private Long readLongByHeader(Row row, Map<String, Integer> headers, String headerLabel, String fieldKey, int rowNumber, boolean required) {
        Integer columnIndex = headers.get(normalizeHeader(headerLabel));
        if (columnIndex == null) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Sai mẫu file import: thiếu cột " + headerLabel,
                        "Tải lại file mẫu import mới nhất từ hệ thống");
            }
            return null;
        }

        String rawValue = getCellString(row.getCell(columnIndex));
        if (!StringUtils.hasText(rawValue)) {
            if (required) {
                throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, null,
                        "Trường này không được để trống",
                        "Nhập số nguyên hợp lệ cho cột " + headerLabel);
            }
            return null;
        }

        try {
            return readLong(row, headers, headerLabel);
        } catch (Exception ex) {
            String suggestion = SYSTEM_RECORD_ID_FIELD.equals(fieldKey)
                    ? "Chỉ nhập số nguyên dương. Để trống cột kỹ thuật này nếu muốn tạo bản ghi mới."
                    : "Chỉ nhập số nguyên hợp lệ cho cột " + headerLabel;
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, rawValue,
                    "Giá trị số nguyên không hợp lệ",
                    suggestion);
        }
    }

    private EDonorType parseDonorTypeForImport(String rawValue, int rowNumber, String headerLabel, String fieldKey) {
        try {
            return parseDonorType(rawValue);
        } catch (Exception ex) {
            throw new ExcelImportValidationException(rowNumber, headerLabel, fieldKey, rawValue,
                    "Loại nhà hảo tâm không hợp lệ",
                    "Chỉ nhập một trong hai giá trị: Cá nhân hoặc Tổ chức");
        }
    }

    private record DonorExcelColumn(String fieldKey,
                                    String headerLabel,
                                    boolean required,
                                    boolean importable,
                                    String dataType,
                                    String example,
                                    String rules,
                                    List<String> referenceValues,
                                    boolean hidden) {
        private DonorExcelColumn(String fieldKey,
                                 String headerLabel,
                                 boolean required,
                                 boolean importable,
                                 String dataType,
                                 String example,
                                 String rules,
                                 List<String> referenceValues) {
            this(fieldKey, headerLabel, required, importable, dataType, example, rules, referenceValues, false);
        }
    }

    private record DonorWorkbookStyles(CellStyle requiredHeaderStyle,
                                       CellStyle optionalHeaderStyle,
                                       CellStyle oddRowStyle,
                                       CellStyle evenRowStyle,
                                       CellStyle guideHeaderStyle,
                                       CellStyle referenceCellStyle,
                                       CellStyle errorHeaderStyle,
                                       CellStyle invalidValueCellStyle) {
    }

    @FunctionalInterface
    private interface HeaderValidator {
        void validate(Map<String, Integer> headers);
    }

    @FunctionalInterface
    private interface RowProcessor {
        void process(Row row, Map<String, Integer> headers, int rowNumber) throws Exception;
    }

    @FunctionalInterface
    private interface WorkbookRowWriter<T> {
        void write(Row row, T item, ExcelWorkbookSupport.WorkbookStyles styles, int rowIndex);
    }
}
