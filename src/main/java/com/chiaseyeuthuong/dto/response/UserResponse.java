package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.ERole;
import com.chiaseyeuthuong.common.EUserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private String phone;
    private String email;
    private ERole role;
    private EUserStatus status;
    private LocalDateTime createdAt;
    private String createdBy;
}
