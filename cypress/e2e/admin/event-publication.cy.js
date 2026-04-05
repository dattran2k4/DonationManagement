import { visitBackofficePage } from './helpers/adminTestUtils.js';

const dbCmd = (sql) =>
  `docker exec csyt-db mysql -ucsyt_user -p123456 -D donation -Nse "${sql}"`;

describe('Xuất bản sự kiện', () => {
  afterEach(function () {
    if (!this.eventSlug) return;

    cy.exec(
      dbCmd(`delete from events where slug = '${this.eventSlug}';`),
      { failOnNonZeroExit: false }
    );
  });

  it('TC-ADM-EVT-010 - Sự kiện trạng thái Nháp không hiển thị trên website', function () {
    const suffix = Date.now();
    const eventName = `Cypress Draft Event ${suffix}`;
    const eventSlug = `cypress-draft-event-${suffix}`;
    this.eventSlug = eventSlug;

    cy.exec(
      dbCmd(
        [
          'insert into events (name, slug, status, short_description, description, content, location, thumbnail_url, target_amount, current_amount, start_date, end_date, category_id, created_at, updated_at)',
          `values ('${eventName}', '${eventSlug}', 'DRAFT', 'Su kien nhap de test publish', 'Mo ta nhap', 'Noi dung nhap', 'Da Nang', '/images/default-event.png', 10000000, 0, '2026-04-01', '2026-04-30', 1, now(), now());`
        ].join(' ')
      )
    );

    visitBackofficePage('/admin/events');
    cy.get('#searchFilter').clear().type(eventName);
    cy.contains(eventName, { timeout: 20000 }).should('be.visible');
    cy.contains('Bản nháp').should('be.visible');

    cy.visit('/events');
    cy.get('#search-event').clear().type(eventName);
    cy.contains('Không tìm thấy sự kiện nào khớp với bộ lọc.', { timeout: 20000 }).should('be.visible');
  });

  it('TC-ADM-EVT-011 - Chuyển từ Nháp sang trạng thái công khai thì sự kiện xuất hiện trên website', function () {
    const suffix = Date.now();
    const eventName = `Cypress Publish Event ${suffix}`;
    const eventSlug = `cypress-publish-event-${suffix}`;
    this.eventSlug = eventSlug;

    cy.exec(
      dbCmd(
        [
          'insert into events (name, slug, status, short_description, description, content, location, thumbnail_url, target_amount, current_amount, start_date, end_date, category_id, created_at, updated_at)',
          `values ('${eventName}', '${eventSlug}', 'DRAFT', 'Su kien nhap de test public', 'Mo ta nhap', 'Noi dung nhap', 'Da Nang', '/images/default-event.png', 10000000, 0, '2026-04-01', '2026-04-30', 1, now(), now());`
        ].join(' ')
      )
    );

    cy.exec(
      dbCmd(`update events set status = 'UPCOMING', updated_at = now() where slug = '${eventSlug}';`)
    );

    cy.visit('/events');
    cy.get('#search-event').clear().type(eventName);
    cy.contains(eventName, { timeout: 20000 }).should('be.visible');
  });
});
