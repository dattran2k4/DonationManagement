import {
  backofficeUsers,
  logoutIfLoggedIn,
  visitBackofficePage
} from './helpers/adminTestUtils.js';

describe('Đăng nhập và thanh điều hướng quản trị', () => {
  it('TC-ADM-LOGIN-001 - Đăng nhập thành công bằng tên tài khoản hợp lệ', () => {
    logoutIfLoggedIn();

    cy.visit('/login');
    cy.get('#username').clear().type(backofficeUsers.admin.username);
    cy.get('#password').clear().type(backofficeUsers.admin.password, { log: false });
    cy.contains('button', 'Đăng nhập hệ thống').click();

    cy.location('pathname', { timeout: 20000 }).should('eq', '/admin/dashboard');
    cy.contains(backofficeUsers.admin.roleLabel).should('be.visible');
  });

  it('TC-ADM-LOGIN-002 - Giao diện đăng nhập hiển thị ổn định theo thiết kế', () => {
    logoutIfLoggedIn();

    cy.visit('/login');

    cy.contains('Đăng nhập quản trị').should('be.visible');
    cy.contains('Tên tài khoản').should('be.visible');
    cy.contains('Mật khẩu').should('be.visible');
    cy.contains('Ghi nhớ đăng nhập').should('be.visible');
    cy.contains('button', 'Đăng nhập hệ thống').should('be.visible');

    cy.get('.hero')
      .should('be.visible')
      .invoke('attr', 'style')
      .should('contain', 'background-image');

    cy.get('.hero-logo img')
      .should('be.visible')
      .and('have.attr', 'src')
      .and('not.be.empty');

    cy.get('.hero-logo').then(($logo) => {
      const logoRect = $logo[0].getBoundingClientRect();

      cy.get('.hero-title').then(($title) => {
        const titleRect = $title[0].getBoundingClientRect();
        expect(logoRect.bottom).to.be.lessThan(titleRect.top + 4);
      });
    });
  });

  it('TC-ADM-SIDEBAR-001 - Hiển thị tên câu lạc bộ, role và nút đăng xuất', () => {
    visitBackofficePage('/admin/dashboard', 'admin');

    cy.get('aside.admin-sidebar').within(() => {
      cy.contains('Chia sẻ yêu thương').should('be.visible');
      cy.contains(backofficeUsers.admin.roleLabel).should('be.visible');
      cy.contains('button', 'Đăng xuất').should('be.visible');
      cy.get('form[action="/logout"]').should('exist');
    });

    cy.contains('button', 'Đăng xuất').click();
    cy.location('pathname', { timeout: 20000 }).should('eq', '/login');
    cy.location('search').should('include', 'logout');
    cy.contains('Bạn đã đăng xuất thành công.').should('be.visible');
  });

  it('TC-ADM-SIDEBAR-002 - Thứ tự menu và nhãn hiển thị đúng theo thiết kế', () => {
    logoutIfLoggedIn();

    cy.visit('/login');
    cy.get('#username').clear().type(backofficeUsers.admin.username);
    cy.get('#password').clear().type(backofficeUsers.admin.password, { log: false });
    cy.contains('button', 'Đăng nhập hệ thống').click();

    cy.location('pathname', { timeout: 20000 }).should('eq', '/admin/dashboard');
    cy.contains(backofficeUsers.admin.roleLabel).should('be.visible');
    cy.get('aside.admin-sidebar').should('be.visible');

    cy.get('aside.admin-sidebar nav a').then(($items) => {
      const labels = [...$items].map((item) => {
        const lines = item.innerText
          .split('\n')
          .map((line) => line.trim())
          .filter(Boolean);
        return lines[lines.length - 1];
      });

      expect(labels).to.deep.equal([
        'Tổng quan',
        'Sự kiện',
        'Hoạt động',
        'Nhà hảo tâm',
        'Quyên góp',
        'Lịch sử giao dịch',
        'Cài đặt'
      ]);
    });
  });
});
