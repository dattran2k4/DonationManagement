import { pagedResponse, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Admin Transactions', () => {
  it('loads the transactions list, searches, filters by method and shows unlinked rows', () => {
    cy.intercept('GET', '/api/transactions?*', (req) => {
      const search = req.query.search || '';
      const method = req.query.method || '';

      let transactions = [
        {
          id: 401,
          transactionCode: 'TXN-401',
          amount: 1000000,
          counterAccountName: 'Nguyen Van Chuyen Khoan',
          counterAccountNumber: '123456789',
          donationCode: 'MEMO001',
          paymentMethodValue: 'Chuyển khoản online',
          paymentMethod: 'BANK_TRANSFER_ONLINE',
          createdAt: '2026-03-22T08:00:00'
        },
        {
          id: 402,
          transactionCode: 'TXN-402',
          amount: 2000000,
          counterAccountName: 'Thu quy',
          counterAccountNumber: '',
          donationCode: null,
          paymentMethodValue: 'Tiền mặt',
          paymentMethod: 'CASH',
          createdAt: '2026-03-22T09:00:00'
        }
      ];

      if (search) {
        transactions = transactions.filter((item) =>
          item.transactionCode.includes(search) || (item.counterAccountName || '').includes(search)
        );
      }
      if (method) {
        transactions = transactions.filter((item) => item.paymentMethod === method);
      }

      req.reply({ statusCode: 200, body: pagedResponse(transactions) });
    }).as('listTransactions');

    visitAdminPage('/admin/transactions');
    cy.wait('@listTransactions');
    cy.contains('TXN-401').should('be.visible');
    cy.contains('TXN-402').should('be.visible');
    cy.contains('--').should('be.visible');

    cy.get('#transactionSearchInput').type('TXN-401');
    cy.wait('@listTransactions').its('request.query.search').should('eq', 'TXN-401');

    cy.get('#transactionMethodFilter').select('CASH');
    cy.wait('@listTransactions').its('request.query.method').should('eq', 'CASH');

    cy.get('#transactionResetFilterBtn').click();
    cy.wait('@listTransactions').then(({ request }) => {
      expect(request.query.search || '').to.eq('');
      expect(request.query.method || '').to.eq('');
    });
  });

  it('shows a transaction detail page for a synthetic linked transaction record', () => {
    const suffix = `${Date.now()}`;
    const transactionCode = `CYPTRX-${suffix}`;
    let transactionId = 0;
    let donationId = 0;

    cy.exec(
      `docker exec csyt-db mysql -ucsyt_user -p123456 -D donation -Nse "select id from donations where id not in (select donation_id from donation_transactions where donation_id is not null) order by id desc limit 1;"`
    ).then(({ stdout }) => {
      donationId = Number(stdout.trim().split('\n').pop());
      expect(donationId).to.be.greaterThan(0);
    });

    cy.then(() => {
      cy.exec(
        `docker exec csyt-db mysql -ucsyt_user -p123456 -D donation -Nse "insert into donation_transactions (donation_id, amount, payment_method, created_at, updated_at, account_bank_id, counter_account_name, counter_account_number, description, raw_api_data, transaction_code, transaction_date_time) values (${donationId}, 7654321.00, 'BANK_TRANSFER_OFFLINE', now(), now(), 'BANK-CYP', 'CYPRESS TRANSFER', '970400001234', 'Noi dung test chi tiet', '{}', '${transactionCode}', '2026-03-23 10:10:10'); select id from donation_transactions where transaction_code = '${transactionCode}' order by id desc limit 1;"`
      ).then(({ stdout }) => {
        transactionId = Number(stdout.trim().split('\n').pop());
      });
    });

    cy.then(() => {
      visitAdminPage(`/admin/transactions/${transactionId}`);
    });

    cy.contains(transactionCode).should('be.visible');
    cy.contains('7654321').should('be.visible');
    cy.contains('VND').should('be.visible');
    cy.contains('CYPRESS TRANSFER').should('be.visible');
    cy.contains('970400001234').should('be.visible');
    cy.contains('Noi dung test chi tiet').should('be.visible');
  });
});
