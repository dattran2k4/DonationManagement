import {
  pagedResponse,
  stubDownloadApis,
  visitAdminPage
} from './helpers/adminTestUtils.js';

const excelMimeType =
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const modules = [
  {
    key: 'donor',
    label: 'nhà hảo tâm',
    exportTitle: 'TC-ADM-DONOR-012 - Export danh sách nhà hảo tâm ra Excel',
    importTitle: 'TC-ADM-DONOR-013 - Import danh sách nhà hảo tâm từ Excel',
    page: '/admin/donors',
    listPattern: '/api/donors?*',
    exportPattern: '/api/admin/excel/donors/export*',
    importPattern: '/api/admin/excel/donors/import',
    exportButton: '#donorExportBtn',
    importInput: '#donorImportInput',
    exportedFilename: 'nha-hao-tam.xlsx',
    exportSuccessMessage: 'Xuất Excel nhà hảo tâm thành công.',
    importSuccessMessage: 'Nhập Excel nhà hảo tâm thành công.',
    initialRows: [
      {
        id: 1,
        type: 'INDIVIDUAL',
        fullName: 'Nguyen Thi Lan',
        phone: '0911111111',
        email: 'lan@test.local',
        createdAt: '2026-03-01T08:00:00',
        numberOfDonations: 2,
        totalDonationAmount: 5000000
      }
    ],
    importedRows: [
      {
        id: 1,
        type: 'INDIVIDUAL',
        fullName: 'Nguyen Thi Lan',
        phone: '0911111111',
        email: 'lan@test.local',
        createdAt: '2026-03-01T08:00:00',
        numberOfDonations: 2,
        totalDonationAmount: 5000000
      },
      {
        id: 2,
        type: 'ORGANIZATION',
        fullName: 'Cong ty Excel Moi',
        phone: '0909999999',
        email: 'excel@test.local',
        createdAt: '2026-03-05T08:00:00',
        numberOfDonations: 0,
        totalDonationAmount: 0,
        organization: {
          name: 'Cong ty Excel Moi',
          representative: 'Tran Excel'
        }
      }
    ],
    importedAssertion: 'Cong ty Excel Moi'
  },
  {
    key: 'donation',
    label: 'quyên góp',
    exportTitle: 'TC-ADM-DON-016 - Export danh sách donation ra Excel',
    importTitle: 'TC-ADM-DON-017 - Import danh sách donation từ Excel',
    page: '/admin/donations',
    listPattern: '/api/donations/list?*',
    exportPattern: '/api/admin/excel/donations/export*',
    importPattern: '/api/admin/excel/donations/import',
    exportButton: '#donationExportBtn',
    importInput: '#donationImportInput',
    exportedFilename: 'quyen-gop.xlsx',
    exportSuccessMessage: 'Xuất Excel quyên góp thành công.',
    importSuccessMessage: 'Nhập Excel quyên góp thành công.',
    initialRows: [
      {
        id: 10,
        memoCode: 'MEMO001',
        donorName: 'Nguyen Thi Lan',
        donationVia: 'STAFF',
        amount: 2500000,
        objectName: 'Chiến dịch A',
        target: 'EVENT',
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        status: 'PENDING_APPROVED',
        donatedAt: '2026-03-22T08:15:00'
      }
    ],
    importedRows: [
      {
        id: 10,
        memoCode: 'MEMO001',
        donorName: 'Nguyen Thi Lan',
        donationVia: 'STAFF',
        amount: 2500000,
        objectName: 'Chiến dịch A',
        target: 'EVENT',
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        status: 'PENDING_APPROVED',
        donatedAt: '2026-03-22T08:15:00'
      },
      {
        id: 11,
        memoCode: 'MEMO-IMPORT-02',
        donorName: 'Donor Excel Donation',
        donationVia: 'STAFF',
        amount: 4000000,
        objectName: 'Hoạt động Excel',
        target: 'ACTIVITY',
        paymentMethod: 'CASH',
        status: 'CONFIRMED',
        donatedAt: '2026-03-23T10:00:00'
      }
    ],
    importedAssertion: 'MEMO-IMPORT-02'
  },
  {
    key: 'event',
    label: 'sự kiện',
    exportTitle: 'TC-ADM-EVT-012 - Export danh sách sự kiện ra Excel',
    importTitle: 'TC-ADM-EVT-013 - Import danh sách sự kiện từ Excel',
    page: '/admin/events',
    listPattern: '/api/events?*',
    exportPattern: '/api/admin/excel/events/export*',
    importPattern: '/api/admin/excel/events/import',
    exportButton: '#eventExportBtn',
    importInput: '#eventImportInput',
    exportedFilename: 'su-kien.xlsx',
    exportSuccessMessage: 'Xuất Excel sự kiện thành công.',
    importSuccessMessage: 'Nhập Excel sự kiện thành công.',
    initialRows: [
      {
        id: 1,
        name: 'Gây quỹ mổ tim',
        code: 'EVT-001',
        categoryId: 1,
        status: 'ONGOING',
        currentAmount: 25000000,
        targetAmount: 50000000,
        startDate: '2026-03-01',
        endDate: '2026-04-01',
        thumbnailUrl: ''
      }
    ],
    importedRows: [
      {
        id: 1,
        name: 'Gây quỹ mổ tim',
        code: 'EVT-001',
        categoryId: 1,
        status: 'ONGOING',
        currentAmount: 25000000,
        targetAmount: 50000000,
        startDate: '2026-03-01',
        endDate: '2026-04-01',
        thumbnailUrl: ''
      },
      {
        id: 2,
        name: 'Sự kiện import Cypress',
        code: 'EVT-IMPORT',
        categoryId: 1,
        status: 'UPCOMING',
        currentAmount: 0,
        targetAmount: 12000000,
        startDate: '2026-04-10',
        endDate: '2026-05-10',
        thumbnailUrl: ''
      }
    ],
    importedAssertion: 'Sự kiện import Cypress'
  },
  {
    key: 'activity',
    label: 'hoạt động',
    exportTitle: 'TC-ADM-ACT-008 - Export danh sách hoạt động ra Excel',
    importTitle: 'TC-ADM-ACT-009 - Import danh sách hoạt động từ Excel',
    page: '/admin/activities',
    listPattern: '/api/activities?*',
    exportPattern: '/api/admin/excel/activities/export*',
    importPattern: '/api/admin/excel/activities/import',
    exportButton: '#activityExportBtn',
    importInput: '#activityImportInput',
    exportedFilename: 'hoat-dong.xlsx',
    exportSuccessMessage: 'Xuất Excel hoạt động thành công.',
    importSuccessMessage: 'Nhập Excel hoạt động thành công.',
    initialRows: [
      {
        id: 301,
        name: 'Khám sàng lọc',
        event: { name: 'Gây quỹ mổ tim' },
        startDate: '2026-03-10',
        endDate: '2026-03-12',
        location: 'Bệnh viện A',
        currentAmount: 5000000,
        targetAmount: 10000000,
        status: 'ONGOING'
      }
    ],
    importedRows: [
      {
        id: 301,
        name: 'Khám sàng lọc',
        event: { name: 'Gây quỹ mổ tim' },
        startDate: '2026-03-10',
        endDate: '2026-03-12',
        location: 'Bệnh viện A',
        currentAmount: 5000000,
        targetAmount: 10000000,
        status: 'ONGOING'
      },
      {
        id: 302,
        name: 'Hoạt động import Cypress',
        event: { name: 'Gây quỹ mổ tim' },
        startDate: '2026-04-10',
        endDate: '2026-04-15',
        location: 'Đà Nẵng',
        currentAmount: 0,
        targetAmount: 8000000,
        status: 'UPCOMING'
      }
    ],
    importedAssertion: 'Hoạt động import Cypress'
  },
  {
    key: 'transaction',
    label: 'giao dịch',
    exportTitle: 'TC-ADM-TRX-005 - Export danh sách giao dịch ra Excel',
    importTitle: 'TC-ADM-TRX-006 - Import danh sách giao dịch từ Excel',
    page: '/admin/transactions',
    listPattern: '/api/transactions?*',
    exportPattern: '/api/admin/excel/transactions/export*',
    importPattern: '/api/admin/excel/transactions/import',
    exportButton: '#transactionExportBtn',
    importInput: '#transactionImportInput',
    exportedFilename: 'giao-dich.xlsx',
    exportSuccessMessage: 'Xuất Excel giao dịch thành công.',
    importSuccessMessage: 'Nhập Excel giao dịch thành công.',
    initialRows: [
      {
        id: 401,
        transactionCode: 'TXN-401',
        amount: 1000000,
        counterAccountName: 'Nguyen Van Chuyen Khoan',
        counterAccountNumber: '123456789',
        donationCode: 'MEMO001',
        paymentMethodValue: 'Chuyển khoản online',
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        createdAt: '2026-03-22T08:00:00'
      }
    ],
    importedRows: [
      {
        id: 401,
        transactionCode: 'TXN-401',
        amount: 1000000,
        counterAccountName: 'Nguyen Van Chuyen Khoan',
        counterAccountNumber: '123456789',
        donationCode: 'MEMO001',
        paymentMethodValue: 'Chuyển khoản online',
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        createdAt: '2026-03-22T08:00:00'
      },
      {
        id: 402,
        transactionCode: 'TXN-IMPORT-02',
        amount: 2500000,
        counterAccountName: 'Excel Import Transfer',
        counterAccountNumber: '970400001234',
        donationCode: null,
        paymentMethodValue: 'Tiền mặt',
        paymentMethod: 'CASH',
        createdAt: '2026-03-22T09:00:00'
      }
    ],
    importedAssertion: 'TXN-IMPORT-02'
  }
];

