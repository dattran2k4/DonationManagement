-- donation.attachments definition

CREATE TABLE `attachments` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `object_id` bigint DEFAULT NULL,
                               `file_url` varchar(500) NOT NULL,
                               `file_name` varchar(255) DEFAULT NULL,
                               `entity_type` enum('ACTIVITY','DONATION','EVENT','POST') NOT NULL,
                               `file_type` enum('DOCUMENT','IMAGE','PDF','VIDEO') DEFAULT NULL,
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- donation.categories definition

CREATE TABLE `categories` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `name` varchar(255) NOT NULL,
                              `slug` varchar(255) NOT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_categories_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.events definition

CREATE TABLE `events` (
                          `current_amount` decimal(38,2) DEFAULT NULL,
                          `end_date` date DEFAULT NULL,
                          `number_of_activities` int DEFAULT NULL,
                          `start_date` date DEFAULT NULL,
                          `target_amount` decimal(38,2) DEFAULT NULL,
                          `completed_at` datetime(6) DEFAULT NULL,
                          `created_at` datetime(6) DEFAULT NULL,
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `updated_at` datetime(6) DEFAULT NULL,
                          `description` varchar(1000) DEFAULT NULL,
                          `content` text,
                          `name` varchar(255) NOT NULL,
                          `thumbnail_url` varchar(255) DEFAULT NULL,
                          `status` enum('UPCOMING','COMPLETED','ONGOING') DEFAULT NULL,
                          `slug` varchar(255) NOT NULL,
                          `short_description` varchar(255) DEFAULT NULL,
                          `category_id` int DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_events_slug` (`slug`),
                          CONSTRAINT `fk_event_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.roles definition

CREATE TABLE `roles` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `name` enum('ADMIN','STAFF','ACCOUNTING', 'DONOR') DEFAULT NULL,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.system_configs definition

CREATE TABLE `system_configs` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `config_key` varchar(255) DEFAULT NULL,
                                  `config_value` varchar(255) DEFAULT NULL,
                                  `description` varchar(255) DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_system_configs_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.users definition

CREATE TABLE `users` (
                         `created_at` datetime(6) DEFAULT NULL,
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `updated_at` datetime(6) DEFAULT NULL,
                         `email` varchar(255) DEFAULT NULL,
                         `full_name` varchar(255) DEFAULT NULL,
                         `password` varchar(255) DEFAULT NULL,
                         `phone` varchar(255) DEFAULT NULL,
                         `username` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_users_email` (`email`),
                         UNIQUE KEY `uk_users_phone` (`phone`),
                         UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.activities definition

CREATE TABLE `activities` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `completed_at` datetime(6) DEFAULT NULL,
                              `content` text,
                              `created_at` datetime(6) DEFAULT NULL,
                              `current_amount` decimal(38,2) DEFAULT NULL,
                              `description` varchar(255) DEFAULT NULL,
                              `end_date` date DEFAULT NULL,
                              `name` varchar(255) DEFAULT NULL,
                              `slug` varchar(255) DEFAULT NULL,
                              `start_date` date DEFAULT NULL,
                              `status` enum('UPCOMING','COMPLETED','ONGOING') DEFAULT NULL,
                              `target_amount` decimal(38,2) DEFAULT NULL,
                              `thumbnail_url` varchar(255) DEFAULT NULL,
                              `updated_at` datetime(6) DEFAULT NULL,
                              `event_id` bigint DEFAULT NULL,
                              `short_description` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_activities_slug` (`slug`),
                              CONSTRAINT `fk_activity_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.donors definition

CREATE TABLE `donors` (
                          `created_at` datetime(6) DEFAULT NULL,
                          `created_by_user_id` bigint DEFAULT NULL,
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `updated_at` datetime(6) DEFAULT NULL,
                          `note` text,
                          `email` varchar(255) NOT NULL,
                          `full_name` varchar(255) NOT NULL,
                          `phone` varchar(255) NOT NULL,
                          `referral_source` varchar(255) DEFAULT NULL,
                          `type` enum('INDIVIDUAL','ORGANIZATION') NOT NULL,
                          `display_name` varchar(255) DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_donors_email` (`email`),
                          UNIQUE KEY `uk_donors_phone` (`phone`),
                          CONSTRAINT `fk_donor_created_by` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.organizations definition

CREATE TABLE `organizations` (
                                 `id` bigint NOT NULL,
                                 `billing_address` text,
                                 `name` varchar(255) DEFAULT NULL,
                                 `representative` varchar(255) DEFAULT NULL,
                                 `tax_code` varchar(255) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_organization_tax_code` (`tax_code`),
                                 CONSTRAINT `fk_organization_donor` FOREIGN KEY (`id`) REFERENCES `donors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.user_roles definition

CREATE TABLE `user_roles` (
                              `role_id` bigint NOT NULL,
                              `user_id` bigint NOT NULL,
                              PRIMARY KEY (`role_id`,`user_id`),
                              KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
                              CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
                              CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.donations definition

CREATE TABLE donations (
                           id bigint NOT NULL AUTO_INCREMENT,
                           amount decimal(38,2) NOT NULL,
                           approval_required bit(1) DEFAULT NULL,
                           confirmed_at datetime(6) DEFAULT NULL,
                           created_at datetime(6) DEFAULT NULL,
                           donated_at datetime(6) DEFAULT NULL,
                           donation_via enum('STAFF','WEB') DEFAULT NULL,
                           memo_code varchar(255) DEFAULT NULL,
                           message text,
                           need_receipt bit(1) DEFAULT NULL,
                           order_code bigint DEFAULT NULL,
                           payment_method enum('BANK_TRANSFER_OFFLINE','BANK_TRANSFER_ONLINE','CASH') DEFAULT NULL,
                           receipt_email varchar(255) DEFAULT NULL,
                           receipt_name varchar(255) DEFAULT NULL,
                           status enum('CANCELLED','CONFIRMED','FAILED','PENDING_APPROVED','PENDING_PAYMENT','REJECTED') DEFAULT NULL,
                           target enum('ACTIVITY','EVENT','NONE') DEFAULT NULL,
                           type enum('MONEY','ITEM') DEFAULT NULL,
                           updated_at datetime(6) DEFAULT NULL,
                           activity_id bigint DEFAULT NULL,
                           confirmed_by_user_id bigint DEFAULT NULL,
                           created_by_user_id bigint DEFAULT NULL,
                           donor_id bigint DEFAULT NULL,
                           event_id bigint DEFAULT NULL,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) REFERENCES donors (id),
                           CONSTRAINT fk_donation_event FOREIGN KEY (event_id) REFERENCES events (id),
                           CONSTRAINT fk_donation_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
                           CONSTRAINT fk_donation_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
                           CONSTRAINT fk_donation_confirmed_by FOREIGN KEY (confirmed_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- donation.donation_transactions definition

CREATE TABLE `donation_transactions` (
                                         `amount` decimal(38,2) DEFAULT NULL,
                                         `payment_method` enum('BANK_TRANSFER_OFFLINE','BANK_TRANSFER_ONLINE','CASH') DEFAULT NULL,
                                         `created_at` datetime(6) DEFAULT NULL,
                                         `donation_id` bigint DEFAULT NULL,
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `updated_at` datetime(6) DEFAULT NULL,
                                         `account_bank_id` varchar(255) DEFAULT NULL,
                                         `counter_account_name` varchar(255) DEFAULT NULL,
                                         `counter_account_number` varchar(255) DEFAULT NULL,
                                         `description` varchar(255) DEFAULT NULL,
                                         `raw_api_data` text,
                                         `transaction_code` varchar(255) DEFAULT NULL,
                                         `transaction_date_time` varchar(255) DEFAULT NULL,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_donation_transactions_donation` (`donation_id`),
                                         CONSTRAINT `fk_donation_transactions_donations` FOREIGN KEY (`donation_id`) REFERENCES `donations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1) roles (4 dòng)
INSERT INTO roles (id, name) VALUES
                                 (1, 'ADMIN'),
                                 (2, 'STAFF'),
                                 (3, 'ACCOUNTING'),
                                 (4, 'DONOR');

-- 2) users (6 dòng)
INSERT INTO users (id, created_at, updated_at, email, full_name, password, phone, username) VALUES
                                                                                                (1, '2026-02-01 08:10:00.000000', '2026-02-20 10:00:00.000000', 'admin@gms.local',      'Quản trị hệ thống',  '$2y$demo_admin', '0905000001', 'admin'),
                                                                                                (2, '2026-02-02 09:00:00.000000', '2026-02-21 09:10:00.000000', 'staff1@gms.local',     'Nguyễn Minh Anh',     '$2y$demo_staff', '0905000002', 'staff.minhanh'),
                                                                                                (3, '2026-02-02 09:05:00.000000', '2026-02-21 09:15:00.000000', 'account1@gms.local',   'Trần Thu Hà',         '$2y$demo_acc',   '0905000003', 'acc.thuha'),
                                                                                                (4, '2026-02-03 14:20:00.000000', '2026-02-21 09:20:00.000000', 'staff2@gms.local',     'Lê Quốc Bảo',         '$2y$demo_staff', '0905000004', 'staff.quocbao'),
                                                                                                (5, '2026-02-03 15:00:00.000000', '2026-02-21 09:25:00.000000', 'account2@gms.local',   'Phạm Ngọc Linh',      '$2y$demo_acc',   '0905000005', 'acc.ngoclinh'),
                                                                                                (6, '2026-02-05 08:30:00.000000', '2026-02-22 08:00:00.000000', 'donor.portal@gms.local','Tài khoản nhà hảo tâm','$2y$demo_donor', '0905000006', 'donor.portal');

-- 3) user_roles (6 dòng)
INSERT INTO user_roles (role_id, user_id) VALUES
                                              (1, 1), -- admin
                                              (2, 2), -- staff
                                              (3, 3), -- accounting
                                              (2, 4), -- staff
                                              (3, 5), -- accounting
                                              (4, 6); -- donor portal user

-- 4) system_configs (5 dòng)
INSERT INTO system_configs (id, config_key, config_value, description) VALUES
                                                                           (1, 'club_name', 'CLB Chia sẻ Yêu Thương', 'Tên hiển thị của câu lạc bộ'),
                                                                           (2, 'default_currency', 'VND', 'Đơn vị tiền tệ mặc định'),
                                                                           (3, 'receipt_sender_email', 'receipt@gms.local', 'Email gửi biên nhận'),
                                                                           (4, 'bank_account_display', 'Ngân hàng Agribank - 2000206383413 - CLB Chia sẻ Yêu Thương', 'Thông tin tài khoản nhận chuyển khoản'),
                                                                           (5, 'approval_threshold_vnd', '10000000', 'Ngưỡng cần duyệt (VND)');

-- 5) categories (4 dòng)
INSERT INTO categories (id, name, slug) VALUES
                                            (1, 'Y tế', 'y-te'),
                                            (2, 'Giáo dục', 'giao-duc'),
                                            (3, 'Cứu trợ thiên tai', 'cuu-tro-thien-tai'),
                                            (4, 'Học bổng', 'hoc-bong');

-- 6) events (4 dòng, đủ UPCOMING/ONGOING/COMPLETED)
INSERT INTO events (
    id, name, slug, status, short_description, description, content, thumbnail_url,
    target_amount, current_amount, number_of_activities,
    start_date, end_date, completed_at, created_at, updated_at, category_id
) VALUES
      (1, 'Gây quỹ mổ tim cho bé An', 'gay-quy-mo-tim-be-an', 'ONGOING',
       'Hỗ trợ chi phí phẫu thuật tim bẩm sinh cho bé An.',
       'Chiến dịch kêu gọi cộng đồng hỗ trợ chi phí phẫu thuật và hồi phục.',
       'Nội dung chi tiết: công khai tiến độ, chứng từ và cập nhật sức khỏe định kỳ.',
       'https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       200000000.00, 125500000.00, 2,
       '2026-01-15', '2026-03-31', NULL,
       '2026-01-10 09:00:00.000000', '2026-02-22 09:00:00.000000', 1),

      (2, 'Tết Ấm Miền Trung 2026', 'tet-am-mien-trung-2026', 'COMPLETED',
       'Trao quà Tết cho các hộ khó khăn tại miền Trung.',
       'Chương trình tổng hợp quà thiết yếu và tiền mặt, trao trực tiếp theo danh sách xác minh.',
       'Nội dung chi tiết: báo cáo chi, hình ảnh trao quà, danh sách điểm phát.',
       'https://images.unsplash.com/photo-1599059813005-11265ba4b4ce?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       150000000.00, 158200000.00, 2,
       '2025-12-15', '2026-01-31', '2026-02-02 18:00:00.000000',
       '2025-12-10 08:30:00.000000', '2026-02-03 10:00:00.000000', 3),

      (3, 'Học bổng Tiếp Bước 2026', 'hoc-bong-tiep-buoc-2026', 'UPCOMING',
       'Học bổng cho học sinh có hoàn cảnh khó khăn.',
       'Mở đợt tiếp nhận hồ sơ và xét duyệt theo tiêu chí minh bạch.',
       'Nội dung chi tiết: tiêu chí, quy trình xét duyệt, lịch trao học bổng.',
       'https://images.unsplash.com/photo-1608686207856-001b95cf60ca?q=80&w=927&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       300000000.00, 0.00, 1,
       '2026-03-01', '2026-06-30', NULL,
       '2026-02-15 09:00:00.000000', '2026-02-22 09:10:00.000000', 4),

      (4, 'Sách cho em vùng cao', 'sach-cho-em-vung-cao', 'ONGOING',
       'Gây quỹ mua sách và dụng cụ học tập cho học sinh vùng cao.',
       'Mua sách theo danh mục được nhà trường đề xuất và trao tặng theo đợt.',
       'Nội dung chi tiết: danh mục sách, biên nhận mua hàng, lịch trao tặng.',
       'https://images.unsplash.com/photo-1593113598332-cd288d649433?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       80000000.00, 24300000.00, 1,
       '2026-02-01', '2026-04-30', NULL,
       '2026-01-25 10:00:00.000000', '2026-02-22 09:20:00.000000', 2);

