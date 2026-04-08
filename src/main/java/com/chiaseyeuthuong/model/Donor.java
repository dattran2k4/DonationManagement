package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.EDonorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donors")
public class Donor extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", unique = true, nullable = false)
    private String phone;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "referral_source")
    private String referralSource;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "note", columnDefinition = "TEXT", length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EDonorType type;

    @OneToOne(mappedBy = "donor", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Organization organization;

    @OneToMany(mappedBy = "donor")
    private List<Donation> donations = new ArrayList<>();

    public void setOrganization(Organization org) {
        if (org == null) {
            if (this.organization != null) {
                this.organization.setDonor(null);
            }
        } else {
            org.setDonor(this);
        }
        this.organization = org;
    }
}
