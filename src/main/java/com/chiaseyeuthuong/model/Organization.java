package com.chiaseyeuthuong.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    private Long id;

    @Column(name = "tax_code", unique = true)
    private String taxCode;

    @Column(name = "name")
    private String name;

    @Column(name = "representative")
    private String representative;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @OneToOne
    @MapsId // Chia sẻ Primary Key với bảng Donor
    @JoinColumn(name = "id")
    private Donor donor;
}
