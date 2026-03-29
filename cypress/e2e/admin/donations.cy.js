import { pagedResponse, stubAlert, stubConfirm, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Admin Donations', () => {
  it('loads the donation list, filters it and resets the filters', () => {
    cy.intercept('GET', '/api/donations/list?*', (req) => {
      const search = req.query.search || '';
      const status = req.query.status || '';
      const target = req.query.target || '';

      let donations = [
        {
          id: 10,
          memoCode: 'MEMO001',
          donorName: 'Nguyen Thi Lan',
          donationVia: 'STAFF',
          amount: 2500000,
          objectName: 'Chien dich A',
          target: 'EVENT',
          paymentMethod: 'BANK_TRANSFER_ONLINE',
          status: 'PENDING_APPROVED',
          donatedAt: '2026-03-22T08:15:00'
        },
        {
          id: 11,
          memoCode: 'MEMO002',
          donorName: 'Cong ty Thien Tam',
          donationVia: 'WEBSITE',
          amount: 4000000,
          objectName: 'Khong gan muc tieu',
          target: 'NONE',
          paymentMethod: 'CASH',
          status: 'CONFIRMED',
          donatedAt: '2026-03-21T07:00:00'
        }
      ];

      if (search) {
        donations = donations.filter((item) => item.memoCode.includes(search) || item.donorName.includes(search));
      }
      if (status) {
        donations = donations.filter((item) => item.status === status);
      }
      if (target) {
        donations = donations.filter((item) => item.target === target);
      }

      req.reply({ statusCode: 200, body: pagedResponse(donations) });
    }).as('listDonations');

    visitAdminPage('/admin/donations');
    cy.wait('@listDonations');
    cy.contains('MEMO001').should('be.visible');

    cy.get('#donationSearchInput').type('MEMO001');
    cy.wait('@listDonations').its('request.query.search').should('eq', 'MEMO001');

    cy.get('#donationStatusFilter').select('PENDING_APPROVED');
    cy.wait('@listDonations').its('request.query.status').should('eq', 'PENDING_APPROVED');

    cy.get('#donationTargetFilter').select('EVENT');
    cy.wait('@listDonations').its('request.query.target').should('eq', 'EVENT');

    cy.get('#donationResetFilterBtn').click();
    cy.wait('@listDonations').then(({ request }) => {
      expect(request.query.search || '').to.eq('');
      expect(request.query.status || '').to.eq('');
      expect(request.query.target || '').to.eq('');
    });
  });

  it('approves a pending donation from the admin list', () => {
    stubAlert();
    stubConfirm(true);

    let callCount = 0;
    cy.intercept('GET', '/api/donations/list?*', (req) => {
      callCount += 1;
      const item = callCount === 1
        ? {
            id: 21,
            memoCode: 'PENDING-21',
            donorName: 'Donor Pending',
            donationVia: 'STAFF',
            amount: 1500000,
            objectName: 'Su kien tim',
            target: 'EVENT',
            paymentMethod: 'BANK_TRANSFER_ONLINE',
            status: 'PENDING_APPROVED',
            donatedAt: '2026-03-22T09:00:00'
          }
        : {
            id: 21,
            memoCode: 'PENDING-21',
            donorName: 'Donor Pending',
            donationVia: 'STAFF',
            amount: 1500000,
            objectName: 'Su kien tim',
            target: 'EVENT',
            paymentMethod: 'BANK_TRANSFER_ONLINE',
            status: 'CONFIRMED',
            donatedAt: '2026-03-22T09:00:00'
          };

      req.reply({ statusCode: 200, body: pagedResponse([item]) });
    }).as('loadDonationList');

    cy.intercept('PATCH', '/api/donations/21/change-status?status=CONFIRMED', {
      statusCode: 200,
      body: { status: 200, message: 'Cập nhật thành công!', data: null }
    }).as('approveDonation');

    visitAdminPage('/admin/donations');
    cy.wait('@loadDonationList');

    cy.contains('button', 'Duyệt').click();

    cy.wait('@approveDonation');
    cy.get('@confirm').should('have.been.called');
    cy.get('@alert').should('have.been.calledWith', 'Cập nhật thành công!');
    cy.wait('@loadDonationList');
    cy.contains('Đã xác nhận').should('be.visible');
  });

  it('creates an internal donation with target and receipt information', () => {
    stubAlert();

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 77,
          type: 'INDIVIDUAL',
          fullName: 'Tran Van Donation',
          phone: '0908080808',
          email: 'donation@test.local',
          createdAt: '2026-03-22T09:00:00',
          numberOfDonations: 1,
          totalDonationAmount: 1000000
        }
      ])
    }).as('searchDonors');

    cy.intercept('POST', '/api/donations/staff-create', (req) => {
      expect(req.body).to.include({
        donorId: 77,
        amount: 3500000,
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        needReceipt: true,
        receiptName: 'Tran Van Donation',
        receiptEmail: 'receipt@test.local',
        eventId: 15
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Tạo đơn quyên góp thành công.', data: 501 }
      });
    }).as('createDonation');

    cy.intercept('GET', '/api/donations/list?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 501,
          memoCode: 'STAFF501',
          donorName: 'Tran Van Donation',
          donationVia: 'STAFF',
          amount: 3500000,
          objectName: 'Event 15',
          target: 'EVENT',
          paymentMethod: 'BANK_TRANSFER_ONLINE',
          status: 'PENDING_APPROVED',
          donatedAt: '2026-03-22T09:30:00'
        }
      ])
    }).as('reloadDonationList');

    visitAdminPage('/admin/donations/form');

    cy.get('#donorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#donorDropdownList button', 'Tran Van Donation').click();
    cy.get('#donorId').should('have.value', '77');

    cy.get('#amount').type('3500000');
    cy.get('#message').type('Quyen gop noi bo');
    cy.get('#targetEvent').check({ force: true });
    cy.get('#eventTargetGroup').should('be.visible');
    cy.get('#eventId').type('15');
    cy.get('#needReceipt').check({ force: true });
    cy.get('#receiptFields').should('be.visible');
    cy.get('#receiptName').type('Tran Van Donation');
    cy.get('#receiptEmail').type('receipt@test.local');
    cy.get('#submitDonation').click();

    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Tạo đơn quyên góp thành công.');
    cy.wait('@reloadDonationList');
    cy.location('pathname').should('eq', '/admin/donations');
  });

  it('creates an internal donation without attaching an event or activity target', () => {
    stubAlert();

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 91,
          type: 'INDIVIDUAL',
          fullName: 'Donor No Target',
          phone: '0909000001',
          email: 'none@test.local',
          createdAt: '2026-03-23T09:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0
        }
      ])
    }).as('searchDonors');

    cy.intercept('POST', '/api/donations/staff-create', (req) => {
      expect(req.body).to.include({
        donorId: 91,
        amount: 1500000,
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        needReceipt: false
      });
      expect(req.body.eventId).to.equal(null);
      expect(req.body.activityId).to.equal(null);
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Tạo đơn quyên góp thành công.', data: 601 }
      });
    }).as('createDonation');

    cy.intercept('GET', '/api/donations/list?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 601,
          memoCode: 'STAFF601',
          donorName: 'Donor No Target',
          donationVia: 'STAFF',
          amount: 1500000,
          objectName: '',
          target: 'NONE',
          paymentMethod: 'BANK_TRANSFER_ONLINE',
          status: 'PENDING_APPROVED',
          donatedAt: '2026-03-23T09:30:00'
        }
      ])
    }).as('reloadDonationList');

    visitAdminPage('/admin/donations/form');

    cy.get('#donorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#donorDropdownList button', 'Donor No Target').click();
    cy.get('#amount').type('1500000');
    cy.get('#submitDonation').click();

    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Tạo đơn quyên góp thành công.');
    cy.wait('@reloadDonationList');
    cy.location('pathname').should('eq', '/admin/donations');
  });

  it('creates an internal donation with an attached activity target', () => {
    stubAlert();

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 92,
          type: 'INDIVIDUAL',
          fullName: 'Donor Activity',
          phone: '0909000002',
          email: 'activity@test.local',
          createdAt: '2026-03-23T09:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0
        }
      ])
    }).as('searchDonors');

    cy.intercept('POST', '/api/donations/staff-create', (req) => {
      expect(req.body).to.include({
        donorId: 92,
        amount: 2100000,
        activityId: 25
      });
      expect(req.body.eventId).to.equal(null);
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Tạo đơn quyên góp thành công.', data: 602 }
      });
    }).as('createDonation');

    cy.intercept('GET', '/api/donations/list?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 602,
          memoCode: 'STAFF602',
          donorName: 'Donor Activity',
          donationVia: 'STAFF',
          amount: 2100000,
          objectName: 'Activity 25',
          target: 'ACTIVITY',
          paymentMethod: 'BANK_TRANSFER_ONLINE',
          status: 'PENDING_APPROVED',
          donatedAt: '2026-03-23T09:30:00'
        }
      ])
    }).as('reloadDonationList');

    visitAdminPage('/admin/donations/form');

    cy.get('#donorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#donorDropdownList button', 'Donor Activity').click();
    cy.get('#amount').type('2100000');
    cy.get('#targetActivity').check({ force: true });
    cy.get('#activityTargetGroup').should('be.visible');
    cy.get('#activityId').type('25');
    cy.get('#submitDonation').click();

    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Tạo đơn quyên góp thành công.');
    cy.wait('@reloadDonationList');
    cy.location('pathname').should('eq', '/admin/donations');
  });

  it('validates that a donor must be selected before creating a staff donation', () => {
    stubAlert();

    visitAdminPage('/admin/donations/form');
    cy.get('#amount').type('1500000');
    cy.get('#submitDonation').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng chọn nhà hảo tâm hợp lệ.');
  });

  it('validates that the donation amount is valid and at least 1,000 VND', () => {
    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 91,
          type: 'INDIVIDUAL',
          fullName: 'Donor Placeholder',
          phone: '0909000001',
          email: 'none@test.local',
          createdAt: '2026-03-23T09:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0
        }
      ])
    }).as('searchDonors');
    cy.intercept('POST', '/api/donations/staff-create').as('createDonation');

    visitAdminPage('/admin/donations/form');
    cy.get('#donorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#donorDropdownList button', 'Donor Placeholder').click();
    cy.get('#amount').type('0');
    cy.get('#submitDonation').click();

    cy.get('#amount').then(($input) => {
      expect($input[0].checkValidity()).to.eq(false);
      expect($input[0].validationMessage).to.not.eq('');
    });
    cy.get('@createDonation.all').should('have.length', 0);
  });

  it('validates that the selected target id is provided when an event target is chosen', () => {
    stubAlert();

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 91,
          type: 'INDIVIDUAL',
          fullName: 'Donor Placeholder',
          phone: '0909000001',
          email: 'none@test.local',
          createdAt: '2026-03-23T09:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0
        }
      ])
    }).as('searchDonors');

    visitAdminPage('/admin/donations/form');
    cy.get('#donorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#donorDropdownList button', 'Donor Placeholder').click();
    cy.get('#amount').type('1500000');
    cy.get('#targetEvent').check({ force: true });
    cy.get('#submitDonation').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng nhập ID sự kiện.');
  });

  it('validates receipt information before creating a donation with receipt requested', () => {
    stubAlert();

    cy.intercept('GET', '/api/donors?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 91,
          type: 'INDIVIDUAL',
          fullName: 'Donor Placeholder',
          phone: '0909000001',
          email: 'none@test.local',
          createdAt: '2026-03-23T09:00:00',
          numberOfDonations: 0,
          totalDonationAmount: 0
        }
      ])
    }).as('searchDonors');

    visitAdminPage('/admin/donations/form');
    cy.get('#donorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#donorDropdownList button', 'Donor Placeholder').click();
    cy.get('#amount').type('1500000');
    cy.get('#needReceipt').check({ force: true });
    cy.get('#receiptFields').should('be.visible');
    cy.get('#receiptName').type('Receipt User');
    cy.get('#submitDonation').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng nhập email nhận biên lai.');
  });

  it('shows the seeded donation detail page with donor and donation information', () => {
    visitAdminPage('/admin/donations/17');

    cy.contains('Chi tiết quyên góp').should('be.visible');
    cy.contains('Nhà hảo tâm mẫu 007').should('be.visible');
    cy.contains('0913000007').should('be.visible');
    cy.contains('1,059,000').should('be.visible');
  });

  it('rejects a pending donation from the admin list', () => {
    stubAlert();
    stubConfirm(true);

    let callCount = 0;
    cy.intercept('GET', '/api/donations/list?*', (req) => {
      callCount += 1;
      const item = callCount === 1
        ? {
            id: 31,
            memoCode: 'PENDING-31',
            donorName: 'Donor Reject',
            donationVia: 'STAFF',
            amount: 1700000,
            objectName: 'Khong gan muc tieu',
            target: 'NONE',
            paymentMethod: 'BANK_TRANSFER_ONLINE',
            status: 'PENDING_APPROVED',
            donatedAt: '2026-03-23T09:00:00'
          }
        : {
            id: 31,
            memoCode: 'PENDING-31',
            donorName: 'Donor Reject',
            donationVia: 'STAFF',
            amount: 1700000,
            objectName: 'Khong gan muc tieu',
            target: 'NONE',
            paymentMethod: 'BANK_TRANSFER_ONLINE',
            status: 'REJECTED',
            donatedAt: '2026-03-23T09:00:00'
          };

      req.reply({ statusCode: 200, body: pagedResponse([item]) });
    }).as('loadDonationList');

    cy.intercept('PATCH', '/api/donations/31/change-status?status=REJECTED', {
      statusCode: 200,
      body: { status: 200, message: 'Cập nhật thành công!', data: null }
    }).as('rejectDonation');

    visitAdminPage('/admin/donations');
    cy.wait('@loadDonationList');

    cy.get('button[title="Từ chối"]').click();

    cy.wait('@rejectDonation');
    cy.get('@confirm').should('have.been.called');
    cy.get('@alert').should('have.been.calledWith', 'Cập nhật thành công!');
    cy.wait('@loadDonationList');
    cy.contains('Đã từ chối').should('be.visible');
  });

  it('loads a staff donation edit form and saves the updated information', () => {
    stubAlert();

    cy.intercept('GET', '/api/donations/197', {
      statusCode: 200,
      body: {
        status: 200,
        data: {
          id: 197,
          donorId: 1,
          donorName: 'Phạm Thị Lan',
          donorPhone: '0907000001',
          donationVia: 'STAFF',
          status: 'PENDING_APPROVED',
          target: 'NONE',
          amount: 1219000,
          paymentMethod: 'BANK_TRANSFER_ONLINE',
          message: 'Ban dau',
          needReceipt: false,
          receiptName: null,
          receiptEmail: null,
          eventId: null,
          activityId: null
        }
      }
    }).as('getDonation');

    cy.intercept('PUT', '/api/donations/197/staff-update', (req) => {
      expect(req.body).to.include({
        donorId: 1,
        amount: 1319000,
        paymentMethod: 'BANK_TRANSFER_ONLINE',
        message: 'Da cap nhat'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Cập nhật đơn quyên góp thành công', data: null }
      });
    }).as('updateDonation');

    visitAdminPage('/admin/donations/197/form');
    cy.wait('@getDonation');

    cy.get('#donorId').should('have.value', '1');
    cy.get('#amount').clear().type('1319000');
    cy.get('#message').clear().type('Da cap nhat');
    cy.get('#submitDonation').click();

    cy.wait('@updateDonation');
    cy.get('@alert').should('have.been.calledWith', 'Cập nhật đơn quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/197');
  });

  it('blocks the edit route for a public or already confirmed donation', () => {
    visitAdminPage('/admin/donations/17/form');

    cy.location('pathname').should('eq', '/admin/donations/17');
    cy.contains('Chi tiết quyên góp').should('be.visible');
  });
});
