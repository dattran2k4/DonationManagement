import {
  selectors,
  visitDonationPage,
  stubAlert,
  fillIndividualDonor,
  fillOrganizationDonor,
  fillCommonDonation
} from './helpers/donorTestUtils.js';

describe('Donor Donation Page Remaining Cases', () => {
  it('stays publicly accessible without redirecting to login', () => {
    visitDonationPage();

    cy.location('pathname').should('eq', '/donations');
    cy.contains('Thông tin quyên góp').should('be.visible');
  });

  it('shows a validation alert when individual full name is blank', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor({
      fullName: ' ',
      displayName: 'Anh A',
      phone: '0912345678',
      email: 'a01@test.com'
    });
    fillCommonDonation();
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Họ và tên không được để trống');
  });

  it('shows a validation alert when phone number is invalid', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor({
      fullName: 'Nguyen Van A',
      displayName: 'Anh A',
      phone: '09AB123',
      email: 'a01@test.com'
    });
    fillCommonDonation();
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Số điện thoại không hợp lệ');
  });

  it('shows a backend validation alert when donor email is invalid', () => {
    cy.intercept('POST', '/api/donors/individuals', {
      statusCode: 400,
      body: {
        status: 400,
        error: 'Invalid Payload',
        message: 'Email không hợp lệ'
      }
    }).as('saveIndividual');

    visitDonationPage();
    stubAlert();

    fillIndividualDonor({
      fullName: 'Nguyen Van A',
      displayName: 'Anh A',
      phone: '0912345678',
      email: 'abc@'
    });
    fillCommonDonation();
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.get('@alert').should('have.been.calledWith', 'Email không hợp lệ');
  });

  it('blocks decimal amounts and tells the donor to enter a whole amount', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor();
    fillCommonDonation({ amount: '1000.5' });
    cy.get(selectors.amountInput).blur();

    cy.get('@alert').should('have.been.calledWith', 'Chỗ này chưa code huhu, vui lòng nhập tiền chẳn');
    cy.get(selectors.amountInput).should('have.value', '');
  });

  it('shows a validation alert when organization name is blank', () => {
    visitDonationPage();
    stubAlert();

    fillOrganizationDonor({
      name: ' ',
      taxCode: '0101234567',
      representative: 'Nguyen Van B',
      phone: '0901234567',
      email: 'contact@abc.com'
    });
    fillCommonDonation({ amount: '1000000' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Tên tổ chức không được để trống');
  });

  it('shows a validation alert when tax code is blank', () => {
    visitDonationPage();
    stubAlert();

    fillOrganizationDonor({
      name: 'Cong ty ABC',
      taxCode: ' ',
      representative: 'Nguyen Van B',
      phone: '0901234567',
      email: 'contact@abc.com'
    });
    fillCommonDonation({ amount: '1000000' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Mã số thuế không được để trống');
  });

  it('shows a validation alert when representative is blank', () => {
    visitDonationPage();
    stubAlert();

    fillOrganizationDonor({
      name: 'Cong ty ABC',
      taxCode: '0101234567',
      representative: ' ',
      phone: '0901234567',
      email: 'contact@abc.com'
    });
    fillCommonDonation({ amount: '1000000' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Người đại diện không được để trống');
  });

  it('falls back display name to full name and submits target NONE payload', () => {
    cy.intercept('POST', '/api/donors/individuals', (req) => {
      expect(req.body).to.include({
        fullName: 'Tran Thi B',
        displayName: 'Tran Thi B',
        phone: '0901234567',
        email: 'b01@test.com'
      });
      req.reply({ statusCode: 200, body: { status: 200, data: 505 } });
    }).as('saveIndividual');

    cy.intercept('POST', '/api/donations/donor-create', (req) => {
      expect(req.body).to.include({
        donorId: 505,
        amount: 650000,
        needReceipt: false,
        paymentMethod: 'BANK_TRANSFER_ONLINE'
      });
      expect(req.body.eventId).to.equal(null);
      expect(req.body.activityId).to.equal(null);
      req.reply({ statusCode: 200, body: { status: 200, data: 'THN2103NHT' } });
    }).as('createDonation');

    cy.intercept('POST', '/api/payments', {
      statusCode: 201,
      body: { status: 201, data: `${Cypress.config('baseUrl')}/donations?payment=display-name-fallback` }
    }).as('createPayment');

    visitDonationPage();
    fillIndividualDonor({
      fullName: 'Tran Thi B',
      displayName: ' ',
      phone: '0901234567',
      email: 'b01@test.com'
    });
    fillCommonDonation({ amount: '650000' });
    cy.get(selectors.receiptCheckbox).should('not.be.checked');
    cy.get(selectors.receiptEmailInput).should('have.value', '');
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.wait('@createDonation');
    cy.wait('@createPayment');
    cy.location('search').should('include', 'payment=display-name-fallback');
  });
});
