import {
  selectors,
  visitDonationPage,
  stubAlert,
  fillIndividualDonor,
  fillOrganizationDonor,
  fillCommonDonation
} from './helpers/donorTestUtils.js';

describe('Donor Donation Page', () => {
  it('loads /donations and shows the expected default state', () => {
    visitDonationPage();

    cy.get(selectors.donorTypeIndividual).should('be.checked');
    cy.get(selectors.individualSection).should('be.visible');
    cy.get(selectors.organizationSection).should('have.class', 'hidden');
    cy.get(selectors.receiptFields).should('have.class', 'hidden');
  });

  it('switches correctly between individual and organization donor forms', () => {
    visitDonationPage();

    cy.get(selectors.donorTypeOrganization).check({ force: true });
    cy.get(selectors.organizationSection).should('be.visible');
    cy.get(selectors.individualSection).should('have.class', 'hidden');

    cy.get(selectors.donorTypeIndividual).check({ force: true });
    cy.get(selectors.individualSection).should('be.visible');
    cy.get(selectors.organizationSection).should('have.class', 'hidden');
  });

  it('shows receipt fields and auto-fills receipt email from donor email', () => {
    visitDonationPage();

    cy.get(selectors.emailInput).type('donor01@test.com');
    cy.get(selectors.receiptCheckbox).check({ force: true });

    cy.get(selectors.receiptFields).should('be.visible');
    cy.get(selectors.receiptEmailInput).should('have.value', 'donor01@test.com');
  });

  it('blocks submission when amount is below minimum', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor();
    fillCommonDonation({ amount: '999' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Số tiền phải từ 1.000 đồng đến tối đa 10.000.000 đồng');
  });

  it('blocks submission when amount is above the configured frontend maximum', () => {
    visitDonationPage();
    stubAlert();

    fillIndividualDonor();
    fillCommonDonation({ amount: '10000001' });
    cy.get(selectors.submitButton).click();

    cy.get('@alert').should('have.been.calledWith', 'Số tiền phải từ 1.000 đồng đến tối đa 10.000.000 đồng');
  });

  it('submits a successful individual donation flow with stubbed APIs', () => {
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

  it('submits a successful organization donation flow with stubbed APIs', () => {
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

  it('shows a friendly alert when donor API returns duplicate email', () => {
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

  it('shows a friendly alert when donation creation fails', () => {
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

  it('submits receipt information when donor requests a receipt', () => {
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