-- 7) activities (5 dòng)
INSERT INTO activities (
    id, event_id, name, slug, status, short_description, description, content, thumbnail_url,
    target_amount, current_amount, start_date, end_date, completed_at, created_at, updated_at
) VALUES
      (1, 1, 'Đợt 1 - Chi phí phẫu thuật', 'dot-1-chi-phi-phau-thuat', 'ONGOING',
       'Gom đủ chi phí phẫu thuật theo dự toán bệnh viện.',
       'Đợt 1 tập trung chi phí phẫu thuật và vật tư y tế.',
       'Cập nhật: dự toán, biên lai, tiến độ đóng góp.',
       'https://images.unsplash.com/photo-1532629345422-7515f3d16bb6?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       140000000.00, 98500000.00, '2026-01-15', '2026-02-27', NULL,
       '2026-01-10 10:00:00.000000', '2026-02-22 09:30:00.000000'),

      (2, 1, 'Đợt 2 - Hậu phẫu & phục hồi', 'dot-2-hau-phau-phuc-hoi', 'UPCOMING',
       'Hỗ trợ chi phí thuốc và tái khám sau mổ.',
       'Đợt 2 dự kiến mở sau khi bé hoàn thành phẫu thuật.',
       'Cập nhật: kế hoạch chi phí, lịch tái khám.',
       'https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       60000000.00, 0.00, '2026-03-01', '2026-03-31', NULL,
       '2026-02-10 09:00:00.000000', '2026-02-22 09:31:00.000000'),

      (3, 2, 'Trao quà Quảng Nam', 'trao-qua-quang-nam', 'COMPLETED',
       'Trao quà Tết tại 2 xã thuộc Quảng Nam.',
       'Đã trao quà theo danh sách xác minh của địa phương.',
       'Cập nhật: hình ảnh trao quà, danh sách nhận quà.',
       'https://images.unsplash.com/photo-1469571486292-0ba58a3f068b?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       70000000.00, 72000000.00, '2026-01-10', '2026-01-20', '2026-01-20 17:30:00.000000',
       '2026-01-05 09:00:00.000000', '2026-01-22 10:00:00.000000'),

      (4, 2, 'Sửa nhà sau bão', 'sua-nha-sau-bao', 'COMPLETED',
       'Hỗ trợ sửa chữa nhà ở bị hư hại.',
       'Hoàn tất khảo sát và hỗ trợ vật liệu + nhân công.',
       'Cập nhật: biên nhận vật tư, hình ảnh trước/sau.',
       'https://plus.unsplash.com/premium_photo-1663040178972-ee1d45d33899?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       80000000.00, 86200000.00, '2025-12-20', '2026-01-31', '2026-02-01 12:00:00.000000',
       '2025-12-18 08:00:00.000000', '2026-02-02 09:00:00.000000'),

      (5, 4, 'Mua sách đợt 1', 'mua-sach-dot-1', 'ONGOING',
       'Mua sách theo danh mục nhà trường đề xuất.',
       'Mua và đóng gói sách theo lớp để trao trong tháng 3.',
       'Cập nhật: danh mục sách, hóa đơn, tiến độ đóng góp.',
       'https://images.unsplash.com/photo-1593113616828-6f22bca04804?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
       50000000.00, 24300000.00, '2026-02-05', '2026-03-10', NULL,
       '2026-02-01 09:00:00.000000', '2026-02-22 09:40:00.000000');

