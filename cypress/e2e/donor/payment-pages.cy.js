import { orders, visitSuccess } from './helpers/donorTestUtils.js';

describe('Trang kết quả thanh toán', () => {
  it('TC-NHT-PAY-004 - Hiển thị giá trị mặc định cho khoản quyên góp không gắn sự kiện hoặc hoạt động', () => {
    visitSuccess(orders.none.code);

    cy.contains(orders.none.label).should('be.visible');
    cy.contains(orders.none.donorName).should('be.visible');
    cy.contains(orders.none.phone).should('be.visible');
    cy.contains(orders.none.email).should('be.visible');
    cy.contains(orders.none.message).should('be.visible');
    cy.contains('Xem biên lai / Hóa đơn').should('be.visible');
  });

  it('TC-NHT-EVT-003 - Trang thành công hiển thị đúng tên sự kiện', () => {
    visitSuccess(orders.event.code);

    cy.contains(orders.event.label).should('be.visible');
    cy.contains(orders.event.name).should('be.visible');
    cy.contains(orders.event.donorName).should('be.visible');
    cy.contains(orders.event.phone).should('be.visible');
    cy.contains(orders.event.email).should('be.visible');
    cy.contains(orders.event.message).should('be.visible');
  });

  it('TC-NHT-ACT-003 - Trang thành công hiển thị đúng tên hoạt động', () => {
    visitSuccess(orders.activity.code);

    cy.contains(orders.activity.label).should('be.visible');
    cy.contains(orders.activity.name).should('be.visible');
    cy.contains(orders.activity.donorName).should('be.visible');
    cy.contains(orders.activity.phone).should('be.visible');
    cy.contains(orders.activity.email).should('be.visible');
    cy.contains(orders.activity.message).should('be.visible');
  });

  it('TC-NHT-PAY-007 - Nút Quay lại trang chủ hoạt động đúng', () => {
    visitSuccess(orders.none.code);

    cy.contains('a', 'Quay lại trang chủ').click();
    cy.location('pathname').should('eq', '/');
    cy.contains('Cùng nhau, chúng ta').should('be.visible');
  });

  it('TC-NHT-PAY-006 - Điều hướng trang thất bại khi hủy thanh toán', () => {
    cy.visit('/thanh-toan/that-bai');

    cy.location('pathname').should('eq', '/thanh-toan/that-bai');
    cy.contains('Cùng nhau, chúng ta').should('be.visible');
    cy.contains('Quyên góp').should('be.visible');
  });

  it('TC-NHT-PAY-005 - orderCode không tồn tại', () => {
    cy.request({
      url: '/thanh-toan/thanh-cong?orderCode=999999999',
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.not.equal(500);
    });
  });
});
