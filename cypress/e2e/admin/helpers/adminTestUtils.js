export const adminCredentials = {
  username: 'admin',
  password: '123456'
};

export const backofficeUsers = {
  admin: {
    username: 'admin',
    password: '123456',
    roleLabel: 'Quản trị viên'
  },
  staff: {
    username: 'thuylinh',
    password: '123456',
    roleLabel: 'Nhân viên vận hành'
  },
  accounting: {
    username: 'camtu',
    password: '123456',
    roleLabel: 'Kế toán'
  }
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
  loginAsBackoffice('admin');
}

export function loginAsBackoffice(userKey = 'admin') {
  const user = backofficeUsers[userKey];

  if (!user) {
    throw new Error(`Unsupported back-office user: ${userKey}`);
  }

  cy.session(`backoffice-${userKey}`, () => {
    cy.visit('/login');
    cy.contains('Đăng nhập quản trị').should('be.visible');
    cy.get('#username').clear().type(user.username);
    cy.get('#password').clear().type(user.password, { log: false });
    cy.contains('button', 'Đăng nhập hệ thống').click();
    cy.location('pathname', { timeout: 20000 }).should('eq', '/admin/dashboard');
    cy.contains(user.roleLabel).should('be.visible');
  });
}

export function visitAdminPage(path) {
  visitBackofficePage(path, 'admin');
}

export function visitBackofficePage(path, userKey = 'admin') {
  loginAsBackoffice(userKey);
  cy.visit(path);
}

export function logoutIfLoggedIn() {
  cy.clearCookies();
  cy.clearAllLocalStorage();
  cy.clearAllSessionStorage();
}

export function stubDownloadApis() {
  cy.window().then((win) => {
    cy.stub(win.URL, 'createObjectURL').returns('blob:cypress-download');
    cy.stub(win.URL, 'revokeObjectURL').as('revokeObjectURL');
    cy.stub(win.HTMLAnchorElement.prototype, 'click').callsFake(() => {});
  });
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
