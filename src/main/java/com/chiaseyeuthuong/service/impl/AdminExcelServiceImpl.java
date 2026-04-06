package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.*;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
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
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d.M.yyyy")
    );
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.forLanguageTag("vi-VN"));

    private final EventService eventService;
    private final ActivityService activityService;
    private final DonorService donorService;
    private final DonationService donationService;
    private final TransactionService transactionService;

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;

    private final Validator validator;

    @Override
    public byte[] exportEvents(String search, EEventStatus status, String sortBy, String sortDir, String... categoryIds) {
        List<EventResponse> events = eventService
                .getAllEvents(1, MAX_EXPORT_SIZE, sortBy, sortDir, search, status, false, categoryIds)
                .getData();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("SuKien");
            List<String> headers = List.of(
                    "ID", "Tên sự kiện", "Danh mục ID", "Danh mục", "Trạng thái", "Ngày bắt đầu", "Ngày kết thúc",
                    "Số tiền hiện tại", "Mục tiêu", "Địa điểm", "Mô tả ngắn", "Nội dung",
                    "Ảnh đại diện", "Số nhà hảo tâm", "Tạo lúc", "Cập nhật lúc"
            );
            writeHeaderRow(sheet, workbook, headers);

            int rowIndex = 1;
            for (EventResponse event : events) {
                Row row = sheet.createRow(rowIndex++);
                setNumberCell(row, 0, event.getId());
                setTextCell(row, 1, event.getName());
                setNumberCell(row, 2, event.getCategoryId());
                setTextCell(row, 3, event.getCategory() != null ? event.getCategory().getName() : null);
                setTextCell(row, 4, event.getStatus() != null ? event.getStatus().getValue() : null);
                setDateCell(row, 5, event.getStartDate());
                setDateCell(row, 6, event.getEndDate());
                setDecimalCell(row, 7, event.getCurrentAmount());
                setDecimalCell(row, 8, event.getTargetAmount());
                setTextCell(row, 9, event.getLocation());
                setTextCell(row, 10, event.getShortDescription());
                setTextCell(row, 11, event.getContent());
                setTextCell(row, 12, event.getThumbnailUrl());
                setNumberCell(row, 13, event.getNumberOfDonors());
                setDateTimeCell(row, 14, event.getCreatedAt());
                setDateTimeCell(row, 15, event.getUpdatedAt());
            }

            finalizeWorkbook(sheet, workbook, output, headers.size());
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể xuất file Excel sự kiện");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importEvents(MultipartFile file) {
        Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(category -> normalizeHeader(category.getName()), category -> category, (left, right) -> left));

        return importWorkbook(file, "sự kiện", headers -> {
            ensureAnyHeaderPresent(headers, "Tên sự kiện", "tensukien", "name");
            ensureAnyHeaderPresent(headers, "Danh mục (ID hoặc tên)", "danhmucid", "categoryid", "danhmuc", "category");
            ensureAnyHeaderPresent(headers, "Trạng thái", "trangthai", "status");
            ensureAnyHeaderPresent(headers, "Ngày bắt đầu", "ngaybatdau", "startdate");
            ensureAnyHeaderPresent(headers, "Ngày kết thúc", "ngayketthuc", "enddate");
        }, (row, headers, rowNumber) -> {
            Long eventId = readLong(row, headers, "id");
            EventRequest request = new EventRequest();
            request.setName(requireString(row, headers, "Tên sự kiện", "tensukien", "name"));
            request.setCategoryId(resolveCategoryId(row, headers, categoriesByName));
            request.setStatus(parseEventStatus(requireString(row, headers, "Trạng thái", "trangthai", "status")));
            request.setStartDate(requireDate(row, headers, "Ngày bắt đầu", "ngaybatdau", "startdate"));
            request.setEndDate(requireDate(row, headers, "Ngày kết thúc", "ngayketthuc", "enddate"));
            request.setCurrentAmount(readDecimalOrDefault(row, headers, BigDecimal.ZERO, "sotienhientai", "currentamount"));
            request.setTargetAmount(readDecimalOrDefault(row, headers, BigDecimal.ZERO, "muctieu", "targetamount"));
            request.setLocation(readString(row, headers, "diadiem", "location"));
            request.setShortDescription(readString(row, headers, "motangan", "shortdescription"));
            request.setContent(readString(row, headers, "noidung", "content"));
            request.setThumbnailUrl(readString(row, headers, "anhdaidien", "thumbnailurl", "imageurl"));

            validateBean(request);
            if (eventId == null) {
                eventService.createEvent(request);
            } else {
                eventService.updateEvent(eventId, request);
            }
        });
    }

    @Override
    public byte[] exportActivities(String search, EActivityStatus status) {
        List<ActivityResponse> activities = activityService
                .getAllActivities(1, MAX_EXPORT_SIZE, search, status, false)
                .getData();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("HoatDong");
            List<String> headers = List.of(
                    "ID", "Tên hoạt động", "Sự kiện ID", "Sự kiện", "Trạng thái", "Ngày bắt đầu", "Ngày kết thúc",
                    "Số tiền hiện tại", "Mục tiêu", "Địa điểm", "Mô tả ngắn", "Nội dung",
                    "Ảnh đại diện", "Số nhà hảo tâm", "Tạo lúc", "Cập nhật lúc"
            );
            writeHeaderRow(sheet, workbook, headers);

            int rowIndex = 1;
            for (ActivityResponse activity : activities) {
                Row row = sheet.createRow(rowIndex++);
                setNumberCell(row, 0, activity.getId());
                setTextCell(row, 1, activity.getName());
                setNumberCell(row, 2, activity.getEventId());
                setTextCell(row, 3, activity.getEvent() != null ? activity.getEvent().getName() : null);
                setTextCell(row, 4, activity.getStatus() != null ? activity.getStatus().getValue() : null);
                setDateCell(row, 5, activity.getStartDate());
                setDateCell(row, 6, activity.getEndDate());
                setDecimalCell(row, 7, activity.getCurrentAmount());
                setDecimalCell(row, 8, activity.getTargetAmount());
                setTextCell(row, 9, activity.getLocation());
                setTextCell(row, 10, activity.getShortDescription());
                setTextCell(row, 11, activity.getContent());
                setTextCell(row, 12, activity.getThumbnailUrl());
                setNumberCell(row, 13, activity.getNumberOfDonors());
                setDateTimeCell(row, 14, activity.getCreatedAt());
                setDateTimeCell(row, 15, activity.getUpdatedAt());
            }

            finalizeWorkbook(sheet, workbook, output, headers.size());
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể xuất file Excel hoạt động");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importActivities(MultipartFile file) {
        Map<String, Event> eventsByName = eventRepository.findAll().stream()
                .collect(Collectors.toMap(event -> normalizeHeader(event.getName()), event -> event, (left, right) -> left));

        return importWorkbook(file, "hoạt động", headers -> {
            ensureAnyHeaderPresent(headers, "Tên hoạt động", "tenhoatdong", "name");
            ensureAnyHeaderPresent(headers, "Sự kiện (ID hoặc tên)", "sukienid", "eventid", "sukien", "event");
        }, (row, headers, rowNumber) -> {
            ActivityRequest request = new ActivityRequest();
            request.setId(readLong(row, headers, "id"));
            request.setName(requireString(row, headers, "Tên hoạt động", "tenhoatdong", "name"));
            request.setEventId(resolveEventId(row, headers, eventsByName));
            request.setStatus(parseActivityStatus(readString(row, headers, "trangthai", "status")));
            request.setStartDate(readDate(row, headers, "ngaybatdau", "startdate"));
            request.setEndDate(readDate(row, headers, "ngayketthuc", "enddate"));
            request.setCurrentAmount(readDecimalOrDefault(row, headers, BigDecimal.ZERO, "sotienhientai", "currentamount"));
            request.setTargetAmount(readDecimalOrDefault(row, headers, BigDecimal.ZERO, "muctieu", "targetamount"));
            request.setLocation(readString(row, headers, "diadiem", "location"));
            request.setShortDescription(readString(row, headers, "motangan", "shortdescription"));
            request.setContent(readString(row, headers, "noidung", "content"));
            request.setThumbnailUrl(readString(row, headers, "anhdaidien", "thumbnailurl", "imageurl"));

            validateBean(request);
            activityService.saveActivity(request);
        });
    }

    @Override
    public byte[] exportDonors(String search, EDonorType type, String sortBy, String sortDir) {
        List<DonorResponse> donors = donorService
                .getAllDonor(1, MAX_EXPORT_SIZE, search, type, sortBy, sortDir)
                .getData();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("NhaHaoTam");
            List<String> headers = List.of(
                    "ID", "Loại", "Họ tên cá nhân", "Tên hiển thị", "Tên tổ chức", "Mã số thuế", "Người đại diện",
                    "Địa chỉ xuất hóa đơn", "Số điện thoại", "Email", "Nguồn biết đến", "Ghi chú",
                    "Số lần đóng góp", "Tổng tiền", "Ngày tham gia"
            );
            writeHeaderRow(sheet, workbook, headers);

            int rowIndex = 1;
            for (DonorResponse donor : donors) {
                Row row = sheet.createRow(rowIndex++);
                boolean isOrg = donor.getType() == EDonorType.ORGANIZATION;
                OrganizationResponse organization = donor.getOrganization();

                setNumberCell(row, 0, donor.getId());
                setTextCell(row, 1, donorTypeToLabel(donor.getType()));
                setTextCell(row, 2, isOrg ? null : donor.getFullName());
                setTextCell(row, 3, isOrg ? null : donor.getDisplayName());
                setTextCell(row, 4, isOrg && organization != null ? organization.getName() : null);
                setTextCell(row, 5, organization != null ? organization.getTaxCode() : null);
                setTextCell(row, 6, organization != null ? organization.getRepresentative() : null);
                setTextCell(row, 7, organization != null ? organization.getBillingAddress() : null);
                setTextCell(row, 8, donor.getPhone());
                setTextCell(row, 9, donor.getEmail());
                setTextCell(row, 10, donor.getReferralSource());
                setTextCell(row, 11, donor.getNote());
                setNumberCell(row, 12, donor.getNumberOfDonations() != null ? donor.getNumberOfDonations().longValue() : null);
                setDecimalCell(row, 13, donor.getTotalDonationAmount());
                setDateTimeCell(row, 14, donor.getCreatedAt());
            }

            finalizeWorkbook(sheet, workbook, output, headers.size());
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể xuất file Excel nhà hảo tâm");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importDonors(MultipartFile file) {
        return importWorkbook(file, "nhà hảo tâm", headers -> {
            ensureAnyHeaderPresent(headers, "Loại", "loai", "type");
            ensureAnyHeaderPresent(headers, "Số điện thoại", "sodienthoai", "phone");
        }, (row, headers, rowNumber) -> {
            Long donorId = readLong(row, headers, "id");
            EDonorType donorType = parseDonorType(requireString(row, headers, "Loại", "loai", "type"));

            if (donorType == EDonorType.INDIVIDUAL) {
                IndividualDonorRequest request = new IndividualDonorRequest();
                String fullName = requireString(row, headers, "Họ tên cá nhân", "hotencanhan", "fullname", "hoten");
                request.setFullName(fullName);
                request.setDisplayName(defaultIfBlank(readString(row, headers, "tenhienthi", "displayname"), fullName));
                request.setPhone(requireString(row, headers, "Số điện thoại", "sodienthoai", "phone"));
                request.setEmail(readString(row, headers, "email"));
                request.setReferralSource(readString(row, headers, "nguonbietden", "referralsource"));
                request.setNote(readString(row, headers, "ghichu", "note"));

                validateBean(request);
                if (donorId != null) {
                    donorService.updateIndividualDonor(donorId, request);
                } else {
                    donorService.saveIndividualDonor(request);
                }
                return;
            }

            OrganizeDonorRequest request = new OrganizeDonorRequest();
            request.setName(requireString(row, headers, "Tên tổ chức", "tentochuc", "organizationname", "name"));
            request.setTaxCode(requireString(row, headers, "Mã số thuế", "masothue", "taxcode"));
            request.setRepresentative(requireString(row, headers, "Người đại diện", "nguoidaidien", "representative"));
            request.setPhone(requireString(row, headers, "Số điện thoại", "sodienthoai", "phone"));
            request.setEmail(requireString(row, headers, "Email", "email"));
            request.setBillingAddress(readString(row, headers, "diachixuathoadon", "billingaddress"));
            request.setReferralSource(readString(row, headers, "nguonbietden", "referralsource"));
            request.setNote(readString(row, headers, "ghichu", "note"));

            validateBean(request);
            if (donorId != null) {
                donorService.updateOrganizeDonor(donorId, request);
            } else {
                donorService.saveOrganizeDonor(request);
            }
        });
    }

    @Override
    public byte[] exportDonations(String search, EDonationStatus status, EDonationTarget target, EDonationType type,
                                  EPaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount) {
        List<DonationResponse> donations = donationService
                .getAllDonations(search, status, target, type, paymentMethod, minAmount, maxAmount, 1, MAX_EXPORT_SIZE)
                .getData();

        Map<Long, Donation> donationById = donationRepository.findAllById(
                donations.stream().map(DonationResponse::getId).filter(Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(Donation::getId, donation -> donation));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("QuyenGop");
            List<String> headers = List.of(
                    "ID", "Mã đơn", "Nhà hảo tâm ID", "Nhà hảo tâm", "Số điện thoại", "Email",
                    "Số tiền", "Phương thức", "Trạng thái", "Mục tiêu", "Sự kiện ID", "Sự kiện",
                    "Hoạt động ID", "Hoạt động", "Cần biên lai", "Tên biên lai", "Email biên lai",
                    "Lời nhắn", "Kênh tạo", "Ngày quyên góp", "Ngày tạo"
            );
            writeHeaderRow(sheet, workbook, headers);

            int rowIndex = 1;
            for (DonationResponse donation : donations) {
                Donation donationEntity = donationById.get(donation.getId());
                Row row = sheet.createRow(rowIndex++);
                setNumberCell(row, 0, donation.getId());
                setTextCell(row, 1, donation.getMemoCode());
                setNumberCell(row, 2, donation.getDonorId());
                setTextCell(row, 3, donation.getDonorName());
                setTextCell(row, 4, donation.getDonorPhone());
                setTextCell(row, 5, donationEntity != null && donationEntity.getDonor() != null ? donationEntity.getDonor().getEmail() : null);
                setDecimalCell(row, 6, donation.getAmount());
                setTextCell(row, 7, donation.getPaymentMethod() != null ? donation.getPaymentMethod().getValue() : null);
                setTextCell(row, 8, donation.getStatus() != null ? donationStatusToLabel(donation.getStatus()) : null);
                setTextCell(row, 9, donation.getTarget() != null ? donation.getTarget().getValue() : null);
                setNumberCell(row, 10, donation.getEventId());
                setTextCell(row, 11, donation.getTarget() == EDonationTarget.EVENT ? donation.getObjectName() : null);
                setNumberCell(row, 12, donation.getActivityId());
                setTextCell(row, 13, donation.getTarget() == EDonationTarget.ACTIVITY ? donation.getObjectName() : null);
                setTextCell(row, 14, donation.getNeedReceipt() != null && donation.getNeedReceipt() ? "Có" : "Không");
                setTextCell(row, 15, donation.getReceiptName());
                setTextCell(row, 16, donation.getReceiptEmail());
                setTextCell(row, 17, donation.getMessage());
                setTextCell(row, 18, donationViaToLabel(donation.getDonationVia()));
                setDateTimeCell(row, 19, donation.getDonatedAt());
                setDateTimeCell(row, 20, donation.getCreatedAt());
            }

            finalizeWorkbook(sheet, workbook, output, headers.size());
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể xuất file Excel quyên góp");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importDonations(MultipartFile file, String username) {
        Map<String, Event> eventsByName = eventRepository.findAll().stream()
                .collect(Collectors.toMap(event -> normalizeHeader(event.getName()), event -> event, (left, right) -> left));
        Map<String, Activity> activitiesByName = activityRepository.findAll().stream()
                .collect(Collectors.toMap(activity -> normalizeHeader(activity.getName()), activity -> activity, (left, right) -> left));

        return importWorkbook(file, "quyên góp", headers -> {
            ensureAnyHeaderPresent(headers, "Nhà hảo tâm (ID, số điện thoại hoặc email)", "nhahotamid", "donorid", "sodienthoai", "phone", "email", "donoremail");
            ensureAnyHeaderPresent(headers, "Số tiền", "sotien", "amount");
            ensureAnyHeaderPresent(headers, "Phương thức", "phuongthuc", "paymentmethod");
        }, (row, headers, rowNumber) -> {
            Long donationId = readLong(row, headers, "id");
            Long donorId = resolveDonorId(row, headers);

            DonationRequest request = new DonationRequest();
            request.setDonorId(donorId);
            request.setAmount(requireDecimal(row, headers, "Số tiền", "sotien", "amount"));
            request.setPaymentMethod(parsePaymentMethod(requireString(row, headers, "Phương thức", "phuongthuc", "paymentmethod")));
            request.setMessage(readString(row, headers, "loinhan", "message"));
            request.setNeedReceipt(readBooleanOrDefault(row, headers, false, "canbienlai", "needreceipt"));
            request.setReceiptName(readString(row, headers, "tenbienlai", "receiptname"));
            request.setReceiptEmail(readString(row, headers, "emailbienlai", "receiptemail"));
            request.setEventId(resolveOptionalEventId(row, headers, eventsByName));
            request.setActivityId(resolveOptionalActivityId(row, headers, activitiesByName));

            if (Boolean.TRUE.equals(request.getNeedReceipt())) {
                if (!StringUtils.hasText(request.getReceiptName())) {
                    throw new InvalidDataException("Thiếu tên trên biên lai");
                }
                if (!StringUtils.hasText(request.getReceiptEmail())) {
                    throw new InvalidDataException("Thiếu email nhận biên lai");
                }
            }

            validateBean(request);

            EDonationStatus importedStatus = parseDonationStatus(readString(row, headers, "trangthai", "status"));

            if (donationId != null) {
                Donation existingDonation = donationService.getDonation(donationId);
                donationService.updateStaffDonation(donationId, request);
                if (importedStatus != null && importedStatus != existingDonation.getStatus()) {
                    donationService.changeStatusDonation(importedStatus, donationId);
                }
                return;
            }

            long createdDonationId = donationService.createStaffDonation(request, username);
            if (importedStatus != null && importedStatus != EDonationStatus.PENDING_APPROVED) {
                donationService.changeStatusDonation(importedStatus, createdDonationId);
            }
        });
    }

    @Override
    public byte[] exportTransactions(String search, EPaymentMethod method) {
        List<TransactionResponse> transactions = transactionService
                .getTransactions(1, MAX_EXPORT_SIZE, search, method)
                .getData();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("LichSuGiaoDich");
            List<String> headers = List.of(
                    "ID", "Mã giao dịch", "Số tiền", "Phương thức", "Ngân hàng", "Số tài khoản gửi",
                    "Tên tài khoản gửi", "Mô tả", "Thời gian giao dịch", "Đơn quyên góp ID", "Mã đơn", "Tạo lúc"
            );
            writeHeaderRow(sheet, workbook, headers);

            int rowIndex = 1;
            for (TransactionResponse transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                setNumberCell(row, 0, transaction.getId());
                setTextCell(row, 1, transaction.getTransactionCode());
                setDecimalCell(row, 2, transaction.getAmount());
                setTextCell(row, 3, transaction.getPaymentMethodValue());
                setTextCell(row, 4, transaction.getAccountBankId());
                setTextCell(row, 5, transaction.getCounterAccountNumber());
                setTextCell(row, 6, transaction.getCounterAccountName());
                setTextCell(row, 7, transaction.getDescription());
                setTextCell(row, 8, transaction.getTransactionDateTime());
                setNumberCell(row, 9, transaction.getDonationId());
                setTextCell(row, 10, transaction.getDonationCode());
                setDateTimeCell(row, 11, transaction.getCreatedAt());
            }

            finalizeWorkbook(sheet, workbook, output, headers.size());
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvalidDataException("Không thể xuất file Excel giao dịch");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importTransactions(MultipartFile file) {
        return importWorkbook(file, "giao dịch", headers -> {
            ensureAnyHeaderPresent(headers, "Số tiền", "sotien", "amount");
            ensureAnyHeaderPresent(headers, "Phương thức", "phuongthuc", "paymentmethod");
        }, (row, headers, rowNumber) -> {
            Long transactionId = readLong(row, headers, "id");
            Transaction transaction = transactionId != null
                    ? transactionRepository.findById(transactionId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch với ID " + transactionId))
                    : new Transaction();

            transaction.setAmount(requireDecimal(row, headers, "Số tiền", "sotien", "amount"));
            transaction.setPaymentMethod(parsePaymentMethod(requireString(row, headers, "Phương thức", "phuongthuc", "paymentmethod")));
            transaction.setAccountBankId(readString(row, headers, "nganhang", "accountbankid", "bank"));
            transaction.setCounterAccountNumber(readString(row, headers, "sotaikhoangui", "counteraccountnumber"));
            transaction.setCounterAccountName(readString(row, headers, "tentaikhoangui", "counteraccountname"));
            transaction.setDescription(readString(row, headers, "mota", "description"));
            transaction.setTransactionDateTime(readString(row, headers, "thoigiangiaodich", "transactiondatetime"));

            String transactionCode = readString(row, headers, "magiaodich", "transactioncode");
            if (StringUtils.hasText(transactionCode)) {
                Transaction existingByCode = transactionRepository.findByTransactionCode(transactionCode).orElse(null);
                if (existingByCode != null && !Objects.equals(existingByCode.getId(), transaction.getId())) {
                    throw new InvalidDataException("Mã giao dịch đã tồn tại: " + transactionCode);
                }
                transaction.setTransactionCode(transactionCode);
            } else {
                transaction.setTransactionCode(null);
            }

            Donation donation = resolveOptionalDonation(row, headers);
            if (donation != null) {
                Transaction existingByDonation = transactionRepository.findByDonationId(donation.getId()).orElse(null);
                if (existingByDonation != null && !Objects.equals(existingByDonation.getId(), transaction.getId())) {
                    throw new InvalidDataException("Đơn quyên góp #" + donation.getMemoCode() + " đã có giao dịch liên kết");
                }
                transaction.setDonation(donation);
            } else {
                transaction.setDonation(null);
            }

            transactionRepository.save(transaction);
        });
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
                    .totalRows(totalRows)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .errors(errors)
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

    @FunctionalInterface
    private interface HeaderValidator {
        void validate(Map<String, Integer> headers);
    }

    @FunctionalInterface
    private interface RowProcessor {
        void process(Row row, Map<String, Integer> headers, int rowNumber) throws Exception;
    }
}