-- 8) donors (5 dòng)
INSERT INTO donors (
    id, created_at, updated_at, created_by_user_id,
    email, full_name, phone, referral_source, type, display_name, note
) VALUES
      (1, '2026-02-05 10:00:00.000000', '2026-02-22 09:00:00.000000', 2,
       'lan.pham@example.com', 'Phạm Thị Lan', '0907000001', 'Facebook', 'INDIVIDUAL', 'Chị Lan', 'Ủng hộ định kỳ hằng tháng.'),

      (2, '2026-02-06 11:20:00.000000', '2026-02-22 09:05:00.000000', 2,
       'minh.nguyen@example.com', 'Nguyễn Văn Minh', '0907000002', 'Bạn bè giới thiệu', 'INDIVIDUAL', 'Anh Minh', 'Ưu tiên các chiến dịch giáo dục.'),

      (3, '2026-02-07 08:45:00.000000', '2026-02-22 09:10:00.000000', 4,
       'huong.tran@example.com', 'Trần Ngọc Hương', '0907000003', 'Sự kiện offline', 'INDIVIDUAL', 'Cô Hương', 'Thường xin biên nhận.'),

      (4, '2026-02-08 14:10:00.000000', '2026-02-22 09:15:00.000000', 4,
       'contact@anphat-co.local', 'Công ty TNHH An Phát', '0907000004', 'Đối tác', 'ORGANIZATION', 'An Phát Co.', 'Tài trợ theo chương trình cộng đồng.'),

      (5, '2026-02-09 09:30:00.000000', '2026-02-22 09:18:00.000000', 2,
       'csr@thientam-group.local', 'Thiện Tâm Group', '0907000005', 'Email', 'ORGANIZATION', 'Thiện Tâm Group', 'Quan tâm cứu trợ và y tế.');

