package com.chiaseyeuthuong.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final String USER_ID = "userId";

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showUsersPage() {
        return "pages/admin/users";
    }

    @GetMapping("/form")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateUserPage() {
        return "pages/admin/user-detail";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showUserDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute(USER_ID, id);
        return "pages/admin/user-detail";
    }
}
