package com.chiaseyeuthuong.security;

import com.chiaseyeuthuong.common.EUserStatus;
import com.chiaseyeuthuong.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    @NotNull
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_%s".formatted(user.getRole().name())));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @NotNull
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == null || user.getStatus() == EUserStatus.ACTIVE;
    }

}
