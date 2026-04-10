package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EUserStatus;
import com.chiaseyeuthuong.dto.request.UserCreateRequest;
import com.chiaseyeuthuong.dto.request.UserUpdateRequest;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.UserResponse;

public interface UserService {
    PageResponse<UserResponse> getAllUsers(int page, int size, String search, EUserStatus status, String sortBy, String sortDir);

    long createUser(UserCreateRequest request);

    UserResponse getUserById(Long id);

    void updateUser(Long id, UserUpdateRequest request);

    void updateUserStatus(Long id, EUserStatus status);

    void deleteUsers(Iterable<Long> ids, String currentUsername);
}
