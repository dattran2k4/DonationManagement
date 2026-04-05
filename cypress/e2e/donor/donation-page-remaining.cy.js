import {
  selectors,
  visitDonationPage,
  stubAlert,
  fillIndividualDonor,
  fillOrganizationDonor,
  fillCommonDonation
} from './helpers/donorTestUtils.js';

describe('Trang quyên góp nhà hảo tâm - Bổ sung', () => {
  it('TC-NHT-DON-008 - Nhà hảo tâm không cần đăng nhập vẫn quyên góp được', () => {
    visitDonationPage();

    cy.location('pathname').should('eq', '/donations');
    cy.contains('Thông tin quyên góp').should('be.visible');
  });

  it('TC-NHT-DON-011 - Thiếu họ và tên nhà hảo tâm cá nhân', () => {
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

  it('TC-NHT-DON-012 - Số điện thoại không hợp lệ', () => {
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

  it('TC-NHT-DON-013 - Email không hợp lệ', () => {
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

  it('TC-NHT-DON-014 - Số tiền thập phân không hợp lệ', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor();
    fillCommonDonation({ amount: '1000.5' });
    cy.get(selectors.amountInput).blur();

    cy.get('@alert').should('have.been.calledWith', 'Chỗ này chưa code huhu, vui lòng nhập tiền chẳn');
    cy.get(selectors.amountInput).should('have.value', '');
  });

  it('TC-NHT-DON-015 - Thiếu tên tổ chức', () => {
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

  it('TC-NHT-DON-016 - Thiếu mã số thuế', () => {
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

  it('TC-NHT-DON-017 - Thiếu người đại diện', () => {
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

  it('TC-NHT-DON-018 - Tên hiển thị tự lấy theo họ và tên khi để trống', () => {
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
