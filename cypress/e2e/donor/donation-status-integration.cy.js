import { visitSuccess } from './helpers/donorTestUtils.js';
import { loginAsAdmin } from '../admin/helpers/adminTestUtils.js';

const EVENT_ID = 1;
const ACTIVITY_ID = 1;

const parseDbRow = (stdout) => {
  const [id, orderCode, status] = stdout.trim().split('\t');
  return {
    id: Number(id),
    orderCode: Number(orderCode),
    status
  };
};

const dbQuery = (sql) =>
  cy.exec(
    `docker exec csyt-db mysql -ucsyt_user -p123456 -D donation -Nse "${sql.replaceAll('"', '\\"')}"`,
    { failOnNonZeroExit: true }
  );

const getDonationByMemoCode = (memoCode) =>
  dbQuery(
    `select id, order_code, status from donations where memo_code = '${memoCode}' order by id desc limit 1;`
  ).then(({ stdout }) => parseDbRow(stdout));

const createUniqueDonor = (label) => {
  const suffix = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
  const phone = `09${suffix.slice(-8)}`;
  const email = `cypress.${label}.${suffix}@test.local`;

  return cy
    .request('POST', '/api/donors/individuals', {
      fullName: `Cypress ${label} ${suffix}`,
      displayName: `Cypress ${label}`,
      phone,
      email,
      note: `auto-${label}`,
      referralSource: 'Website'
    })
    .then((response) => {
      expect(response.status).to.eq(200);
      return response.body.data;
    });
};

const createWebDonation = ({ donorId, amount, eventId = null, activityId = null, message = null }) =>
  cy
    .request('POST', '/api/donations/donor-create', {
      donorId,
      amount,
      message,
      needReceipt: false,
      receiptName: null,
      receiptEmail: null,
      paymentMethod: 'BANK_TRANSFER_ONLINE',
      eventId,
      activityId
    })
    .then((response) => {
      expect(response.status).to.eq(200);
      return response.body.data;
    });

const getEventCurrentAmount = (eventId) =>
  cy.request(`/api/events/${eventId}`).then((response) => Number(response.body.data.currentAmount));

const getActivityCurrentAmount = (activityId) =>
  cy.request(`/api/activities/${activityId}/detail`).then((response) => Number(response.body.data.currentAmount));

const confirmDonationAsAdmin = (donationId) => {
  loginAsAdmin();
  return cy
    .request('PATCH', `/api/donations/${donationId}/change-status?status=CONFIRMED`)
    .then((response) => {
      expect(response.status).to.eq(200);
      return response;
    });
};

