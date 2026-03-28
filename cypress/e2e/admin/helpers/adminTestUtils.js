export const adminCredentials = {
  username: 'admin',
  password: '123456'
};

export function pagedResponse(items, overrides = {}) {
  return {
    status: 200,
    message: 'OK',
    data: {
      page: overrides.page ?? 1,
      pageSize: overrides.pageSize ?? 10,
      totalPages: overrides.totalPages ?? 1,
      totalItems: overrides.totalItems ?? items.length,
      data: items
    }
  };
}

export function loginAsAdmin() {
  cy.session('admin-session', () => {
    cy.visit('/login');
    cy.contains('Đăng nhập quản trị').should('be.visible');
    cy.get('#username').clear().type(adminCredentials.username);
    cy.get('#password').clear().type(adminCredentials.password, { log: false });
    cy.contains('button', 'Đăng nhập hệ thống').click();
    cy.location('pathname', { timeout: 20000 }).should('eq', '/admin/dashboard');
    cy.contains('Quản trị viên').should('be.visible');
  });
}

export function visitAdminPage(path) {
  loginAsAdmin();
  cy.visit(path);
}

export function stubAlert() {
  const alertStub = cy.stub();
  cy.on('window:alert', alertStub);
  return cy.wrap(alertStub).as('alert');
}

export function stubConfirm(accepted = true) {
  const confirmStub = cy.stub().returns(accepted);
  cy.on('window:confirm', confirmStub);
  return cy.wrap(confirmStub).as('confirm');
}