-- 9) organizations (2 dòng)
INSERT INTO organizations (id, billing_address, name, representative, tax_code) VALUES
                                                                                              (4, '12 Đường Số 3, Quận Hải Châu, Đà Nẵng', 'Công ty TNHH An Phát', 'Nguyễn Hoài Nam', '0402123456'),
                                                                                              (5, '88 Nguyễn Văn Linh, Quận Thanh Khê, Đà Nẵng', 'Thiện Tâm Group', 'Trần Thị Mai', '0402987654');

-- 10) donations (10 dòng)
INSERT INTO donations (
    id, amount, approval_required, confirmed_at, created_at, donated_at,
    donation_via, memo_code, message, need_receipt, order_code, payment_method,
    receipt_email, receipt_name, status, target, type, updated_at,
    activity_id, confirmed_by_user_id, created_by_user_id, donor_id, event_id
) VALUES
      (1,  500000.00, 0, '2026-02-10 10:30:00.000000', '2026-02-10 10:05:00.000000', '2026-02-10 10:05:00.000000',
       'WEB', 'GMS-20260210-0001', 'Chúc bé sớm khỏe lại.', 1, 202602100001, 'BANK_TRANSFER_ONLINE',
       'lan.pham@example.com', 'Phạm Thị Lan', 'CONFIRMED', 'ACTIVITY', 'MONEY', '2026-02-10 10:30:00.000000',
       1, 3, NULL, 1, NULL),

      (2, 2000000.00, 0, NULL, '2026-02-11 09:10:00.000000', '2026-02-11 09:10:00.000000',
       'WEB', 'GMS-20260211-0002', 'Góp một phần nhỏ cho chương trình.', 0, 202602110002, 'BANK_TRANSFER_OFFLINE',
       NULL, NULL, 'PENDING_PAYMENT', 'EVENT', 'MONEY', '2026-02-11 09:10:00.000000',
       NULL, NULL, NULL, 2, 4),

      (3, 30000000.00, 1, NULL, '2026-02-12 14:20:00.000000', '2026-02-12 14:20:00.000000',
       'STAFF', 'GMS-20260212-0003', 'Tài trợ theo chương trình cộng đồng, vui lòng liên hệ xác nhận.', 1, 202602120003, 'BANK_TRANSFER_OFFLINE',
       'accounting@anphat-co.local', 'Công ty TNHH An Phát', 'PENDING_APPROVED', 'EVENT', 'MONEY', '2026-02-12 14:20:00.000000',
       NULL, NULL, 2, 4, 2),

      (4, 1500000.00, 0, NULL, '2026-02-13 08:55:00.000000', '2026-02-13 08:55:00.000000',
       'WEB', 'GMS-20260213-0004', 'Không tiện để lại thông tin thêm.', 0, 202602130004, 'BANK_TRANSFER_ONLINE',
       NULL, NULL, 'FAILED', 'ACTIVITY', 'MONEY', '2026-02-13 08:55:00.000000',
       5, NULL, NULL, 3, NULL),

      (5, 800000.00, 0, NULL, '2026-02-14 11:00:00.000000', '2026-02-14 11:00:00.000000',
       'WEB', 'GMS-20260214-0005', 'Xin hủy do nhập nhầm số tiền.', 0, 202602140005, 'BANK_TRANSFER_ONLINE',
       NULL, NULL, 'CANCELLED', 'EVENT', 'MONEY', '2026-02-14 11:05:00.000000',
       NULL, NULL, NULL, 1, 1),

      (6, 1200000.00, 0, NULL, '2026-02-15 16:40:00.000000', '2026-02-15 16:40:00.000000',
       'WEB', 'GMS-20260215-0006', 'Mong chương trình triển khai suôn sẻ.', 0, 202602150006, 'BANK_TRANSFER_OFFLINE',
       NULL, NULL, 'REJECTED', 'EVENT', 'MONEY', '2026-02-15 17:10:00.000000',
       NULL, NULL, NULL, 2, 3),

      (7, 1000000.00, 0, '2026-02-16 10:15:00.000000', '2026-02-16 09:50:00.000000', '2026-02-16 09:50:00.000000',
       'STAFF', 'GMS-20260216-0007', 'Đóng góp tại buổi gây quỹ offline.', 1, 202602160007, 'CASH',
       'minh.nguyen@example.com', 'Nguyễn Văn Minh', 'CONFIRMED', 'EVENT', 'MONEY', '2026-02-16 10:15:00.000000',
       NULL, 5, 4, 2, 4),

      (8, 5000000.00, 0, '2026-02-17 15:30:00.000000', '2026-02-17 15:00:00.000000', '2026-02-17 15:00:00.000000',
       'STAFF', 'GMS-20260217-0008', 'Hiện vật quy đổi theo giá trị hóa đơn (ước tính).', 1, 202602170008, 'CASH',
       'huong.tran@example.com', 'Trần Ngọc Hương', 'CONFIRMED', 'ACTIVITY', 'ITEM', '2026-02-17 15:30:00.000000',
       3, 3, 2, 3, NULL),

      (9, 250000.00, 0, '2026-02-18 09:20:00.000000', '2026-02-18 09:10:00.000000', '2026-02-18 09:10:00.000000',
       'WEB', 'GMS-20260218-0009', 'Ủng hộ chung cho hoạt động của CLB.', 0, 202602180009, 'BANK_TRANSFER_ONLINE',
       NULL, NULL, 'CONFIRMED', 'NONE', 'MONEY', '2026-02-18 09:20:00.000000',
       NULL, 3, NULL, 1, NULL),

      (10, 3500000.00, 0, NULL, '2026-02-19 13:30:00.000000', '2026-02-19 13:30:00.000000',
       'WEB', 'GMS-20260219-0010', 'Chuyển khoản theo nội dung, nhờ CLB xác nhận sau.', 1, 202602190010, 'BANK_TRANSFER_OFFLINE',
       'csr@thientam-group.local', 'Thiện Tâm Group', 'PENDING_PAYMENT', 'ACTIVITY', 'MONEY', '2026-02-19 13:30:00.000000',
       1, NULL, NULL, 5, NULL);