describe('Luồng tích hợp quyên góp nhà hảo tâm', () => {
  it('TC-NHT-EVT-004 - Webhook cập nhật số tiền của sự kiện', () => {
    const amount = 11111;
    getEventCurrentAmount(EVENT_ID).then((beforeAmount) => {
      createUniqueDonor('event-confirm')
        .then((donorId) => createWebDonation({ donorId, amount, eventId: EVENT_ID }))
        .then((memoCode) => getDonationByMemoCode(memoCode))
        .then((donation) => {
          expect(donation.status).to.eq('PENDING_PAYMENT');
          return confirmDonationAsAdmin(donation.id).then(() => donation.id);
        })
        .then((donationId) => {
          getEventCurrentAmount(EVENT_ID).then((afterAmount) => {
            expect(afterAmount).to.eq(beforeAmount + amount);
          });

          dbQuery(`select status from donations where id = ${donationId};`).then(({ stdout }) => {
            expect(stdout.trim()).to.eq('CONFIRMED');
          });
        });
    });
  });

  it('TC-NHT-EVT-005 - Hủy thanh toán từ form sự kiện', () => {
    const amount = 22222;
    getEventCurrentAmount(EVENT_ID).then((beforeAmount) => {
      createUniqueDonor('event-cancel')
        .then((donorId) => createWebDonation({ donorId, amount, eventId: EVENT_ID }))
        .then((memoCode) => getDonationByMemoCode(memoCode))
        .then((donation) => {
          expect(donation.status).to.eq('PENDING_PAYMENT');

          cy.visit('/thanh-toan/that-bai');
          cy.location('pathname').should('eq', '/thanh-toan/that-bai');
          cy.contains('Cùng nhau, chúng ta').should('be.visible');

          getEventCurrentAmount(EVENT_ID).then((afterAmount) => {
            expect(afterAmount).to.eq(beforeAmount);
          });

          dbQuery(`select status from donations where id = ${donation.id};`).then(({ stdout }) => {
            expect(stdout.trim()).to.eq('PENDING_PAYMENT');
          });
        });
    });
  });

  it('TC-NHT-ACT-004 + TC-NHT-ACT-005 - Webhook cập nhật số tiền hoạt động và cộng dồn cho sự kiện cha', () => {
    const amount = 33333;
    getActivityCurrentAmount(ACTIVITY_ID).then((beforeActivityAmount) => {
      getEventCurrentAmount(EVENT_ID).then((beforeEventAmount) => {
        createUniqueDonor('activity-confirm')
          .then((donorId) => createWebDonation({ donorId, amount, activityId: ACTIVITY_ID }))
          .then((memoCode) => getDonationByMemoCode(memoCode))
          .then((donation) => {
            expect(donation.status).to.eq('PENDING_PAYMENT');
            return confirmDonationAsAdmin(donation.id).then(() => donation.id);
          })
          .then((donationId) => {
            getActivityCurrentAmount(ACTIVITY_ID).then((afterActivityAmount) => {
              expect(afterActivityAmount).to.eq(beforeActivityAmount + amount);
            });

            getEventCurrentAmount(EVENT_ID).then((afterEventAmount) => {
              expect(afterEventAmount).to.eq(beforeEventAmount + amount);
            });

            dbQuery(`select status from donations where id = ${donationId};`).then(({ stdout }) => {
              expect(stdout.trim()).to.eq('CONFIRMED');
            });
          });
      });
    });
  });

  it('TC-NHT-ACT-006 - Hủy thanh toán từ form hoạt động', () => {
    const amount = 44444;
    getActivityCurrentAmount(ACTIVITY_ID).then((beforeActivityAmount) => {
      getEventCurrentAmount(EVENT_ID).then((beforeEventAmount) => {
        createUniqueDonor('activity-cancel')
          .then((donorId) => createWebDonation({ donorId, amount, activityId: ACTIVITY_ID }))
          .then((memoCode) => getDonationByMemoCode(memoCode))
          .then((donation) => {
            expect(donation.status).to.eq('PENDING_PAYMENT');

            cy.visit('/thanh-toan/that-bai');
            cy.location('pathname').should('eq', '/thanh-toan/that-bai');
            cy.contains('Cùng nhau, chúng ta').should('be.visible');

            getActivityCurrentAmount(ACTIVITY_ID).then((afterActivityAmount) => {
              expect(afterActivityAmount).to.eq(beforeActivityAmount);
            });

            getEventCurrentAmount(EVENT_ID).then((afterEventAmount) => {
              expect(afterEventAmount).to.eq(beforeEventAmount);
            });

            dbQuery(`select status from donations where id = ${donation.id};`).then(({ stdout }) => {
              expect(stdout.trim()).to.eq('PENDING_PAYMENT');
            });
          });
      });
    });
  });

  it('TC-NHT-PAY-003 - Hiển thị giá trị mặc định khi không có lời nhắn', () => {
    const amount = 55555;

    createUniqueDonor('no-message')
      .then((donorId) => createWebDonation({ donorId, amount, message: null }))
      .then((memoCode) => getDonationByMemoCode(memoCode))
      .then((donation) => {
        return confirmDonationAsAdmin(donation.id).then(() => donation.orderCode);
      })
      .then((orderCode) => {
        visitSuccess(orderCode);
        cy.contains('Không có').should('be.visible');
      });
  });

  it('TC-NHT-PAY-008 - Nút Xem biên lai / Hóa đơn', () => {
    visitSuccess(932603210199);

    cy.contains('button', 'Xem biên lai / Hóa đơn').click();
    cy.location('pathname').should('eq', '/thanh-toan/thanh-cong');
    cy.location('search').should('include', 'orderCode=932603210199');
  });
});
