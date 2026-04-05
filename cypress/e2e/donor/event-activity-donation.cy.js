import {
  pages,
  selectors,
  fillIndividualDonor,
  fillOrganizationDonor,
  fillCommonDonation
} from './helpers/donorTestUtils.js';

describe('Quyên góp từ trang sự kiện và hoạt động', () => {
  it('TC-NHT-EVT-001 - Hiển thị form quyên góp gắn mã sự kiện', () => {
    cy.visit(pages.event);

    cy.contains('Gây quỹ mổ tim cho bé An').should('be.visible');
    cy.get('form#donationForm').should('be.visible');
    cy.get('input[name="eventId"]').should('have.value', '1');
    cy.get('input[name="activityId"]').should('not.exist');
  });

  it('TC-NHT-EVT-002 - Quyên góp thành công từ trang sự kiện', () => {
    let expectedEventId = null;

    cy.intercept('POST', '/api/donors/individuals', {
      statusCode: 200,
      body: { status: 200, data: 601 }
    }).as('saveIndividual');

    cy.intercept('POST', '/api/donations/donor-create', (req) => {
      expect(req.body).to.include({
        donorId: 601,
        amount: 300000,
        paymentMethod: 'BANK_TRANSFER_ONLINE'
      });
      expect(req.body.eventId).to.equal(expectedEventId);
      expect(req.body.activityId).to.equal(null);
      req.reply({ statusCode: 200, body: { status: 200, data: 'EVT2103ABC' } });
    }).as('createDonation');

    cy.intercept('POST', '/api/payments', {
      statusCode: 201,
      body: { status: 201, data: `${Cypress.config('baseUrl')}/donations?payment=event-success` }
    }).as('createPayment');

    cy.visit(pages.event);
    cy.get('input[name="eventId"]').invoke('val').then((value) => {
      expectedEventId = Number(value);
    });
    fillIndividualDonor({ force: true });
    fillCommonDonation({ amount: '300000', force: true });
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.wait('@createDonation');
    cy.wait('@createPayment');
    cy.location('search').should('include', 'payment=event-success');
  });

  it('TC-NHT-ACT-001 - Hiển thị form quyên góp gắn mã hoạt động', () => {
    cy.visit(pages.activity);

    cy.contains('Đợt 1 - Chi phí phẫu thuật').should('be.visible');
    cy.get('form#donationForm').should('be.visible');
    cy.get('input[name="activityId"]').should('have.value', '1');
    cy.get('input[name="eventId"]').should('not.exist');
  });

  it('TC-NHT-ACT-002 - Quyên góp thành công từ trang hoạt động', () => {
    let expectedActivityId = null;

    cy.intercept('POST', '/api/donors/organizations', {
      statusCode: 200,
      body: { status: 200, data: 602 }
    }).as('saveOrganization');

    cy.intercept('POST', '/api/donations/donor-create', (req) => {
      expect(req.body).to.include({
        donorId: 602,
        amount: 450000,
        paymentMethod: 'BANK_TRANSFER_ONLINE'
      });
      expect(req.body.activityId).to.equal(expectedActivityId);
      expect(req.body.eventId).to.equal(null);
      req.reply({ statusCode: 200, body: { status: 200, data: 'ACT2103XYZ' } });
    }).as('createDonation');

    cy.intercept('POST', '/api/payments', {
      statusCode: 201,
      body: { status: 201, data: `${Cypress.config('baseUrl')}/donations?payment=activity-success` }
    }).as('createPayment');

    cy.visit(pages.activity);
    cy.get('input[name="activityId"]').invoke('val').then((value) => {
      expectedActivityId = Number(value);
    });
    fillOrganizationDonor({ force: true });
    fillCommonDonation({ amount: '450000', force: true });
    cy.get(selectors.submitButton).click();

    cy.wait('@saveOrganization');
    cy.wait('@createDonation');
    cy.wait('@createPayment');
    cy.location('search').should('include', 'payment=activity-success');
  });
});
