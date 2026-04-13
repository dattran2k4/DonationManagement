import { stubAlert, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Cài đặt Hệ thống', () => {
  it('TC-ADM-SET-001 + TC-ADM-SET-002 - Mở trang cài đặt và cập nhật cấu hình văn bản thành công', () => {
    stubAlert();

    cy.intercept('GET', '/api/configs/map', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          ORG_NAME: 'Chia Sẻ Yêu Thương',
          ORG_LOGO_URL: '/images/logo.jpg',
          ORG_ADDRESS: '1 Nguyen Hue',
          ORG_PHONE: '0905000001',
          ORG_EMAIL: 'contact@test.local',
          ORG_FACEBOOK_URL: 'https://facebook.com/demo',
          HOME_BANNER_URL: '',
          ABOUT_BANNER_URL: '',
          ABOUT_STORY: 'Story',
          ABOUT_MISSION_VISION: 'Mission',
          ABOUT_OLD_LOGO_URL: '',
          ABOUT_NEW_LOGO_URL: ''
        }
      }
    }).as('getSettings');

    cy.intercept('PUT', '/api/configs', (req) => {
      const configs = req.body.configs || [];
      const asMap = Object.fromEntries(configs.map((item) => [item.key, item.value]));

      expect(asMap.ORG_NAME).to.eq('CLB Admin Cypress');
      expect(asMap.ORG_PHONE).to.eq('0911222333');

      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Đã lưu cấu hình hệ thống thành công.', data: null }
      });
    }).as('saveSettings');

    visitAdminPage('/admin/settings');
    cy.wait('@getSettings');

    cy.get('#settingsTabAbout').click();
    cy.get('[data-tab-panel="about"]').should('be.visible');
    cy.get('#settingsTabGeneral').click();
    cy.get('[data-tab-panel="general"]').should('be.visible');

    cy.get('#ORG_NAME').clear().type('CLB Admin Cypress');
    cy.get('#ORG_PHONE').clear().type('0911222333');
    cy.contains('button', /^Lưu$/).click();

    cy.wait('@saveSettings');
    cy.get('@alert').should('have.been.calledWith', 'Đã lưu cấu hình hệ thống thành công.');

    cy.intercept('GET', '/api/configs/map', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          ORG_NAME: 'CLB Admin Cypress',
          ORG_LOGO_URL: '/images/logo.jpg',
          ORG_ADDRESS: '1 Nguyen Hue',
          ORG_PHONE: '0911222333',
          ORG_EMAIL: 'contact@test.local',
          ORG_FACEBOOK_URL: 'https://facebook.com/demo',
          HOME_BANNER_URL: '',
          ABOUT_BANNER_URL: '',
          ABOUT_STORY: 'Story',
          ABOUT_MISSION_VISION: 'Mission',
          ABOUT_OLD_LOGO_URL: '',
          ABOUT_NEW_LOGO_URL: ''
        }
      }
    }).as('reloadSettings');

    cy.reload();
    cy.wait('@reloadSettings');
    cy.get('#ORG_NAME').should('have.value', 'CLB Admin Cypress');
    cy.get('#ORG_PHONE').should('have.value', '0911222333');
  });

  it('TC-ADM-SET-004 + TC-ADM-SET-005 - Kiểm tra tệp ảnh không hợp lệ và đặt lại thay đổi chưa lưu', () => {
    stubAlert();

    cy.intercept('GET', '/api/configs/map', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          ORG_NAME: 'Chia Sẻ Yêu Thương',
          ORG_LOGO_URL: '/images/logo.jpg',
          ORG_ADDRESS: '1 Nguyen Hue',
          ORG_PHONE: '0905000001',
          ORG_EMAIL: 'contact@test.local',
          ORG_FACEBOOK_URL: 'https://facebook.com/demo',
          HOME_BANNER_URL: '',
          ABOUT_BANNER_URL: '',
          ABOUT_STORY: 'Story',
          ABOUT_MISSION_VISION: 'Mission',
          ABOUT_OLD_LOGO_URL: '',
          ABOUT_NEW_LOGO_URL: ''
        }
      }
    }).as('getSettings');

    visitAdminPage('/admin/settings');
    cy.wait('@getSettings');

    cy.get('#ORG_NAME').clear().type('Tam doi');
    cy.get('#resetSettingsBtn').click();
    cy.wait('@getSettings');
    cy.get('#ORG_NAME').should('have.value', 'Chia Sẻ Yêu Thương');

    cy.get('#ORG_LOGO_URL_file').selectFile(
      {
        contents: Cypress.Buffer.from('not-an-image'),
        fileName: 'invalid.txt',
        mimeType: 'text/plain'
      },
      { force: true }
    );

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng chọn tệp hình ảnh hợp lệ.');
  });

  it('TC-ADM-SET-003 - Tải ảnh hợp lệ', () => {
    stubAlert();

    cy.intercept('GET', '/api/configs/map', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          ORG_NAME: 'Chia Sẻ Yêu Thương',
          ORG_LOGO_URL: '/images/logo.jpg',
          ORG_ADDRESS: '1 Nguyen Hue',
          ORG_PHONE: '0905000001',
          ORG_EMAIL: 'contact@test.local',
          ORG_FACEBOOK_URL: 'https://facebook.com/demo',
          HOME_BANNER_URL: '',
          ABOUT_BANNER_URL: '',
          ABOUT_STORY: 'Story',
          ABOUT_MISSION_VISION: 'Mission',
          ABOUT_OLD_LOGO_URL: '',
          ABOUT_NEW_LOGO_URL: ''
        }
      }
    }).as('getSettings');

    cy.intercept('POST', '/api/configs/upload-image', {
      statusCode: 201,
      body: {
        status: 201,
        message: 'Tải ảnh cấu hình thành công',
        data: '/uploads/images/logo-cypress.png'
      }
    }).as('uploadImage');

    cy.intercept('PUT', '/api/configs', (req) => {
      const configs = req.body.configs || [];
      const asMap = Object.fromEntries(configs.map((item) => [item.key, item.value]));
      expect(asMap.ORG_LOGO_URL).to.eq('/uploads/images/logo-cypress.png');
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Đã lưu cấu hình hệ thống thành công.', data: null }
      });
    }).as('saveSettings');

    visitAdminPage('/admin/settings');
    cy.wait('@getSettings');

    cy.get('#ORG_LOGO_URL_file').selectFile(
      {
        contents: Cypress.Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
        fileName: 'logo-cypress.png',
        mimeType: 'image/png'
      },
      { force: true }
    );

    cy.get('#ORG_LOGO_URL').should('have.value', '/uploads/images/logo-cypress.png');
    cy.get('#ORG_LOGO_URL_preview').should('have.attr', 'src').and('include', 'blob:');

    cy.contains('button', /^Lưu$/).click();

    cy.wait('@uploadImage');
    cy.wait('@saveSettings');
    cy.get('@alert').should('have.been.calledWith', 'Đã lưu cấu hình hệ thống thành công.');
    cy.get('#ORG_LOGO_URL').should('have.value', '/uploads/images/logo-cypress.png');
  });
});
