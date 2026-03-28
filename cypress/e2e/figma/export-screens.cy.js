import { loginAsAdmin } from '../admin/helpers/adminTestUtils.js';

const donorScreens = [
  {
    group: 'Trang chung',
    name: 'trang-chu',
    title: 'Trang chu',
    route: '/',
    waitFor: 'Các sự kiện đang diễn ra'
  },
  {
    group: 'Trang chung',
    name: 'gioi-thieu',
    title: 'Gioi thieu',
    route: '/about',
    waitFor: 'Câu Chuyện Của Chúng Tôi'
  },
  {
    group: 'Trang chung',
    name: 'lien-he',
    title: 'Lien he',
    route: '/contact'
  },
  {
    group: 'Su kien',
    name: 'danh-sach-su-kien',
    title: 'Danh sach su kien',
    route: '/events',
    waitFor: 'Khám phá các sự kiện'
  },
  {
    group: 'Su kien',
    name: 'chi-tiet-su-kien',
    title: 'Chi tiet su kien',
    route: '/events/gay-quy-mo-tim-be-an',
    waitFor: 'Gây quỹ mổ tim cho bé An'
  },
  {
    group: 'Hoat dong',
    name: 'chi-tiet-hoat-dong',
    title: 'Chi tiet hoat dong',
    route: '/activities/dot-1-chi-phi-phau-thuat',
    waitFor: 'Đợt 1 - Chi phí phẫu thuật'
  },
  {
    group: 'Quyen gop',
    name: 'form-quyen-gop',
    title: 'Form quyen gop',
    route: '/donations',
    waitFor: 'Thông tin quyên góp'
  },
  {
    group: 'Quyen gop',
    name: 'thanh-toan-thanh-cong',
    title: 'Thanh toan thanh cong',
    route: '/thanh-toan/thanh-cong?orderCode=932603210006',
    waitFor: 'Cảm ơn bạn đã đồng hành cùng chúng tôi!'
  }
];

const adminScreens = [
  {
    group: 'Xac thuc',
    name: 'dang-nhap',
    title: 'Dang nhap',
    route: '/login',
    waitFor: 'Đăng nhập quản trị'
  },
  {
    group: 'Tong quan',
    name: 'dashboard',
    title: 'Dashboard',
    route: '/admin/dashboard',
    waitFor: 'Quản trị viên',
    admin: true
  },
  {
    group: 'Nha hao tam',
    name: 'danh-sach-nha-hao-tam',
    title: 'Danh sach nha hao tam',
    route: '/admin/donors',
    waitFor: 'Nhà hảo tâm',
    admin: true
  },
  {
    group: 'Nha hao tam',
    name: 'tao-nha-hao-tam',
    title: 'Tao nha hao tam',
    route: '/admin/donors/form',
    waitFor: 'Thêm Nhà hảo tâm mới',
    admin: true
  },
  {
    group: 'Nha hao tam',
    name: 'chi-tiet-nha-hao-tam',
    title: 'Chi tiet nha hao tam',
    route: '/admin/donors/1',
    waitFor: 'Phạm Thị Lan',
    admin: true
  },
  {
    group: 'Nha hao tam',
    name: 'chinh-sua-nha-hao-tam',
    title: 'Chinh sua nha hao tam',
    route: '/admin/donors/1/form',
    waitFor: 'Chỉnh sửa Nhà hảo tâm',
    admin: true
  },
  {
    group: 'Quyen gop',
    name: 'danh-sach-quyen-gop',
    title: 'Danh sach quyen gop',
    route: '/admin/donations',
    waitFor: 'Quyên góp',
    admin: true
  },
  {
    group: 'Quyen gop',
    name: 'tao-quyen-gop',
    title: 'Tao quyen gop',
    route: '/admin/donations/form',
    waitFor: 'Tạo mới quyên góp',
    admin: true
  },
  {
    group: 'Quyen gop',
    name: 'chi-tiet-quyen-gop',
    title: 'Chi tiet quyen gop',
    route: '/admin/donations/11',
    waitFor: 'Chi tiết quyên góp',
    admin: true
  },
  {
    group: 'Quyen gop',
    name: 'chinh-sua-quyen-gop',
    title: 'Chinh sua quyen gop',
    route: '/admin/donations/21/form',
    waitFor: 'Thông tin quyên góp',
    admin: true
  },
  {
    group: 'Su kien',
    name: 'danh-sach-su-kien-admin',
    title: 'Danh sach su kien',
    route: '/admin/events',
    admin: true
  },
  {
    group: 'Su kien',
    name: 'tao-su-kien',
    title: 'Tao su kien',
    route: '/admin/events/form',
    admin: true
  },
  {
    group: 'Su kien',
    name: 'chinh-sua-su-kien',
    title: 'Chinh sua su kien',
    route: '/admin/events/1/form',
    admin: true
  },
  {
    group: 'Hoat dong',
    name: 'danh-sach-hoat-dong',
    title: 'Danh sach hoat dong',
    route: '/admin/activities',
    admin: true
  },
  {
    group: 'Hoat dong',
    name: 'tao-hoat-dong',
    title: 'Tao hoat dong',
    route: '/admin/activities/form',
    admin: true
  },
  {
    group: 'Hoat dong',
    name: 'chinh-sua-hoat-dong',
    title: 'Chinh sua hoat dong',
    route: '/admin/activities/1/form',
    admin: true
  },
  {
    group: 'Giao dich',
    name: 'danh-sach-giao-dich',
    title: 'Danh sach giao dich',
    route: '/admin/transactions',
    admin: true
  },
  {
    group: 'Giao dich',
    name: 'chi-tiet-giao-dich',
    title: 'Chi tiet giao dich',
    route: '/admin/transactions/2',
    admin: true
  },
  {
    group: 'Cau hinh',
    name: 'cai-dat-he-thong',
    title: 'Cai dat he thong',
    route: '/admin/settings',
    admin: true
  }
];

function captureScreen(screen) {
  const [pathname] = screen.route.split('?');

  if (screen.admin) {
    loginAsAdmin();
  }

  cy.visit(screen.route, {
    failOnStatusCode: false
  });

  cy.location('pathname', { timeout: 30000 }).should('eq', pathname);
  cy.get('body', { timeout: 30000 }).should('be.visible');

  if (screen.waitFor) {
    cy.contains(screen.waitFor, { timeout: 30000 }).should('be.visible');
  }

  cy.wait(2500);

  cy.document().then((doc) => {
    doc.body.style.overflow = 'visible';
  });

  cy.screenshot(`figma-export/${screen.name}`, {
    capture: 'fullPage'
  });
}

describe('Capture screens for grouped Figma export', () => {
  it('captures donor screens', () => {
    donorScreens.forEach(captureScreen);
  });

  it('captures admin screens', () => {
    adminScreens.forEach(captureScreen);
  });
});
