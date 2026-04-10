package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EUserStatus;
import com.chiaseyeuthuong.dto.request.UserCreateRequest;
import com.chiaseyeuthuong.dto.request.UserUpdateRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ApiUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getListUsers(@RequestParam(required = false, defaultValue = "1") int page,
                                    @RequestParam(required = false, defaultValue = "10") int size,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) EUserStatus status,
                                    @RequestParam(required = false, defaultValue = "id") String sortBy,
                                    @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách thành viên thành công")
                .data(userService.getAllUsers(page, size, search, status, sortBy, sortDir))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getUserDetail(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết thành viên thành công")
                .data(userService.getUserById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Tạo thành viên thành công")
                .data(userService.createUser(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật thành viên thành công")
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse updateUserStatus(@PathVariable Long id, @RequestParam EUserStatus status) {
        userService.updateUserStatus(id, status);
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật trạng thái thành viên thành công")
                .build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse deleteUsers(@RequestParam List<Long> ids, Principal principal) {
        userService.deleteUsers(ids, principal.getName());
        return ApiResponse.builder()
                .status(200)
                .message("Xóa danh sách thành viên thành công")
                .build();
    }
}
