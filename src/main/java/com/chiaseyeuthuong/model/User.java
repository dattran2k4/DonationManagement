package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.ERole;
import com.chiaseyeuthuong.common.EUserStatus;
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
@Table(name = "users")
public class User extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", unique = true)
    private String phone;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private ERole role;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EUserStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = EUserStatus.ACTIVE;
        }
    }
}