-- 11) donation_transactions (6 dòng)
INSERT INTO donation_transactions (
    id, donation_id, amount, payment_method, created_at, updated_at,
    account_bank_id, counter_account_name, counter_account_number,
    description, raw_api_data, transaction_code, transaction_date_time
) VALUES
      (1, 1,  500000.00,  'BANK_TRANSFER_ONLINE',  '2026-02-10 10:30:00.000000', '2026-02-10 10:30:00.000000',
       'BANKTXN-20260210-0001', 'PHAM THI LAN',     '970400000001', 'Ung ho dot 1 phau thuat', NULL, 'FT2602100001', '2026-02-10 10:29:10'),

      (2, 2,  2000000.00, 'BANK_TRANSFER_OFFLINE', '2026-02-11 09:10:30.000000', '2026-02-11 09:10:30.000000',
       'BANKTXN-20260211-0002', 'NGUYEN VAN MINH',  '970400000002', 'Ung ho sach vung cao', NULL, 'FT2602110002', '2026-02-11 09:08:42'),

      (3, 3,  30000000.00,'BANK_TRANSFER_OFFLINE', '2026-02-12 14:25:00.000000', '2026-02-12 14:25:00.000000',
       'BANKTXN-20260212-0003', 'AN PHAT CO LTD',   '970400000003', 'Tai tro Tet am Mien Trung', NULL, 'FT2602120003', '2026-02-12 14:21:05'),

      (4, 4,  1500000.00, 'BANK_TRANSFER_ONLINE',  '2026-02-13 09:00:00.000000', '2026-02-13 09:00:00.000000',
       'BANKTXN-20260213-0004', 'TRAN NGOC HUONG',  '970400000004', 'Ung ho mua sach dot 1', NULL, 'FT2602130004', '2026-02-13 08:56:30'),

      (5, 9,  250000.00,  'BANK_TRANSFER_ONLINE',  '2026-02-18 09:20:00.000000', '2026-02-18 09:20:00.000000',
       'BANKTXN-20260218-0009', 'PHAM THI LAN',     '970400000005', 'Ung ho chung hoat dong CLB', NULL, 'FT2602180009', '2026-02-18 09:19:05'),

      (6, 10, 3500000.00, 'BANK_TRANSFER_OFFLINE', '2026-02-19 13:35:00.000000', '2026-02-19 13:35:00.000000',
       'BANKTXN-20260219-0010', 'THIEN TAM GROUP',  '970400000006', 'Ung ho dot 1 phau thuat', NULL, 'FT2602190010', '2026-02-19 13:31:18');

-- 12) attachments (5 dòng)
INSERT INTO attachments (id, object_id, file_url, file_name, entity_type, file_type) VALUES
                                                                                         (1, 1,  'https://example.com/uploads/2026/02/bienlai-phauthuat-dot1.pdf', 'bienlai-phauthuat-dot1.pdf', 'ACTIVITY', 'PDF'),
                                                                                         (2, 3,  'https://example.com/uploads/2026/02/anh-trao-qua-quang-nam-01.jpg', 'anh-trao-qua-quang-nam-01.jpg', 'ACTIVITY', 'IMAGE'),
                                                                                         (3, 1,  'https://example.com/uploads/2026/02/ck-gms-20260210-0001.png',     'ck-gms-20260210-0001.png',     'DONATION', 'IMAGE'),
                                                                                         (4, 2,  'https://example.com/uploads/2026/02/bao-cao-tet-am-2026.docx',     'bao-cao-tet-am-2026.docx',     'EVENT', 'DOCUMENT'),
                                                                                         (5, 999,'https://example.com/uploads/2026/02/bai-viet-gioi-thieu.mp4',      'bai-viet-gioi-thieu.mp4',      'EVENT', 'VIDEO');