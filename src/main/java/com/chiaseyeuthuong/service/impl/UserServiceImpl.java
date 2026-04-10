package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EUserStatus;
import com.chiaseyeuthuong.dto.request.UserCreateRequest;
import com.chiaseyeuthuong.dto.request.UserUpdateRequest;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.UserResponse;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.User;
import com.chiaseyeuthuong.repository.UserRepository;
import com.chiaseyeuthuong.service.UserService;
import com.chiaseyeuthuong.service.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "fullName", "username", "email", "phone", "role", "status");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    public long createUser(UserCreateRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedPhone = request.getPhone().trim();
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new InvalidDataException("Email đã tồn tại");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new InvalidDataException("Số điện thoại đã tồn tại");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new InvalidDataException("Username đã tồn tại");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizedPhone);
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(EUserStatus.ACTIVE);

        return userRepository.save(user).getId();
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên"));
        return toResponse(user);
    }

    @Override
    public void updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên"));

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedPhone = request.getPhone().trim();
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new InvalidDataException("Email đã tồn tại");
        }
        if (userRepository.existsByPhoneAndIdNot(normalizedPhone, id)) {
            throw new InvalidDataException("Số điện thoại đã tồn tại");
        }
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, id)) {
            throw new InvalidDataException("Username đã tồn tại");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizedPhone);
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setRole(request.getRole());

        userRepository.save(user);
    }

    @Override
    public void updateUserStatus(Long id, EUserStatus status) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(Iterable<Long> ids, String currentUsername) {
        for (Long id : ids) {
            User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên"));
            validateDeletePermission(user, currentUsername);
            userRepository.delete(user);
        }

        log.info("Deleted users");
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

    private void validateDeletePermission(User user, String currentUsername) {
        if (currentUsername != null && currentUsername.equalsIgnoreCase(user.getUsername())) {
            throw new InvalidDataException("Không thể tự xóa tài khoản đang đăng nhập");
        }
    }
}
