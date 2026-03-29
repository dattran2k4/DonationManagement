import { pagedResponse, stubAlert, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Admin Events', () => {
  it('loads the events list and applies search, status, category and sort filters', () => {
    cy.intercept('GET', '/api/events?*', (req) => {
      const search = req.query.search || '';
      const status = req.query.status || '';
      const categoryIds = req.query.categoryIds || '';
      const sortBy = req.query.sortBy || 'id';
      const sortDir = req.query.sortDir || 'desc';

      let events = [
        {
          id: 1,
          name: 'Gay quy mo tim',
          code: 'EVT-001',
          categoryId: 1,
          status: 'ONGOING',
          currentAmount: 25000000,
          targetAmount: 50000000,
          startDate: '2026-03-01',
          endDate: '2026-04-01',
          thumbnailUrl: ''
        },
        {
          id: 2,
          name: 'Tiep suc vung cao',
          code: 'EVT-002',
          categoryId: 2,
          status: 'UPCOMING',
          currentAmount: 5000000,
          targetAmount: 20000000,
          startDate: '2026-04-10',
          endDate: '2026-05-10',
          thumbnailUrl: ''
        }
      ];

      if (search) {
        events = events.filter((item) => item.name.includes(search) || item.code.includes(search));
      }
      if (status) {
        events = events.filter((item) => item.status === status);
      }
      if (categoryIds) {
        events = events.filter((item) => String(item.categoryId) === String(categoryIds));
      }
      if (sortBy === 'currentAmount' && sortDir === 'desc') {
        events = [...events].sort((a, b) => b.currentAmount - a.currentAmount);
      }

      req.reply({ statusCode: 200, body: pagedResponse(events, { pageSize: 5 }) });
    }).as('listEvents');

    visitAdminPage('/admin/events');
    cy.wait('@listEvents');
    cy.contains('Gay quy mo tim').should('be.visible');

    cy.get('#searchFilter').type('mo tim');
    cy.wait('@listEvents').its('request.query.search').should('eq', 'mo tim');

    cy.get('#statusFilter').select('ONGOING');
    cy.wait('@listEvents').its('request.query.status').should('eq', 'ONGOING');

    cy.get('#categoryFilter').select('1');
    cy.wait('@listEvents').its('request.query.categoryIds').should('eq', '1');

    cy.get('#sortFilter').select('currentAmount:desc');
    cy.wait('@listEvents').then(({ request }) => {
      expect(request.query.sortBy).to.eq('currentAmount');
      expect(request.query.sortDir).to.eq('desc');
    });
  });

  it('creates a new event successfully from the admin form', () => {
    stubAlert();

    cy.intercept('POST', '/api/events/save', (req) => {
      expect(req.body).to.include({
        name: 'Su kien Cypress Moi',
        categoryId: '1',
        status: 'ONGOING',
        location: 'Da Nang'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Successfully saved event', data: 901 }
      });
    }).as('saveEvent');

    visitAdminPage('/admin/events/form');

    cy.get('#eventName').type('Su kien Cypress Moi');
    cy.get('#eventCategory').select('1');
    cy.get('#eventLocation').type('Da Nang');
    cy.get('#eventStartDate').type('2026-03-20');
    cy.get('#eventEndDate').type('2026-03-30');
    cy.get('#eventStatus').select('ONGOING');
    cy.get('#eventTargetAmount').type('10000000');
    cy.get('#saveBtn').click();

    cy.wait('@saveEvent');
    cy.get('@alert').should('have.been.calledWith', 'Lưu thành công!');
  });

  it('validates that the event name is required before saving', () => {
    stubAlert();

    visitAdminPage('/admin/events/form');
    cy.get('#saveBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng nhập tên sự kiện!');
  });

  it('shows a business error when the event end date is before the start date', () => {
    stubAlert();

    visitAdminPage('/admin/events/form');

    cy.get('#eventName').type('Su kien ngay loi');
    cy.get('#eventCategory').select('1');
    cy.get('#eventStatus').select('ONGOING');
    cy.get('#eventStartDate').type('2026-03-30');
    cy.get('#eventEndDate').type('2026-03-20');
    cy.get('#saveBtn').click();

    cy.get('@alert').should((alertStub) => {
      expect(alertStub).to.have.been.calledOnce;
      expect(String(alertStub.getCall(0).args[0])).to.contain('Thời gian kết thúc sự kiện không được trước thời gian bắt đầu');
    });
  });

  it('updates an existing event successfully from the edit form', () => {
    stubAlert();

    cy.intercept('POST', '/api/events/save', (req) => {
      expect(req.body.id).to.eq('1');
      expect(req.body.name).to.eq('Gay quy mo tim cap nhat');
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Successfully saved event', data: 1 }
      });
    }).as('saveEvent');

    visitAdminPage('/admin/events/1/form');

    cy.get('#eventName').clear().type('Gay quy mo tim cap nhat');
    cy.get('#saveBtn').click();

    cy.wait('@saveEvent');
    cy.get('@alert').should('have.been.calledWith', 'Lưu thành công!');
  });

  it('does not render an edit action for completed events in the admin list', () => {
    cy.intercept('GET', '/api/events?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 99,
          name: 'Su kien da hoan thanh',
          code: 'EVT-099',
          categoryId: 1,
          status: 'COMPLETED',
          currentAmount: 10000000,
          targetAmount: 10000000,
          startDate: '2026-01-01',
          endDate: '2026-01-30',
          thumbnailUrl: ''
        }
      ])
    }).as('listEvents');

    visitAdminPage('/admin/events');
    cy.wait('@listEvents');

    cy.contains('Su kien da hoan thanh').should('be.visible');
    cy.get('a[href="/admin/events/99/form"]').should('not.exist');
  });
});
