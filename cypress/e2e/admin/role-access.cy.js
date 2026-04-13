import {
  backofficeUsers,
  visitBackofficePage
} from './helpers/adminTestUtils.js';

describe('Phân quyền và ma trận truy cập', () => {
  it('TC-ADM-ROLE-001 - Vai trò ADMIN có đầy đủ quyền theo ma trận phân quyền', () => {
    visitBackofficePage('/admin/donors', 'admin');
    cy.contains(backofficeUsers.admin.roleLabel).should('be.visible');
    cy.get('#donorImportBtn').should('be.visible');
    cy.contains('a', 'Thêm Nhà hảo tâm').should('be.visible');

    visitBackofficePage('/admin/donations', 'admin');
    cy.get('#donationImportBtn').should('be.visible');
    cy.contains('a', 'Tạo mới quyên góp').should('be.visible');

    visitBackofficePage('/admin/events', 'admin');
    cy.get('#eventImportBtn').should('be.visible');
    cy.contains('a', 'Thêm Sự kiện mới').should('be.visible');

    visitBackofficePage('/admin/activities', 'admin');
    cy.get('#activityImportBtn').should('be.visible');
    cy.contains('a', 'Thêm Hoạt động mới').should('be.visible');

    visitBackofficePage('/admin/transactions', 'admin');
    cy.get('#transactionImportBtn').should('be.visible');

    visitBackofficePage('/admin/settings', 'admin');
    cy.contains('button', /^Lưu$/).should('be.visible');
  });

  it('TC-ADM-ROLE-002 - Vai trò Kế toán (ACCOUNTING) chỉ thấy và dùng được đúng màn hình, thao tác được cấp quyền', () => {
    visitBackofficePage('/admin/donors', 'accounting');
    cy.contains(backofficeUsers.accounting.roleLabel).should('be.visible');
    cy.get('#donorImportBtn').should('not.exist');
    cy.contains('a', 'Thêm Nhà hảo tâm').should('not.exist');

    visitBackofficePage('/admin/donations', 'accounting');
    cy.get('#donationImportBtn').should('not.exist');
    cy.contains('a', 'Tạo mới quyên góp').should('not.exist');

    visitBackofficePage('/admin/events', 'accounting');
    cy.get('#eventImportBtn').should('be.visible');
    cy.contains('a', 'Thêm Sự kiện mới').should('be.visible');

    visitBackofficePage('/admin/activities', 'accounting');
    cy.get('#activityImportBtn').should('be.visible');
    cy.contains('a', 'Thêm Hoạt động mới').should('be.visible');

    visitBackofficePage('/admin/transactions', 'accounting');
    cy.get('#transactionImportBtn').should('be.visible');

    visitBackofficePage('/admin/settings', 'accounting');
    cy.contains('Bạn đang ở chế độ chỉ xem').should('be.visible');
    cy.contains('button', /^Lưu$/).should('not.exist');
  });

  it('TC-ADM-ROLE-003 - Vai trò Nhân viên (STAFF) chỉ thấy và dùng được đúng màn hình, thao tác được cấp quyền', () => {
    visitBackofficePage('/admin/donors', 'staff');
    cy.contains(backofficeUsers.staff.roleLabel).should('be.visible');
    cy.get('#donorImportBtn').should('be.visible');
    cy.contains('a', 'Thêm Nhà hảo tâm').should('be.visible');

    visitBackofficePage('/admin/donations', 'staff');
    cy.get('#donationImportBtn').should('be.visible');
    cy.contains('a', 'Tạo mới quyên góp').should('be.visible');

    visitBackofficePage('/admin/events', 'staff');
    cy.get('#eventImportBtn').should('not.exist');
    cy.contains('a', 'Thêm Sự kiện mới').should('not.exist');

    visitBackofficePage('/admin/activities', 'staff');
    cy.get('#activityImportBtn').should('not.exist');
    cy.contains('a', 'Thêm Hoạt động mới').should('not.exist');

    visitBackofficePage('/admin/transactions', 'staff');
    cy.get('#transactionImportBtn').should('not.exist');

    visitBackofficePage('/admin/settings', 'staff');
    cy.contains('Bạn đang ở chế độ chỉ xem').should('be.visible');
    cy.contains('button', /^Lưu$/).should('not.exist');
  });

  it('TC-ADM-ROLE-004 - Đồng bộ giao diện và máy chủ khi kiểm soát quyền', () => {
    visitBackofficePage('/admin/donors', 'admin');
    cy.request('/admin/donors/form').its('status').should('eq', 200);

    visitBackofficePage('/admin/donors', 'accounting');
    cy.contains('a', 'Thêm Nhà hảo tâm').should('not.exist');
    cy.request({
      method: 'GET',
      url: '/admin/donors/form',
      failOnStatusCode: false
    }).its('status').should('eq', 403);

    visitBackofficePage('/admin/events', 'staff');
    cy.contains('a', 'Thêm Sự kiện mới').should('not.exist');
    cy.request({
      method: 'GET',
      url: '/admin/events/form',
      failOnStatusCode: false
    }).its('status').should('eq', 403);
  });
});
