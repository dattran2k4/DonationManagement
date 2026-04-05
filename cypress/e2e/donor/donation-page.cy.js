import {
  selectors,
  visitDonationPage,
  stubAlert,
  fillIndividualDonor,
  fillOrganizationDonor,
  fillCommonDonation
} from './helpers/donorTestUtils.js';

describe('Trang quyên góp nhà hảo tâm', () => {
  it('TC-NHT-DON-001 - Mở trang quyên góp', () => {
    visitDonationPage();

    cy.get(selectors.donorTypeIndividual).should('be.checked');
    cy.get(selectors.individualSection).should('be.visible');
    cy.get(selectors.organizationSection).should('have.class', 'hidden');
    cy.get(selectors.receiptFields).should('have.class', 'hidden');
  });

  it('TC-NHT-DON-002 + TC-NHT-DON-003 - Mặc định tab nhà hảo tâm là Cá nhân và cho phép chuyển đổi giữa Cá nhân, Tổ chức', () => {
    visitDonationPage();

    cy.get(selectors.donorTypeOrganization).check({ force: true });
    cy.get(selectors.organizationSection).should('be.visible');
    cy.get(selectors.individualSection).should('have.class', 'hidden');

    cy.get(selectors.donorTypeIndividual).check({ force: true });
    cy.get(selectors.individualSection).should('be.visible');
    cy.get(selectors.organizationSection).should('have.class', 'hidden');
  });

  it('TC-NHT-DON-004 + TC-NHT-DON-005 - Hiển thị thông tin biên lai và tự động điền email biên lai', () => {
    visitDonationPage();

    cy.get(selectors.emailInput).type('donor01@test.com');
    cy.get(selectors.receiptCheckbox).check({ force: true });

    cy.get(selectors.receiptFields).should('be.visible');
    cy.get(selectors.receiptEmailInput).should('have.value', 'donor01@test.com');
  });

  it('TC-NHT-DON-009 - Số tiền nhỏ hơn mức tối thiểu', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor();
    fillCommonDonation({ amount: '999' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Số tiền phải từ 1.000 đồng đến tối đa 10.000.000 đồng');
  });

  it('TC-NHT-DON-010 - Số tiền vượt giới hạn giao diện', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor();
    fillCommonDonation({ amount: '10000001' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Số tiền phải từ 1.000 đồng đến tối đa 10.000.000 đồng');
  });

  it('TC-NHT-DON-006 - Quyên góp cá nhân thành công', () => {
    cy.intercept('POST', '/api/donors/individuals', (req) => {
      expect(req.body).to.include({
        fullName: 'Nguyen Van A',
        displayName: 'Anh A',
        phone: '0912345678',
        email: 'a01@test.com'
      });
      req.reply({ statusCode: 200, body: { status: 200, data: 101 } });
    }).as('saveIndividual');

    cy.intercept('POST', '/api/donations/donor-create', (req) => {
      expect(req.body).to.include({
        donorId: 101,
        amount: 500000,
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        needReceipt: false
      });
      req.reply({ statusCode: 200, body: { status: 200, data: 'THN123ABC' } });
    }).as('createDonation');

    cy.intercept('POST', '/api/payments', (req) => {
      expect(req.body).to.deep.equal({ donationMemoCode: 'THN123ABC' });
      req.reply({
        statusCode: 201,
        body: { status: 201, data: `${Cypress.config('baseUrl')}/donations?payment=success` }
      });
    }).as('createPayment');

    visitDonationPage();
    fillIndividualDonor();
    fillCommonDonation();
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.wait('@createDonation');
    cy.wait('@createPayment');
    cy.location('search').should('include', 'payment=success');
  });

  it('TC-NHT-DON-007 - Quyên góp tổ chức thành công', () => {
    cy.intercept('POST', '/api/donors/organizations', (req) => {
      expect(req.body).to.include({
        name: 'Cong ty ABC',
        taxCode: '0101234567',
        representative: 'Nguyen Van B',
        phone: '0901234567',
        email: 'contact@abc.com'
      });
      req.reply({ statusCode: 200, body: { status: 200, data: 202 } });
    }).as('saveOrganization');

    cy.intercept('POST', '/api/donations/donor-create', (req) => {
      expect(req.body).to.include({
        donorId: 202,
        amount: 1000000,
        paymentMethod: 'BANK_TRANSFER_ONLINE'
      });
      req.reply({ statusCode: 200, body: { status: 200, data: 'THN999XYZ' } });
    }).as('createDonation');

    cy.intercept('POST', '/api/payments', {
      statusCode: 201,
      body: { status: 201, data: `${Cypress.config('baseUrl')}/donations?payment=org-success` }
    }).as('createPayment');

    visitDonationPage();
    fillOrganizationDonor();
    fillCommonDonation({ amount: '1000000' });
    cy.get(selectors.submitButton).click();

    cy.wait('@saveOrganization');
    cy.wait('@createDonation');
    cy.wait('@createPayment');
    cy.location('search').should('include', 'payment=org-success');
  });

  it('TC-NHT-DON-019 - Dữ liệu nhà hảo tâm bị trùng', () => {
    cy.intercept('POST', '/api/donors/individuals', {
      statusCode: 409,
      body: {
        status: 409,
        error: 'Conflict',
        message: 'Email nhà hảo tâm đã tồn tại'
      }
    }).as('saveIndividual');

    visitDonationPage();
    stubAlert();
    fillIndividualDonor();
    fillCommonDonation();
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.get('@alert').should('have.been.calledWith', 'Email nhà hảo tâm đã tồn tại');
  });

  it('AUTO-NHT-DON-001 - Hiển thị thông báo thân thiện khi tạo quyên góp lỗi', () => {
    cy.intercept('POST', '/api/donors/individuals', {
      statusCode: 200,
      body: { status: 200, data: 303 }
    }).as('saveIndividual');

    cy.intercept('POST', '/api/donations/donor-create', {
      statusCode: 400,
      body: {
        status: 400,
        error: 'Bad Request',
        message: 'Số tiền không hợp lệ'
      }
    }).as('createDonation');

    visitDonationPage();
    stubAlert();
    fillIndividualDonor({
      fullName: 'Tran Thi B',
      displayName: 'Chi B',
      phone: '0988888888',
      email: 'b01@test.com'
    });
    fillCommonDonation({ amount: '500000' });
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Số tiền không hợp lệ');
  });

  it('AUTO-NHT-DON-002 - Gửi đúng thông tin biên lai khi nhà hảo tâm yêu cầu', () => {
    cy.intercept('POST', '/api/donors/individuals', {
      statusCode: 200,
      body: { status: 200, data: 404 }
    }).as('saveIndividual');

    cy.intercept('POST', '/api/donations/donor-create', (req) => {
      expect(req.body).to.include({
        donorId: 404,
        needReceipt: true,
        receiptName: 'Nguyen Van A',
        receiptEmail: 'receipt@test.com'
      });
      req.reply({ statusCode: 200, body: { status: 200, data: 'THNREC001' } });
    }).as('createDonation');

    cy.intercept('POST', '/api/payments', {
      statusCode: 201,
      body: { status: 201, data: `${Cypress.config('baseUrl')}/donations?payment=receipt-success` }
    }).as('createPayment');

    visitDonationPage();
    fillIndividualDonor();
    fillCommonDonation();
    cy.get(selectors.receiptCheckbox).check({ force: true });
    cy.get('input[name="receiptName"]').clear().type('Nguyen Van A');
    cy.get(selectors.receiptEmailInput).clear().type('receipt@test.com');
    cy.get(selectors.submitButton).click();

    cy.wait('@saveIndividual');
    cy.wait('@createDonation');
    cy.wait('@createPayment');
    cy.location('search').should('include', 'payment=receipt-success');
  });
});
