import { pagedResponse, stubAlert, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Quản lý Hoạt động', () => {
  it('TC-ADM-ACT-001 + TC-ADM-ACT-002 - Mở danh sách hoạt động, tìm kiếm và lọc', () => {
    cy.intercept('GET', '/api/activities?*', (req) => {
      const search = req.query.search || '';
      const status = req.query.status || '';

      let activities = [
        {
          id: 301,
          name: 'Kham sang loc',
          event: { name: 'Gay quy mo tim' },
          startDate: '2026-03-10',
          endDate: '2026-03-12',
          location: 'Benh vien A',
          currentAmount: 5000000,
          targetAmount: 10000000,
          status: 'ONGOING'
        },
        {
          id: 302,
          name: 'Hau phau',
          event: { name: 'Gay quy mo tim' },
          startDate: '2026-04-10',
          endDate: '2026-04-15',
          location: 'Benh vien B',
          currentAmount: 2000000,
          targetAmount: 8000000,
          status: 'UPCOMING'
        }
      ];

      if (search) {
        activities = activities.filter((item) => item.name.includes(search) || item.location.includes(search));
      }
      if (status) {
        activities = activities.filter((item) => item.status === status);
      }

      req.reply({ statusCode: 200, body: pagedResponse(activities, { pageSize: 2 }) });
    }).as('listActivities');

    visitAdminPage('/admin/activities');
    cy.wait('@listActivities');
    cy.contains('Kham sang loc').should('be.visible');

    cy.get('#activitySearchInput').type('Kham');
    cy.wait('@listActivities').its('request.query.search').should('eq', 'Kham');

    cy.get('#activityStatusFilter').select('ONGOING');
    cy.wait('@listActivities').its('request.query.status').should('eq', 'ONGOING');

    cy.get('#activityResetFilterBtn').click();
    cy.wait('@listActivities').then(({ request }) => {
      expect(request.query.search || '').to.eq('');
      expect(request.query.status || '').to.eq('');
    });
  });

  it('TC-ADM-ACT-003 - Tạo hoạt động thành công', () => {
    stubAlert();

    cy.intercept('POST', '/api/activities/save', (req) => {
      expect(req.body).to.include({
        eventId: 1,
        name: 'Hoat dong Cypress Moi',
        status: 'UPCOMING',
        location: 'Da Nang'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Successfully saved activity', data: 801 }
      });
    }).as('saveActivity');

    visitAdminPage('/admin/activities/form');

    cy.get('#activityEventSearch').click();
    cy.contains('.activity-event-option', 'Gây quỹ mổ tim cho bé An').click();
    cy.get('#activityName').type('Hoat dong Cypress Moi');
    cy.get('#activityLocation').type('Da Nang');
    cy.get('#activityStatus').select('UPCOMING');
    cy.get('#activityEndDate').type('2026-04-15');
    cy.get('#saveBtn').click();

    cy.wait('@saveActivity');
    cy.get('@alert').should('have.been.calledWith', 'Lưu hoạt động thành công!');
  });

  it('TC-ADM-ACT-004 - Kiểm tra bắt buộc của hoạt động', () => {
    stubAlert();

    visitAdminPage('/admin/activities/form');
    cy.get('#saveBtn').click();

    cy.get('@alert').should((alertStub) => {
      expect(alertStub).to.have.been.calledOnce;
      expect(String(alertStub.getCall(0).args[0])).to.contain('sự kiện cha');
    });
  });

  it('TC-ADM-ACT-005 - Tự đồng bộ ngày bắt đầu từ sự kiện cha', () => {
    visitAdminPage('/admin/activities/form');

    cy.get('#activityStartDate').should('have.value', '');
    cy.get('#activityEventSearch').click();
    cy.get('.activity-event-option[data-id="1"]').invoke('attr', 'data-start-date').then((startDate) => {
      cy.get('.activity-event-option[data-id="1"]').click();
      cy.get('#activityStartDate').should('have.value', startDate);
    });
  });

  it('TC-ADM-ACT-006 - Sửa hoạt động thành công', () => {
    stubAlert();

    cy.intercept('POST', '/api/activities/save', (req) => {
      expect(req.body.id).to.eq(1);
      expect(req.body.name).to.eq('Dot 1 cap nhat');
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Successfully saved activity', data: 1 }
      });
    }).as('saveActivity');

    visitAdminPage('/admin/activities/1/form');

    cy.get('#activityName').clear().type('Dot 1 cap nhat');
    cy.get('#saveBtn').click();

    cy.wait('@saveActivity');
    cy.get('@alert').should('have.been.calledWith', 'Lưu hoạt động thành công!');
  });
});
