import { pages } from './helpers/donorTestUtils.js';

describe('Các trang nội dung nhà hảo tâm', () => {
  const heroDonationSelector =
    'div.relative.overflow-hidden a[href="/quyen-gop"].bg-primary';

  it('TC-NHT-ACTVIEW-001 - Hiển thị đúng tên hoạt động', () => {
    cy.visit(pages.activity);

    cy.get('h1').should('contain', 'Đợt 1 - Chi phí phẫu thuật');
  });

  it('TC-NHT-ACTVIEW-002 - Hiển thị đúng địa điểm hoạt động', () => {
    cy.visit(pages.activity);

    cy.contains('Bệnh viện Nhi Đồng 1, TP. HCM').should('be.visible');
  });

  it('TC-NHT-ACTVIEW-003 - Hiển thị đúng mô tả ngắn', () => {
    cy.visit(pages.activity);

    cy.contains('Gom đủ chi phí phẫu thuật theo dự toán bệnh viện.').should('be.visible');
  });

  it('TC-NHT-ACTVIEW-004 - Hiển thị đúng ảnh hoạt động', () => {
    cy.visit(pages.activity);

    cy.get('img[alt="Đợt 1 - Chi phí phẫu thuật"]')
      .should('be.visible')
      .and('have.attr', 'src')
      .and('include', 'photo-1532629345422-7515f3d16bb6');
  });

  it('TC-NHT-HOME-001 - Màu chủ đạo đúng theo màu thương hiệu', () => {
    cy.visit(pages.home);

    cy.get(heroDonationSelector).should('be.visible');
    cy.get('div.inline-flex.items-center').first().should('have.class', 'text-primary');
    cy.contains('Xem tất cả sự kiện').should('have.class', 'text-primary');
  });

  it('TC-NHT-HOME-002 - Nút nền xanh hiển thị chữ trắng rõ ràng', () => {
    cy.visit(pages.home);

    cy.get(heroDonationSelector)
      .should('have.css', 'color', 'rgb(255, 255, 255)');
  });

  it('TC-NHT-HOME-003 - Đồng bộ phông chữ và kiểu chữ giữa các khu vực', () => {
    cy.visit(pages.home);

    cy.get('h1').first().then(($heroTitle) => {
      const heroFont = getComputedStyle($heroTitle[0]).fontFamily;

      cy.get('h2').first().then(($sectionTitle) => {
        const sectionFont = getComputedStyle($sectionTitle[0]).fontFamily;
        expect(sectionFont).to.eq(heroFont);
      });
    });
  });
});
