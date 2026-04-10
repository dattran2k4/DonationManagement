package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EUserStatus;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.UserResponse;
import com.chiaseyeuthuong.model.User;
import com.chiaseyeuthuong.repository.UserRepository;
import com.chiaseyeuthuong.service.UserService;
import com.chiaseyeuthuong.service.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "fullName", "email", "phone", "role", "status");

    private final UserRepository userRepository;

    @Override
    public PageResponse<UserResponse> getAllUsers(int page, int size, String search, EUserStatus status, String sortBy, String sortDir) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = size > 0 ? size : 10;
        String safeSortBy = resolveSortBy(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Specification<User> specification = UserSpecification.filterUsers(search, status);
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, safeSortBy));
        Page<User> pageUsers = userRepository.findAll(specification, pageRequest);

        return PageResponse.<UserResponse>builder()
                .page(pageNumber + 1)
                .pageSize(pageSize)
                .totalItems(pageUsers.getTotalElements())
                .totalPages(pageUsers.getTotalPages())
                .data(pageUsers.getContent().stream().map(this::toResponse).toList())
                .build();
    }

    private String resolveSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "id";
        }
        return ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        if (response.getStatus() == null) {
            response.setStatus(EUserStatus.ACTIVE);
        }
        return response;
    }
}
