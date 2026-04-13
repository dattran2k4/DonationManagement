import { pagedResponse, stubAlert, stubConfirm, visitAdminPage } from './helpers/adminTestUtils.js';

describe('Quản lý quyên góp', () => {
  it('TC-ADM-DON-001 + TC-ADM-DON-002 + TC-ADM-DON-003 - Mở danh sách quyên góp, tìm kiếm, lọc và đặt lại bộ lọc', () => {
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

  it('TC-ADM-DON-012 - Duyệt khoản quyên góp chờ xử lý', () => {
    stubAlert();

    cy.intercept('PATCH', '/api/donations/21/change-status?status=CONFIRMED', {
      statusCode: 200,
      body: { status: 200, message: 'Cập nhật thành công!', data: null }
    }).as('approveDonation');

    visitAdminPage('/admin/donations/21');

    cy.get('#approveBtn').click();

    cy.wait('@approveDonation');
    cy.get('@alert').should('have.been.calledWith', 'Duyệt khoản quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/21');
  });

  it('TC-ADM-DON-005 + TC-ADM-DON-010 - Tạo khoản quyên góp nội bộ gắn chiến dịch và thông tin biên lai', () => {
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
        paymentMethod: 'BANK_TRANSFER_OFFLINE',
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

    cy.intercept('GET', '/api/events?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 15,
          name: 'Event 15',
          status: 'ONGOING'
        }
      ])
    }).as('searchEvents');

    visitAdminPage('/admin/donations/form');

    cy.get('#editDonorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#editDonorDropdownList button', 'Tran Van Donation').click();
    cy.get('#editDonorId').should('have.value', '77');

    cy.get('#editAmount').clear().type('3500000');
    cy.get('#editMessage').type('Quyen gop noi bo');
    cy.get('#donationPaymentMethodSelect').select('BANK_TRANSFER_OFFLINE');
    cy.get('#editEventSearchInput').click();
    cy.wait('@searchEvents');
    cy.contains('#editEventDropdownList button', 'Event 15').click();
    cy.get('#editEventId').should('have.value', '15');
    cy.get('#editNeedReceipt').check({ force: true });
    cy.get('#editReceiptFields').should('be.visible');
    cy.get('#editReceiptName').type('Tran Van Donation');
    cy.get('#editReceiptEmail').type('receipt@test.local');
    cy.get('#saveDonationBtn').click();

    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Tạo khoản quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/501');
    cy.location('search').should('include', 'saved=1');
  });

  it('TC-ADM-DON-004 - Tạo khoản quyên góp nội bộ không gắn mục tiêu', () => {
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
        paymentMethod: 'CASH',
        needReceipt: false
      });
      expect(req.body.eventId).to.equal(null);
      expect(req.body.activityId).to.equal(null);
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Tạo đơn quyên góp thành công.', data: 601 }
      });
    }).as('createDonation');

    visitAdminPage('/admin/donations/form');

    cy.get('#editDonorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#editDonorDropdownList button', 'Donor No Target').click();
    cy.get('#editAmount').clear().type('1500000');
    cy.get('#saveDonationBtn').click();

    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Tạo khoản quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/601');
    cy.location('search').should('include', 'saved=1');
  });

  it('TC-ADM-DON-006 - Tạo khoản quyên góp nội bộ gắn hoạt động', () => {
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
        paymentMethod: 'CASH',
        eventId: 15,
        activityId: 25
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Tạo đơn quyên góp thành công.', data: 602 }
      });
    }).as('createDonation');

    cy.intercept('GET', '/api/activities?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 25,
          name: 'Activity 25',
          status: 'ONGOING',
          event: { id: 15, name: 'Event 15' },
          startDate: '2026-03-23',
          endDate: '2026-03-24',
          location: 'Da Nang',
          currentAmount: 0,
          targetAmount: 1000000
        }
      ])
    }).as('searchActivities');

    visitAdminPage('/admin/donations/form');

    cy.get('#editDonorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#editDonorDropdownList button', 'Donor Activity').click();
    cy.get('#editAmount').clear().type('2100000');
    cy.get('#editActivitySearchInput').click();
    cy.wait('@searchActivities');
    cy.contains('#editActivityDropdownList button', 'Activity 25').click();
    cy.get('#editActivityId').should('have.value', '25');
    cy.get('#editEventId').should('have.value', '15');
    cy.get('#saveDonationBtn').click();

    cy.wait('@createDonation');
    cy.get('@alert').should('have.been.calledWith', 'Tạo khoản quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/602');
    cy.location('search').should('include', 'saved=1');
  });

  it('TC-ADM-DON-007 - Kiểm tra bắt buộc phải chọn nhà hảo tâm', () => {
    stubAlert();

    visitAdminPage('/admin/donations/form');
    cy.get('#editAmount').clear().type('1500000');
    cy.get('#saveDonationBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng chọn nhà hảo tâm hợp lệ.');
  });

  it('TC-ADM-DON-008 - Kiểm tra số tiền không hợp lệ', () => {
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
    cy.get('#editDonorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#editDonorDropdownList button', 'Donor Placeholder').click();
    cy.get('#editAmount').clear().type('0');
    cy.get('#saveDonationBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Số tiền tối thiểu là 1.000 đồng.');
    cy.get('@createDonation.all').should('have.length', 0);
  });

  it('TC-ADM-DON-009 - Tự đồng bộ sự kiện cha khi chọn hoạt động', () => {
    cy.intercept('GET', '/api/activities?*', {
      statusCode: 200,
      body: pagedResponse([
        {
          id: 25,
          name: 'Activity 25',
          status: 'ONGOING',
          event: { id: 15, name: 'Event 15' },
          startDate: '2026-03-23',
          endDate: '2026-03-24',
          location: 'Da Nang',
          currentAmount: 0,
          targetAmount: 1000000
        }
      ])
    }).as('searchActivities');

    visitAdminPage('/admin/donations/form');
    cy.get('#editActivitySearchInput').click();
    cy.wait('@searchActivities');
    cy.contains('#editActivityDropdownList button', 'Activity 25').click();

    cy.get('#editActivityId').should('have.value', '25');
    cy.get('#editEventId').should('have.value', '15');
    cy.get('#editEventSearchInput').should('have.value', 'Event 15');
  });

  it('TC-ADM-DON-010 - Kiểm tra thông tin biên lai', () => {
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
    cy.get('#editDonorSearchInput').click();
    cy.wait('@searchDonors');
    cy.contains('#editDonorDropdownList button', 'Donor Placeholder').click();
    cy.get('#editAmount').clear().type('1500000');
    cy.get('#editNeedReceipt').check({ force: true });
    cy.get('#editReceiptFields').should('be.visible');
    cy.get('#editReceiptName').type('Receipt User');
    cy.get('#saveDonationBtn').click();

    cy.get('@alert').should('have.been.calledWith', 'Vui lòng nhập email nhận biên lai.');
  });

  it('TC-ADM-DON-011 - Xem chi tiết khoản quyên góp', () => {
    visitAdminPage('/admin/donations/17');

    cy.contains('Chi tiết quyên góp').should('be.visible');
    cy.contains('Nhà hảo tâm mẫu 007').should('be.visible');
    cy.contains('0913000007').should('be.visible');
    cy.contains('1,059,000').should('be.visible');
  });

  it('TC-ADM-DON-013 - Từ chối khoản quyên góp chờ xử lý', () => {
    stubAlert();
    cy.intercept('PATCH', '/api/donations/21/reject', (req) => {
      expect(req.body).to.deep.equal({ reason: 'Ly do Cypress' });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Từ chối đơn từ thiện thành công', data: null }
      });
    }).as('rejectDonation');

    visitAdminPage('/admin/donations/21');

    cy.window().then((win) => {
      cy.stub(win, 'prompt').returns('Ly do Cypress').as('prompt');
    });

    cy.get('#rejectBtn').click();

    cy.wait('@rejectDonation');
    cy.get('@prompt').should('have.been.called');
    cy.get('@alert').should('have.been.calledWith', 'Từ chối khoản quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/21');
  });

  it('TC-ADM-DON-014 - Sửa khoản quyên góp nội bộ chưa xác nhận', () => {
    stubAlert();

    cy.intercept('PUT', '/api/donations/21/staff-update', (req) => {
      expect(req.body).to.include({
        amount: 1319000,
        paymentMethod: 'BANK_TRANSFER_OFFLINE',
        message: 'Da cap nhat'
      });
      req.reply({
        statusCode: 200,
        body: { status: 200, message: 'Cập nhật đơn quyên góp thành công', data: null }
      });
    }).as('updateDonation');

    visitAdminPage('/admin/donations/21');

    cy.get('#editDonorId').invoke('val').should('not.equal', '');
    cy.get('#editAmount').clear().type('1319000');
    cy.get('#editMessage').clear().type('Da cap nhat');
    cy.get('#donationPaymentMethodSelect').select('BANK_TRANSFER_OFFLINE');
    cy.get('#saveDonationBtn').click();

    cy.wait('@updateDonation');
    cy.get('@alert').should('have.been.calledWith', 'Cập nhật khoản quyên góp thành công');
    cy.location('pathname').should('eq', '/admin/donations/21');
    cy.location('search').should('include', 'saved=1');
  });

  it('TC-ADM-DON-015 - Chặn sửa khoản quyên góp đã xác nhận hoặc khoản quyên góp công khai', () => {
    visitAdminPage('/admin/donations/17/form');

    cy.location('pathname').should('eq', '/admin/donations/17');
    cy.contains('Chi tiết quyên góp').should('be.visible');
  });
});
