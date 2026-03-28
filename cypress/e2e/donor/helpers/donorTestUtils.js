export const pages = {
  donation: '/donations',
  event: '/events/gay-quy-mo-tim-be-an',
  activity: '/activities/dot-1-chi-phi-phau-thuat'
};

export const selectors = {
  donorTypeIndividual: '#donor_personal',
  donorTypeOrganization: '#donor_org',
  individualSection: '#individual-section',
  organizationSection: '#organization-section',
  receiptCheckbox: '#needReceipt',
  receiptFields: '#receipt-fields',
  submitButton: '#submitDonation',
  amountInput: '#donationAmount',
  emailInput: '#email',
  receiptEmailInput: '#receiptEmail'
};

export const orders = {
  none: {
    code: '932603210199',
    donorName: 'Nhà hảo tâm mẫu 199',
    phone: '0913000199',
    email: 'donor.seed.199@demo.local',
    message: 'Bản ghi quyên góp mẫu 199',
    label: 'CLB Chia sẻ yêu thương'
  },
  event: {
    code: '932603210192',
    donorName: 'Tổ chức mẫu 192',
    phone: '0913000192',
    email: 'org.seed.192@demo.local',
    message: 'Bản ghi quyên góp mẫu 192',
    label: 'Chiến dịch',
    name: 'Gây quỹ mổ tim cho bé An'
  },
  activity: {
    code: '932603210185',
    donorName: 'Nhà hảo tâm mẫu 185',
    phone: '0913000185',
    email: 'donor.seed.185@demo.local',
    message: 'Bản ghi quyên góp mẫu 185',
    label: 'Hoạt động',
    name: 'Đợt 2 - Hậu phẫu & phục hồi'
  }
};

export function visitDonationPage() {
  cy.visit(pages.donation);
  cy.contains('Thông tin quyên góp').should('be.visible');
  cy.contains('Quyên góp ngay').should('be.visible');
}

export function visitSuccess(orderCode) {
  cy.visit(`/thanh-toan/thanh-cong?orderCode=${orderCode}`);
  cy.contains('Cảm ơn bạn đã đồng hành cùng chúng tôi!').should('be.visible');
}

export function stubAlert() {
  cy.on('window:alert', cy.stub().as('alert'));
}

export function fillIndividualDonor({
  fullName = 'Nguyen Van A',
  displayName = 'Anh A',
  phone = '0912345678',
  email = 'a01@test.com',
  note = 'Ung ho chuong trinh',
  force = false
} = {}) {
  cy.get(selectors.donorTypeIndividual).check({ force: true });
  cy.get('input[name="fullName"]').scrollIntoView().clear({ force }).type(fullName, { force });
  cy.get('input[name="displayName"]').scrollIntoView().clear({ force }).type(displayName, { force });
  cy.get('input[name="phone"]').scrollIntoView().clear({ force }).type(phone, { force });
  cy.get(selectors.emailInput).scrollIntoView().clear({ force }).type(email, { force });
  cy.get('textarea[name="note"]').scrollIntoView().clear({ force }).type(note, { force });
}

export function fillOrganizationDonor({
  name = 'Cong ty ABC',
  taxCode = '0101234567',
  representative = 'Nguyen Van B',
  phone = '0901234567',
  email = 'contact@abc.com',
  billingAddress = '1 Nguyen Hue, Q1',
  force = false
} = {}) {
  cy.get(selectors.donorTypeOrganization).check({ force: true });
  cy.get('input[name="name"]').scrollIntoView().clear({ force }).type(name, { force });
  cy.get('input[name="taxCode"]').scrollIntoView().clear({ force }).type(taxCode, { force });
  cy.get('input[name="representative"]').scrollIntoView().clear({ force }).type(representative, { force });
  cy.get('input[name="billingAddress"]').scrollIntoView().clear({ force }).type(billingAddress, { force });
  cy.get('input[name="phone"]').scrollIntoView().clear({ force }).type(phone, { force });
  cy.get(selectors.emailInput).scrollIntoView().clear({ force }).type(email, { force });
}

export function fillCommonDonation({ amount = '500000', force = false } = {}) {
  cy.get(selectors.amountInput).scrollIntoView().clear({ force }).type(amount, { force });
}
