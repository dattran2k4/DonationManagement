import { pagedResponse, stubAlert, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Admin Donors', () => {
  it('loads the donor list and applies keyword and type filters', () => {
    cy.intercept('GET', '/api/donors?*', (req) => {
      const search = req.query.search || '';
      const type = req.query.type || '';

      let donors = [
        {
          id: 1,
          type: 'INDIVIDUAL',
          fullName: 'Nguyen Thi Lan',
          phone: '0911111111',
          email: 'lan@test.local',
          createdAt: '2026-03-01T08:00:00',
          numberOfDonations: 2,
          totalDonationAmount: 5000000
        },
        {
          id: 2,
          type: 'ORGANIZATION',
          fullName: 'Cong ty Thien Tam',
          phone: '0909999999',
          email: 'contact@thientam.local',
          createdAt: '2026-03-02T08:00:00',
          numberOfDonations: 5,
          totalDonationAmount: 12000000,
          organization: {
            name: 'Cong ty Thien Tam',
            representative: 'Tran Van A'
          }
        }
      ];

      if (search) {
        donors = donors.filter((item) =>
          (item.fullName || '').includes(search) ||
          (item.email || '').includes(search) ||
          (item.phone || '').includes(search) ||
          (item.organization?.name || '').includes(search)
        );
      }

      if (type) {
        donors = donors.filter((item) => item.type === type);
      }

      req.reply({ statusCode: 200, body: pagedResponse(donors) });
    }).as('listDonors');

    visitAdminPage('/admin/donors');
    cy.wait('@listDonors');

    cy.contains('Nguyen Thi Lan').should('be.visible');
    cy.contains('Cong ty Thien Tam').should('be.visible');

    cy.get('#donorSearchInput').type('Lan');
    cy.wait('@listDonors').its('request.query.search').should('eq', 'Lan');
    cy.contains('Nguyen Thi Lan').should('be.visible');

    cy.get('#donorSearchInput').clear();
    cy.wait('@listDonors').then(({ request }) => {
      expect(request.query.search || '').to.eq('');
    });

    cy.get('#donorTypeFilter').select('ORGANIZATION');
    cy.wait('@listDonors').its('request.query.type').should('eq', 'ORGANIZATION');
    cy.contains('Cong ty Thien Tam').should('be.visible');
    cy.contains('Hiển thị').should('be.visible');
    cy.get('button[title="Xem hồ sơ"]').should('exist');
    cy.get('button[title="Chỉnh sửa"]').should('exist');
  });

  it('creates an individual donor successfully from the admin form', () => {
    stubAlert();

    cy.intercept('POST', '/api/donors/individuals', (req) => {
      expect(req.body).to.include({
        fullName: 'Le Van B',
        displayName: 'Anh B',
        phone: '0912345678',
        email: 'levanb@test.local'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Lưu nhà hảo tâm thành công', data: 88 }
      });
    }).as('createDonor');

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 88,
          type: 'INDIVIDUAL',
          fullName: 'Le Van B',
          phone: '0912345678',
          email: 'levanb@test.local',
          createdAt: '2026-03-22T08:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0
        }
      ])
    }).as('reloadDonors');

    visitAdminPage('/admin/donors/form');

    cy.get('#fullName').type('Le Van B');
    cy.get('#displayName').type('Anh B');
    cy.get('#phone').type('0912345678');
    cy.get('#email').type('levanb@test.local');
    cy.get('#saveDonorBtn').click();

    cy.wait('@createDonor');
    cy.get('@alert').should('have.been.calledWith', 'Lưu nhà hảo tâm thành công');
    cy.wait('@reloadDonors');
    cy.location('pathname').should('eq', '/admin/donors');
  });

  it('creates an organization donor successfully from the admin form', () => {
    stubAlert();

    cy.intercept('POST', '/api/donors/organizations', (req) => {
      expect(req.body).to.include({
        name: 'Cong ty An Tam',
        taxCode: '0107654321',
        representative: 'Tran Thi Org',
        phone: '0902222333',
        email: 'org-admin@test.local'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Lưu nhà hảo tâm thành công', data: 89 }
      });
    }).as('createOrganization');

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 89,
          type: 'ORGANIZATION',
          fullName: 'Cong ty An Tam',
          phone: '0902222333',
          email: 'org-admin@test.local',
          createdAt: '2026-03-23T08:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0,
          organization: {
            name: 'Cong ty An Tam',
            representative: 'Tran Thi Org'
          }
        }
      ])
    }).as('reloadDonors');

    visitAdminPage('/admin/donors/form');

    cy.get('#donor_org').check({ force: true });
    cy.get('#orgName').type('Cong ty An Tam');
    cy.get('#taxCode').type('0107654321');
    cy.get('#representative').type('Tran Thi Org');
    cy.get('#billingAddress').type('1 Nguyen Hue');
    cy.get('#phone').type('0902222333');
    cy.get('#email').type('org-admin@test.local');
    cy.get('#saveDonorBtn').click();

    cy.wait('@createOrganization');
    cy.get('@alert').should('have.been.calledWith', 'Lưu nhà hảo tâm thành công');
    cy.wait('@reloadDonors');
    cy.location('pathname').should('eq', '/admin/donors');
  });

  it('validates required individual donor fields on the admin form', () => {
    stubAlert();

    visitAdminPage('/admin/donors/form');

    cy.get('#displayName').type('Bo trong ho ten');
    cy.get('#phone').type('0911111222');
    cy.get('#email').type('required@test.local');
    cy.get('#saveDonorBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Họ và tên không được để trống');
  });

  it('validates required organization donor fields on the admin form', () => {
    stubAlert();

    visitAdminPage('/admin/donors/form');

    cy.get('#donor_org').check({ force: true });
    cy.get('#phone').type('0901111111');
    cy.get('#email').type('org@test.local');
    cy.get('#saveDonorBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Tên tổ chức không được để trống');
  });

  it('shows the seeded donor detail page with the saved contact information', () => {
    visitAdminPage('/admin/donors/1');

    cy.contains('Hồ sơ Nhà hảo tâm').should('be.visible');
    cy.contains('Phạm Thị Lan').should('be.visible');
    cy.contains('0907000001').should('be.visible');
    cy.contains('lan.pham@example.com').should('be.visible');
    cy.contains('Chỉnh sửa').should('be.visible');
  });

  it('loads the edit donor form, keeps donor type locked and saves updated information', () => {
    stubAlert();

    cy.intercept('GET', '/api/donors/1', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          id: 1,
          type: 'INDIVIDUAL',
          fullName: 'Pham Thi Lan',
          displayName: 'Co Lan',
          phone: '0907000001',
          email: 'lan.pham@example.com',
          referralSource: 'Facebook',
          note: 'Donor cu'
        }
      }
    }).as('getDonor');

    cy.intercept('PUT', '/api/donors/1/individuals', (req) => {
      expect(req.body).to.include({
        fullName: 'Pham Thi Lan Updated',
        displayName: 'Co Lan Moi',
        phone: '0907000099',
        email: 'lan.updated@test.local'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Donor updated successfully', data: 1 }
      });
    }).as('updateDonor');

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 1,
          type: 'INDIVIDUAL',
          fullName: 'Pham Thi Lan Updated',
          phone: '0907000099',
          email: 'lan.updated@test.local',
          createdAt: '2026-03-23T08:00:00',
          numberOfDonations: 3,
          totalDonationAmount: 1000000
        }
      ])
    }).as('reloadDonors');

    visitAdminPage('/admin/donors/1/form');
    cy.wait('@getDonor');

    cy.get('#donor_personal').should('be.disabled');
    cy.get('#donor_org').should('be.disabled');

    cy.get('#fullName').clear().type('Pham Thi Lan Updated');
    cy.get('#displayName').clear().type('Co Lan Moi');
    cy.get('#phone').clear().type('0907000099');
    cy.get('#email').clear().type('lan.updated@test.local');
    cy.get('#saveDonorBtn').click();

    cy.wait('@updateDonor');
    cy.get('@alert').should('have.been.calledWith', 'Cập nhật nhà hảo tâm thành công');
    cy.wait('@reloadDonors');
    cy.location('pathname').should('eq', '/admin/donors');
  });
});
