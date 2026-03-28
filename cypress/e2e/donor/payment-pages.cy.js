import { orders, visitSuccess } from './helpers/donorTestUtils.js';

describe('Payment Result Pages', () => {
  it('shows the fallback organization label for a general donation success page', () => {
    visitSuccess(orders.none.code);

    cy.contains(orders.none.label).should('be.visible');
    cy.contains(orders.none.donorName).should('be.visible');
    cy.contains(orders.none.phone).should('be.visible');
    cy.contains(orders.none.email).should('be.visible');
    cy.contains(orders.none.message).should('be.visible');
    cy.contains('Xem biên lai / Hóa đơn').should('be.visible');
  });

  it('shows the linked event information on a successful event donation', () => {
    visitSuccess(orders.event.code);

    cy.contains(orders.event.label).should('be.visible');
    cy.contains(orders.event.name).should('be.visible');
    cy.contains(orders.event.donorName).should('be.visible');
    cy.contains(orders.event.phone).should('be.visible');
    cy.contains(orders.event.email).should('be.visible');
    cy.contains(orders.event.message).should('be.visible');
  });

  it('shows the linked activity information on a successful activity donation', () => {
    visitSuccess(orders.activity.code);

    cy.contains(orders.activity.label).should('be.visible');
    cy.contains(orders.activity.name).should('be.visible');
    cy.contains(orders.activity.donorName).should('be.visible');
    cy.contains(orders.activity.phone).should('be.visible');
    cy.contains(orders.activity.email).should('be.visible');
    cy.contains(orders.activity.message).should('be.visible');
  });

  it('allows the donor to return to the home page from the success page', () => {
    visitSuccess(orders.none.code);

    cy.contains('a', 'Quay lại trang chủ').click();
    cy.location('pathname').should('eq', '/');
    cy.contains('Cùng nhau, chúng ta').should('be.visible');
  });

  it('renders the home page content on the failed payment route', () => {
    cy.visit('/thanh-toan/that-bai');

    cy.location('pathname').should('eq', '/thanh-toan/that-bai');
    cy.contains('Cùng nhau, chúng ta').should('be.visible');
    cy.contains('Quyên góp').should('be.visible');
  });

  it('handles an invalid order code without returning a server error', () => {
    cy.request({
      url: '/thanh-toan/thanh-cong?orderCode=999999999',
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.not.equal(500);
    });
  });
});