function stubListResponses(config) {
  let loadCount = 0;

  cy.intercept('GET', config.listPattern, (req) => {
    loadCount += 1;
    const rows = loadCount === 1 ? config.initialRows : config.importedRows;

    req.reply({
      statusCode: 200,
      body: pagedResponse(rows, {
        pageSize: 50,
        totalItems: rows.length
      })
    });
  }).as(`${config.key}List`);
}

describe('Nhập và xuất dữ liệu quản trị', () => {
  modules.forEach((config) => {
    it(config.exportTitle, () => {
      stubListResponses(config);

      cy.intercept('GET', config.exportPattern, {
        statusCode: 200,
        body: 'fake-excel-content',
        headers: {
          'content-type': excelMimeType,
          'content-disposition': `attachment; filename="${config.exportedFilename}"`
        }
      }).as(`${config.key}Export`);

      visitAdminPage(config.page);
      cy.wait(`@${config.key}List`).its('request.query.size').should('eq', '50');
      stubDownloadApis();

      cy.get(config.exportButton).click();

      cy.wait(`@${config.key}Export`);
      cy.contains('.admin-toast__message', config.exportSuccessMessage).should('be.visible');
    });

    it(config.importTitle, () => {
      stubListResponses(config);

      cy.intercept('POST', config.importPattern, {
        statusCode: 200,
        body: {
          status: 200,
          message: config.importSuccessMessage,
          data: {
            module: config.label,
            totalRows: config.importedRows.length,
            successCount: config.importedRows.length,
            failureCount: 0,
            success: true,
            partialSuccess: false,
            errors: [],
            errorDetails: [],
            errorReportToken: null,
            errorReportFilename: null,
            message: config.importSuccessMessage
          }
        }
      }).as(`${config.key}Import`);

      visitAdminPage(config.page);
      cy.wait(`@${config.key}List`);

      cy.get(config.importInput).selectFile(
        {
          contents: Cypress.Buffer.from('fake-excel-import-content'),
          fileName: `${config.key}-import.xlsx`,
          mimeType: excelMimeType
        },
        { force: true }
      );

      cy.wait(`@${config.key}Import`);
      cy.contains('.admin-toast__message', config.importSuccessMessage).should('be.visible');
      cy.contains('.admin-modal-title', `Kết quả nhập Excel ${config.label}`).should('be.visible');
      cy.contains('.admin-summary-card__value', String(config.importedRows.length)).should('be.visible');
      cy.wait(`@${config.key}List`);
      cy.contains(config.importedAssertion).should('be.visible');
    });
  });

  it('TC-ADM-LIST-001 - Mỗi trang hiển thị 50 bản ghi ở các danh sách chính', () => {
    const listConfigs = [
      { page: '/admin/donors', pattern: '/api/donors?*', alias: 'donorsList' },
      { page: '/admin/donations', pattern: '/api/donations/list?*', alias: 'donationsList' },
      { page: '/admin/events', pattern: '/api/events?*', alias: 'eventsList' },
      { page: '/admin/activities', pattern: '/api/activities?*', alias: 'activitiesList' },
      { page: '/admin/transactions', pattern: '/api/transactions?*', alias: 'transactionsList' }
    ];

    listConfigs.forEach((config, index) => {
      cy.intercept('GET', config.pattern, {
        statusCode: 200,
        body: pagedResponse([], {
          pageSize: 50,
          totalItems: 0
        })
      }).as(config.alias);

      visitAdminPage(config.page);
      cy.wait(`@${config.alias}`).its('request.query.size').should('eq', '50');

      if (index < listConfigs.length - 1) {
        cy.clearCookies();
      }
    });
  });
});
