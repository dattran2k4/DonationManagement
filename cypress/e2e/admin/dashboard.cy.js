import { visitAdminPage } from './helpers/adminTestUtils.js';

describe('Bảng điều khiển quản trị', () => {
  it('TC-ADM-DASH-001 - Mở trang bảng điều khiển', () => {
    cy.intercept('GET', '/api/dashboard/donation-trend*', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          points: [
            { label: 'T2', totalAmount: 1200000 },
            { label: 'T3', totalAmount: 4500000 }
          ]
        }
      }
    }).as('getTrend');

    visitAdminPage('/admin/dashboard');
    cy.wait('@getTrend');

    cy.contains('Tổng quan').should('be.visible');
    cy.contains('Quản trị viên').should('be.visible');
    cy.get('#donationTrendSummary').should('contain', '7 ngày gần nhất');
    cy.get('#donationTrendBars').children().should('have.length', 2);
  });

  it('TC-ADM-DASH-002 - Chuyển kỳ thống kê', () => {
    cy.intercept('GET', '/api/dashboard/donation-trend?period=WEEK', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          points: [
            { label: 'T2', totalAmount: 500000 },
            { label: 'T3', totalAmount: 1000000 }
          ]
        }
      }
    }).as('trendWeek');

    cy.intercept('GET', '/api/dashboard/donation-trend?period=MONTH', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          points: [
            { label: 'Tuần 1', totalAmount: 5000000 },
            { label: 'Tuần 2', totalAmount: 3000000 },
            { label: 'Tuần 3', totalAmount: 7000000 }
          ]
        }
      }
    }).as('trendMonth');

    visitAdminPage('/admin/dashboard');
    cy.wait('@trendWeek');

    cy.get('[data-dashboard-period="MONTH"]').click();
    cy.wait('@trendMonth');

    cy.get('#donationTrendSummary').should('contain', '30 ngày gần nhất');
    cy.get('[data-dashboard-period="MONTH"]').should('have.class', 'bg-primary');
    cy.get('#donationTrendBars').children().should('have.length', 3);
  });

  it('TC-ADM-DASH-003 - Hiển thị ổn định khi không có dữ liệu', () => {
    cy.intercept('GET', '/api/dashboard/donation-trend?period=WEEK', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          points: []
        }
      }
    }).as('trendWeek');

    visitAdminPage('/admin/dashboard');
    cy.wait('@trendWeek');

    cy.get('#donationTrendSummary').should('contain', '7 ngày gần nhất');
    cy.get('#donationTrendEmptyState').should('be.visible');
    cy.get('#donationTrendBars').children().should('have.length', 0);
  });
});
