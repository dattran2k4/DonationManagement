import { pagedResponse, stubAlert, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Admin Activities', () => {
  it('loads the activities list, filters it and resets the filters', () => {
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

  it('creates a new activity successfully from the admin form', () => {
    stubAlert();

    cy.intercept('POST', '/api/activities/save', (req) => {
      expect(req.body).to.include({
        eventId: '1',
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

    cy.get('#activityEvent').select('1');
    cy.get('#activityName').type('Hoat dong Cypress Moi');
    cy.get('#activityLocation').type('Da Nang');
    cy.get('#activityStatus').select('UPCOMING');
    cy.get('#activityEndDate').type('2026-04-15');
    cy.get('#saveBtn').click();

    cy.wait('@saveActivity');
    cy.get('@alert').should('have.been.calledWith', 'Lưu hoạt động thành công!');
  });

  it('validates that the parent event is required before saving an activity', () => {
    stubAlert();

    visitAdminPage('/admin/activities/form');
    cy.get('#saveBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng chọn sự kiện cha!');
  });

  it('auto-fills the activity start date from the selected parent event on create mode', () => {
    visitAdminPage('/admin/activities/form');

    cy.get('#activityStartDate').should('have.value', '');
    cy.get('#activityEvent').find('option[value="1"]').invoke('attr', 'data-start-date').then((startDate) => {
      cy.get('#activityEvent').select('1');
      cy.get('#activityStartDate').should('have.value', startDate);
    });
  });

  it('updates an existing activity successfully from the edit form', () => {
    stubAlert();

    cy.intercept('POST', '/api/activities/save', (req) => {
      expect(req.body.id).to.eq('1');
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
