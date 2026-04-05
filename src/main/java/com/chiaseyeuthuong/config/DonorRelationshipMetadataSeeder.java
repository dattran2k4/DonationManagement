package com.chiaseyeuthuong.config;

import com.chiaseyeuthuong.model.OrganizationRoleType;
import com.chiaseyeuthuong.model.PersonRelationshipType;
import com.chiaseyeuthuong.repository.OrganizationRoleTypeRepository;
import com.chiaseyeuthuong.repository.PersonRelationshipTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DONOR-RELATIONSHIP-SEEDER")
public class DonorRelationshipMetadataSeeder implements ApplicationRunner {

    private final PersonRelationshipTypeRepository personRelationshipTypeRepository;
    private final OrganizationRoleTypeRepository organizationRoleTypeRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedPersonRelationshipTypes();
        seedOrganizationRoleTypes();
    }

    private void seedPersonRelationshipTypes() {
        List<PersonSeedItem> items = List.of(
                new PersonSeedItem("SPOUSE", "Vợ/chồng", "SPOUSE", 1),
                new PersonSeedItem("PARENT", "Cha/mẹ", "CHILD", 2),
                new PersonSeedItem("CHILD", "Con", "PARENT", 3),
                new PersonSeedItem("GRANDPARENT", "Ông/bà", "GRANDCHILD", 4),
                new PersonSeedItem("GRANDCHILD", "Cháu", "GRANDPARENT", 5),
                new PersonSeedItem("GRANDSON", "Cháu trai", "GRANDPARENT", 6),
                new PersonSeedItem("GRANDDAUGHTER", "Cháu gái", "GRANDPARENT", 7),
                new PersonSeedItem("AUNT", "Cô/Dì", "GRANDCHILD", 8),
                new PersonSeedItem("UNCLE", "Chú/Bác/Cậu", "GRANDCHILD", 9),
                new PersonSeedItem("SIBLING", "Anh/chị/em", "SIBLING", 10),
                new PersonSeedItem("OTHER_RELATIVE", "Người thân khác", "OTHER_RELATIVE", 11)
        );

        items.forEach(item -> personRelationshipTypeRepository.findByCode(item.code())
                .map(existing -> {
                    existing.setName(item.name());
                    existing.setReverseCode(item.reverseCode());
                    existing.setSortOrder(item.sortOrder());
                    existing.setIsActive(true);
                    return personRelationshipTypeRepository.save(existing);
                })
                .orElseGet(() -> {
                    PersonRelationshipType type = new PersonRelationshipType();
                    type.setCode(item.code());
                    type.setName(item.name());
                    type.setReverseCode(item.reverseCode());
                    type.setSortOrder(item.sortOrder());
                    type.setIsActive(true);
                    log.info("Seeding person relationship type {}", item.code());
                    return personRelationshipTypeRepository.save(type);
                }));
    }

    private void seedOrganizationRoleTypes() {
        List<SeedItem> items = List.of(
                new SeedItem("EMPLOYEE", "Nhân viên", 1),
                new SeedItem("OWNER", "Chủ sở hữu", 2),
                new SeedItem("FOUNDER", "Nhà sáng lập", 3),
                new SeedItem("CO_FOUNDER", "Đồng sáng lập", 4),
                new SeedItem("CEO", "Giám đốc điều hành (CEO)", 5),
                new SeedItem("CFO", "Giám đốc tài chính (CFO)", 6),
                new SeedItem("CTO", "Giám đốc công nghệ (CTO)", 7),
                new SeedItem("REPRESENTATIVE", "Đại diện", 8),
                new SeedItem("BOARD_MEMBER", "Thành viên ban điều hành", 9),
                new SeedItem("OTHER", "Khác", 10)
        );

        items.forEach(item -> organizationRoleTypeRepository.findByCode(item.code())
                .map(existing -> {
                    existing.setName(item.name());
                    existing.setSortOrder(item.sortOrder());
                    existing.setIsActive(true);
                    return organizationRoleTypeRepository.save(existing);
                })
                .orElseGet(() -> {
                    OrganizationRoleType type = new OrganizationRoleType();
                    type.setCode(item.code());
                    type.setName(item.name());
                    type.setSortOrder(item.sortOrder());
                    type.setIsActive(true);
                    log.info("Seeding organization role type {}", item.code());
                    return organizationRoleTypeRepository.save(type);
                }));
    }

    private record PersonSeedItem(String code, String name, String reverseCode, int sortOrder) {
    }

    private record SeedItem(String code, String name, int sortOrder) {
    }
}
